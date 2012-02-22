//========================================================================
//$Id: AndroidLog.java 391 2011-02-08 01:06:04Z janb.webtide $
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

package org.mortbay.ijetty.log;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.util.log.Logger;

import android.util.Log;

public class AndroidLog implements Logger
{
    public static final String LOG_TAG = "Jetty";

    public static final AtomicBoolean isIgnoredEnabled = new AtomicBoolean();

    public final String name;

    public AndroidLog()
    {
        this( "org.mortbay.ijetty.AndroidLog" );
    }

    public AndroidLog( String name )
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void debug( Throwable th )
    {
        if ( Log.isLoggable( LOG_TAG, Log.DEBUG ) )
        {
            Log.d( LOG_TAG, "", th );
        }
    }

    public void debug( String msg, Throwable th )
    {
        if ( Log.isLoggable( LOG_TAG, Log.DEBUG ) )
        {
            Log.d( LOG_TAG, msg, th );
        }
    }

    public void debug( String msg, Object... args )
    {
        if ( Log.isLoggable( LOG_TAG, Log.DEBUG ) )
        {
            Log.d( LOG_TAG, msg );
        }
    }

    public Logger getLogger( String name )
    {
        return new AndroidLog( name );
    }

    public void info( String msg, Object... args )
    {
        Log.i( LOG_TAG, msg );
    }

    public void info( Throwable th )
    {
        Log.i( LOG_TAG, "", th );
    }

    public void info( String msg, Throwable th )
    {
        Log.i( LOG_TAG, msg, th );
    }

    public boolean isDebugEnabled()
    {
        return Log.isLoggable( LOG_TAG, Log.DEBUG );
    }

    public void warn( Throwable th )
    {
        if ( Log.isLoggable( LOG_TAG, Log.WARN ) )
        {
            Log.e( LOG_TAG, "", th );
        }
    }

    public void warn( String msg, Object... args )
    {
        if ( Log.isLoggable( LOG_TAG, Log.WARN ) )
        {
            Log.w( LOG_TAG, msg );
        }
    }

    public void warn( String msg, Throwable th )
    {
        if ( Log.isLoggable( LOG_TAG, Log.ERROR ) )
        {
            Log.e( LOG_TAG, msg, th );
        }
    }

    public boolean isIgnoredEnabled()
    {
        return isIgnoredEnabled.get();
    }

    public void ignore( Throwable ignored )
    {
        if ( isIgnoredEnabled.get() )
        {
            Log.w( LOG_TAG, "IGNORED", ignored );
        }
    }

    public void setIgnoredEnabled( boolean enabled )
    {
        isIgnoredEnabled.set( enabled );
    }

    public void setDebugEnabled( boolean enabled )
    {
    }
}
