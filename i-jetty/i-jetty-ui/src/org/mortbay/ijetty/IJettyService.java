//========================================================================
//$Id: IJettyService.java 474 2012-01-23 03:07:14Z janb.webtide $
//Copyright 2008 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.ijetty;

import static org.mortbay.ijetty.common.LogSupport.TAG;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.mortbay.ijetty.deployer.AndroidContextDeployer;
import org.mortbay.ijetty.deployer.AndroidWebAppDeployer;
import org.mortbay.ijetty.handler.DefaultHandler;
import org.mortbay.ijetty.util.IJettyToast;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.security.Credential;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * IJettyService
 *
 * Android Service which runs the Jetty server, maintaining it in the active Notifications so that
 * the user can return to the IJetty Activity to control it at any time.
 */
public class IJettyService extends Service
{
    public static final String CONTENT_RESOLVER_ATTRIBUTE = "org.mortbay.ijetty.contentResolver";
    public static final String ANDROID_CONTEXT_ATTRIBUTE = "org.mortbay.ijetty.context";

    public static final int START_PROGRESS_DIALOG = 0;
    public static final int STARTED = 0;
    public static final int NOT_STARTED = 1;
    public static final int STOPPED = 2;
    public static final int NOT_STOPPED = 3;
    public static final int STARTING = 4;
    public static final int STOPPING = 5;

    private static Resources resources;

    public static final String[] configurationClasses =
            new String[]{ "org.mortbay.ijetty.webapp.AndroidWebInfConfiguration", "org.eclipse.jetty.webapp.WebXmlConfiguration",
                    "org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.webapp.TagLibConfiguration" };

    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    private NotificationManager notificationManager;
    private final AtomicReference<Server> server = new AtomicReference<Server>();
    private ContextHandlerCollection contexts;
    private boolean useNIO;
    private boolean useSSL;
    private int port;
    private int sslPort;
    private String consolePassword;
    private String keymgrPassword;
    private String keystorePassword;
    private String truststorePassword;
    private String keystoreFile;
    private String truststoreFile;
    private SharedPreferences preferences;
    private PackageInfo packageInfo;
    private android.os.Handler handler;

    private PowerManager.WakeLock wakeLock;
    private final IBinder binder = new IJettyServiceBinder();

    /**
     * IJettyService always runs in-process with the IJetty activity.
     */
    public class IJettyServiceBinder extends Binder
    {
        public IJettyService getService()
        {
            // Return this instance of LocalService so clients can call public methods
            return IJettyService.this;
        }
    }

    /**
     * Hack to get around bug in ResourceBundles
     *
     * @param id
     * @return
     */
    public static InputStream getStreamToRawResource( int id )
    {
        if ( resources != null )
        {
            return resources.openRawResource( id );
        }
        else
        {
            return null;
        }
    }

    public static boolean isRunning()
    {
        return isRunning.get();
    }

    public IJettyService()
    {
        handler = new android.os.Handler()
        {
            public void handleMessage( Message msg )
            {
                switch ( msg.getData().getInt( "state" ) )
                {
                    case STARTED:
                    {
                        IJettyToast.showServiceToast( IJettyService.this, R.string.jetty_started );
                        notificationManager = (NotificationManager) getSystemService( NOTIFICATION_SERVICE );
                        // The PendingIntent to launch IJetty activity if the user selects this notification
                        PendingIntent contentIntent = PendingIntent.getActivity( IJettyService.this, 0, new Intent( IJettyService.this,
                                IJetty.class ), 0 );

                        CharSequence text = getText( R.string.manage_jetty );
                        Notification notification = new Notification( R.drawable.ijetty_stat, text, System.currentTimeMillis() );
                        notification.setLatestEventInfo( IJettyService.this, getText( R.string.app_name ), text, contentIntent );
                        notificationManager.notify( R.string.jetty_started, notification );

                        Intent startIntent = new Intent( IJetty.START_ACTION );
                        startIntent.addCategory( "default" );
                        Connector[] connectors = server.get().getConnectors();
                        if ( connectors != null )
                        {
                            String[] tmp = new String[connectors.length];

                            for ( int i = 0; i < connectors.length; i++ )
                            {
                                tmp[i] = connectors[i].toString();
                            }

                            startIntent.putExtra( "connectors", tmp );
                        }

                        sendBroadcast( startIntent );
                        break;
                    }
                    case NOT_STARTED:
                        IJettyToast.showServiceToast( IJettyService.this, R.string.jetty_not_started );
                        break;
                    case STOPPED:
                    {
                        // Cancel the persistent notification.
                        notificationManager = (NotificationManager) getSystemService( NOTIFICATION_SERVICE );
                        notificationManager.cancel( R.string.jetty_started );
                        // Tell the user we stopped.
                        IJettyToast.showServiceToast( IJettyService.this, R.string.jetty_stopped );
                        Intent stopIntent = new Intent( IJetty.STOP_ACTION );
                        stopIntent.addCategory( "default" );
                        sendBroadcast( stopIntent );
                        break;
                    }

                    case NOT_STOPPED:
                        IJettyToast.showServiceToast( IJettyService.this, R.string.jetty_not_stopped );
                        break;
                    case STARTING:
                        IJettyToast.showServiceToast( IJettyService.this, R.string.jetty_starting );
                        break;
                    case STOPPING:
                        IJettyToast.showServiceToast( IJettyService.this, R.string.jetty_stopping );
                        break;
                }

            }

        };
    }

