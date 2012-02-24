package org.mortbay.ijetty;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.mortbay.ijetty.IJettyService.IJettyServiceBinder;
import org.mortbay.ijetty.common.BaseActivity;
import org.mortbay.ijetty.common.LogSupport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

/**
 * Manage webapps.
 */
public class IJettyWebappManagementActivity extends BaseActivity
{
    private static final String JETTY_SERVER_NOT_STARTED = "Jetty server not started";
    private static final String JETTY_SERVER_STARTED = "Jetty server started";

    private final AtomicReference<IJettyService> serviceRef = new AtomicReference<IJettyService>();
    private TextView textView;
    private final ServiceConnectionImpl serviceConnection = new ServiceConnectionImpl();

    public static void show( Context context )
    {
        context.startActivity( new Intent( context, IJettyWebappManagementActivity.class ) );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // TODO
//        bindService( new Intent( this, IJettyService.class ), serviceConnection, Context.BIND_NOT_FOREGROUND );
        bindService( new Intent( this, IJettyService.class ), serviceConnection, 0 );

        textView = new TextView( this );
        textView.setText( "" );
        setContentView( textView );
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        IJettyService iJettyService = serviceRef.get();

        if ( iJettyService == null )
        {
            textView.setText( JETTY_SERVER_NOT_STARTED );
        }
        else
        {
            textView.setText( JETTY_SERVER_STARTED );
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unbindService( serviceConnection );
    }

    private void updateWebappInfo( Server server )
    {
        if ( server == null )
        {
            textView.setText( JETTY_SERVER_NOT_STARTED );
            return;
        }

        textView.setText( JETTY_SERVER_STARTED );
        Handler[] handlers = (server == null ? null : server.getChildHandlersByClass( ContextHandler.class ));
    }

    private class ServiceConnectionImpl implements ServiceConnection
    {
        public void onServiceConnected( ComponentName componentName, IBinder iBinder )
        {
            Log.i( LogSupport.TAG, "IJettyService connected" );
            IJettyServiceBinder jettyServiceBinder = (IJettyServiceBinder) iBinder;
            IJettyService service = jettyServiceBinder.getService();
            serviceRef.set( service );
            Log.i( LogSupport.TAG, "found IJettyService" );

            updateWebappInfo( service.getServer() );
        }

        public void onServiceDisconnected( ComponentName componentName )
        {
            Log.i( LogSupport.TAG, "IJettyService disconnect connected" );
            updateWebappInfo( null );
            serviceRef.set( null );
        }
    }
}
