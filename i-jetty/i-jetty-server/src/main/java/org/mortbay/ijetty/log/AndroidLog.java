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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AndroidLog implements org.eclipse.jetty.util.log.Logger
{
    private static final Logger LOGGER = LoggerFactory.getLogger( "Jetty" );

    private final Logger logger;

    public AndroidLog()
    {
        this( LOGGER );
    }

    public AndroidLog( Logger logger )
    {
        this.logger = logger;
    }

    public AndroidLog( String name )
    {
        this(LoggerFactory.getLogger( name ));
    }

    public org.eclipse.jetty.util.log.Logger getLogger( String name )
    {
        return new AndroidLog( name );
    }

    public String getName()
    {
        return logger.getName();
    }

    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    public boolean isErrorEnabled()
    {
        return logger.isErrorEnabled();
    }

    public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    public boolean isTraceEnabled()
    {
        return logger.isTraceEnabled();
    }

    public boolean isWarnEnabled()
    {
        return logger.isWarnEnabled();
    }

    public void debug( String msg, Object arg )
    {
        logger.debug( msg, arg );
    }

    public void debug( String msg, Object arg, Object arg1 )
    {
        logger.debug( msg, arg, arg1 );
    }

    public void debug( String msg, Object[] args )
    {
        logger.debug( msg, args );
    }

    public void debug( String msg, Throwable throwable )
    {
        logger.debug( msg, throwable );
    }

    public void debug( Throwable throwable )
    {
        logger.debug( "", throwable );
    }

    public void info( String msg, Object arg )
    {
        logger.info( msg, arg );
    }

    public void info( String msg, Object arg, Object arg1 )
    {
        logger.info( msg, arg, arg1 );
    }

    public void info( String msg, Object[] args )
    {
        logger.info( msg, args );
    }

    public void info( String msg, Throwable throwable )
    {
        logger.info( msg, throwable );
    }

    public void info( Throwable throwable )
    {
        logger.info( "", throwable );
    }

    public void warn( String msg, Object arg )
    {
        logger.warn( msg, arg );
    }

    public void warn( String msg, Object arg, Object arg1 )
    {
        logger.warn( msg, arg, arg1 );
    }

    public void warn( String msg, Object[] args )
    {
        logger.warn( msg, args );
    }

    public void warn( String msg, Throwable throwable )
    {
        logger.warn( msg, throwable );
    }

    public void warn( Throwable throwable )
    {
        logger.warn( "", throwable );
    }

    public void ignore( Throwable ignored )
    {
        warn( "IGNORED ", ignored );
    }

    public void setDebugEnabled( boolean enabled )
    {
    }
}