    @Override
    public IBinder onBind( Intent intent )
    {
        return binder;
    }

    /**
     * Android Service create
     * @see android.app.Service#onCreate()
     */
    public void onCreate()
    {
        resources = getResources();

        try
        {
            packageInfo = getPackageManager().getPackageInfo( getPackageName(), 0 );
        }
        catch ( Exception e )
        {
            Log.e( TAG, "Unable to determine running jetty version" );
        }
    }

    /**
     * Android Service Start
     * @see android.app.Service#onStart(android.content.Intent, int)
     */
    @Override
    public void onStart( Intent intent, int startId )
    {
        if ( server.get() != null )
        {
            IJettyToast.showServiceToast( IJettyService.this, R.string.jetty_already_started );
            return;
        }

        try
        {
            preferences = PreferenceManager.getDefaultSharedPreferences( this );

            String portDefault = getText( R.string.pref_port_value ).toString();
            String sslPortDefault = getText( R.string.pref_ssl_port_value ).toString();
            String pwdDefault = getText( R.string.pref_console_pwd_value ).toString();

            String nioEnabledDefault = getText( R.string.pref_nio_value ).toString();
            String sslEnabledDefault = getText( R.string.pref_ssl_value ).toString();

            String portKey = getText( R.string.pref_port_key ).toString();
            String sslPortKey = getText( R.string.pref_ssl_port_key ).toString();
            String pwdKey = getText( R.string.pref_console_pwd_key ).toString();
            String nioKey = getText( R.string.pref_nio_key ).toString();
            String sslKey = getText( R.string.pref_ssl_key ).toString();

            useSSL = preferences.getBoolean( sslKey, Boolean.valueOf( sslEnabledDefault ) );
            useNIO = preferences.getBoolean( nioKey, Boolean.valueOf( nioEnabledDefault ) );
            port = Integer.parseInt( preferences.getString( portKey, portDefault ) );
            if ( useSSL )
            {
                sslPort = Integer.parseInt( preferences.getString( sslPortKey, sslPortDefault ) );
                String defaultValue = getText( R.string.pref_keystore_pwd_value ).toString();
                String key = getText( R.string.pref_keystore_pwd_key ).toString();
                keystorePassword = preferences.getString( key, defaultValue );

                defaultValue = getText( R.string.pref_keymgr_pwd_value ).toString();
                key = getText( R.string.pref_keymgr_pwd_key ).toString();
                keymgrPassword = preferences.getString( key, defaultValue );

                defaultValue = getText( R.string.pref_truststore_pwd_value ).toString();
                key = getText( R.string.pref_truststore_pwd_key ).toString();
                truststorePassword = preferences.getString( key, defaultValue );

                defaultValue = getText( R.string.pref_keystore_file ).toString();
                key = getText( R.string.pref_keystore_file_key ).toString();
                keystoreFile = preferences.getString( key, defaultValue );

                defaultValue = getText( R.string.pref_truststore_file ).toString();
                key = getText( R.string.pref_truststore_file_key ).toString();
                truststoreFile = preferences.getString( key, defaultValue );
            }

            consolePassword = preferences.getString( pwdKey, pwdDefault );

            Log.d( TAG, "pref port = " + port );
            Log.d( TAG, "pref use nio = " + useNIO );
            Log.d( TAG, "pref use ssl = " + useSSL );
            Log.d( TAG, "pref ssl port = " + sslPort );

            //Get a wake lock to stop the cpu going to sleep
            PowerManager pm = (PowerManager) getSystemService( Context.POWER_SERVICE );
            wakeLock = pm.newWakeLock( PowerManager.SCREEN_DIM_WAKE_LOCK, "IJetty" );
            wakeLock.acquire();

            new JettyStarterThread( handler ).start();

            super.onStart( intent, startId );
        }
        catch ( Exception e )
        {
            Log.e( TAG, "Error starting jetty", e );
            IJettyToast.showServiceToast( IJettyService.this, R.string.jetty_not_started );
        }
    }

    /**
     * Android Service destroy
     * @see android.app.Service#onDestroy()
     */
    public void onDestroy()
    {
        try
        {
            if ( wakeLock != null )
            {
                wakeLock.release();
                wakeLock = null;
            }

            if ( server.get() != null )
            {
                new JettyStopperThread( handler ).start();

            }
            else
            {
                Log.i( TAG, "Jetty not running" );
                IJettyToast.showServiceToast( IJettyService.this, R.string.jetty_not_running );
            }
        }
        catch ( Exception e )
        {
            Log.e( TAG, "Error stopping jetty", e );
            IJettyToast.showServiceToast( IJettyService.this, R.string.jetty_not_stopped );
        }
    }

    public void onLowMemory()
    {
        Log.i( TAG, "Low on memory" );
        super.onLowMemory();
    }

    /**
     * Get a reference to the Jetty Server instance
     * @return
     */
    public Server getServer()
    {
        return server.get();
    }

    protected Server newServer()
    {
        return new Server();
    }

    protected ContextHandlerCollection newContexts()
    {
        return new ContextHandlerCollection();
    }

    protected void configureConnectors()
    {
        Log.i( TAG, "configuring connectors" );
        if ( server.get() != null )
        {
            if ( useNIO )
            {
                SelectChannelConnector nioConnector = new SelectChannelConnector();
                nioConnector.setUseDirectBuffers( false );
                nioConnector.setPort( port );
                server.get().addConnector( nioConnector );
                Log.i( TAG, "configured (" + SelectChannelConnector.class.getName() + ") on port: " + port );
            }
            else
            {
                SocketConnector bioConnector = new SocketConnector();
                bioConnector.setPort( port );
                server.get().addConnector( bioConnector );
                Log.i( TAG, "configured (" + SocketConnector.class.getName() + ") on port: " + port );
            }

            if ( useSSL )
            {
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStore( keystoreFile );
                sslContextFactory.setTrustStore( truststoreFile );
                sslContextFactory.setKeyStorePassword( keystorePassword );
                sslContextFactory.setKeyManagerPassword( keymgrPassword );
                sslContextFactory.setKeyStoreType( "bks" );
                sslContextFactory.setTrustStorePassword( truststorePassword );
                sslContextFactory.setTrustStoreType( "bks" );

                //TODO SslSelectChannelConnector does not work on android 1.6, but does work on android 2.2
                if ( useNIO )
                {
                    SslSelectChannelConnector sslConnector = new SslSelectChannelConnector( sslContextFactory );
                    sslConnector.setPort( sslPort );
                    server.get().addConnector( sslConnector );
                    Log.i( TAG, "configured (" + sslConnector.getClass().getName() + ") on port: " + sslPort );
                }
                else
                {
                    SslSocketConnector sslConnector = new SslSocketConnector( sslContextFactory );
                    sslConnector.setPort( sslPort );
                    server.get().addConnector( sslConnector );
                    Log.i( TAG, "configured (" + sslConnector.getClass().getName() + " )on port: " + sslPort );
                }
            }
        }
    }

    protected void configureHandlers()
    {
        Log.i( TAG, "configuring handlers" );
        if ( server.get() != null )
        {
            HandlerCollection handlers = new HandlerCollection();
            contexts = new ContextHandlerCollection();
            handlers.setHandlers( new Handler[]{ contexts, new DefaultHandler() } );
            server.get().setHandler( handlers );
        }
    }

    protected void configureDeployers() throws Exception
    {
        Log.i( TAG, "configuring deployers" );
        AndroidWebAppDeployer staticDeployer = new AndroidWebAppDeployer();
        AndroidContextDeployer contextDeployer = new AndroidContextDeployer();

        File jettyDir = IJetty.JETTY_DIR;
        if ( jettyDir.exists() )
        {
            // If the webapps dir exists, start the static webapp deployer
            File webappDir = new File( jettyDir, IJetty.WEBAPP_DIR );
            Log.i( TAG, "webapp directory: " + webappDir.getAbsolutePath() );
            if ( webappDir.exists() )
            {
                staticDeployer.setWebAppDir( IJetty.JETTY_DIR + "/" + IJetty.WEBAPP_DIR );
                staticDeployer.setDefaultsDescriptor( IJetty.JETTY_DIR + "/" + IJetty.ETC_DIR + "/webdefault.xml" );
                staticDeployer.setContexts( contexts );
                staticDeployer.setAttribute( CONTENT_RESOLVER_ATTRIBUTE, getContentResolver() );
                staticDeployer.setAttribute( ANDROID_CONTEXT_ATTRIBUTE, (Context) IJettyService.this );
                staticDeployer.setConfigurationClasses( configurationClasses );
                staticDeployer.setAllowDuplicates( false );
            }

            // Use a ContextDeploy so we can hot-deploy webapps and config at startup.
            if ( new File( jettyDir, IJetty.CONTEXTS_DIR ).exists() )
            {
                contextDeployer.setScanInterval( 10 ); // Don't eat the battery
                contextDeployer.setConfigurationDir( IJetty.JETTY_DIR + "/" + IJetty.CONTEXTS_DIR );
                contextDeployer.setAttribute( CONTENT_RESOLVER_ATTRIBUTE, getContentResolver() );
                contextDeployer.setAttribute( ANDROID_CONTEXT_ATTRIBUTE, (Context) IJettyService.this );
                contextDeployer.setContexts( contexts );
            }

            if ( server.get() != null )
            {
                Log.i( TAG, "Adding context deployer: " + contextDeployer);
                server.get().addBean( contextDeployer );
                Log.i( TAG, "Adding webapp deployer: " + staticDeployer );
                server.get().addBean( staticDeployer );
            }
        }
        else
        {
            Log.w( TAG, "Not loading any webapps - none on SD card." );
        }
    }

    public void configureRealm() throws IOException
    {
        File realmProps = new File( IJetty.JETTY_DIR + "/" + IJetty.ETC_DIR + "/realm.properties" );
        if ( realmProps.exists() )
        {
            HashLoginService realm = new HashLoginService( "Console", IJetty.JETTY_DIR + "/" + IJetty.ETC_DIR + "/realm.properties" );
            realm.setRefreshInterval( 0 );
            if ( consolePassword != null )
            {
                realm.putUser( "admin", Credential.getCredential( consolePassword ),
                        new String[]{ "admin" } ); //set the admin password for console webapp
            }
            server.get().addBean( realm );
        }
    }

    protected void startJetty() throws Exception
    {
        Log.i( TAG, "starting jetty server" );

        //Set jetty.home
        System.setProperty( "jetty.home", IJetty.JETTY_DIR.getAbsolutePath() );
        Log.i( TAG, "jetty.home: " + IJetty.JETTY_DIR.getAbsolutePath() );

        //ipv6 workaround for froyo
        System.setProperty( "java.net.preferIPv6Addresses", "false" );

        server.set( newServer() );

        configureConnectors();
        configureHandlers();
        configureDeployers();
        configureRealm();

        server.get().start();

        isRunning.set( true );

        //TODO
        // Less than ideal solution to the problem that dalvik doesn't know about manifests of jars.
        // A as the version field is private to Server, its difficult
        //if not impossible to set it any other way. Note this means that ContextHandler.SContext.getServerInfo()
        //will still return 0.0.
        HttpGenerator.setServerVersion( "i-jetty " + packageInfo.versionName );
    }

    protected void stopJetty() throws Exception
    {
        try
        {
            Log.i( TAG, "Jetty stopping" );
            server.get().stop();
            Log.i( TAG, "Jetty server stopped" );
            server.set( null );
            resources = null;
            isRunning.set( false );
        }
        finally
        {
            Log.i( TAG, "Finally stopped" );
        }
    }

    private class JettyStarterThread extends Thread
    {
        private final android.os.Handler handler;

        private JettyStarterThread( android.os.Handler handler )
        {
            this.handler = handler;
        }

        public void run()
        {
            try
            {
                sendMessage( STARTING );
                startJetty();
                sendMessage( STARTED );

                Log.i( TAG, "Jetty started" );
            }
            catch ( Exception e )
            {
                sendMessage( NOT_STARTED );
                Log.e( TAG, "Error starting jetty", e );
            }
        }

        public void sendMessage( int state )
        {
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt( "state", state );
            msg.setData( b );
            handler.sendMessage( msg );
        }
    }

    private class JettyStopperThread extends Thread
    {
        private final android.os.Handler handler;

        private JettyStopperThread( android.os.Handler handler )
        {
            this.handler = handler;
        }

        public void run()
        {
            try
            {
                sendMessage( STOPPING );
                stopJetty();
                Log.i( TAG, "Jetty stopped" );
                sendMessage( STOPPED );
            }
            catch ( Exception e )
            {
                sendMessage( NOT_STOPPED );
                Log.e( TAG, "Error stopping jetty", e );
            }
        }

        public void sendMessage( int state )
        {
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt( "state", state );
            msg.setData( b );
            handler.sendMessage( msg );
        }
    }

}
