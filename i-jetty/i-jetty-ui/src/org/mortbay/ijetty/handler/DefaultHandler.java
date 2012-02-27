package org.mortbay.ijetty.handler;

import static org.mortbay.ijetty.common.LogSupport.TAG;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.eclipse.jetty.util.StringUtil;

import android.util.Log;


public class DefaultHandler extends org.eclipse.jetty.server.handler.DefaultHandler
{
    public static final String FORMAT_STRING = "EEE dd MMM yyyy HH:mm:ss.SSS zzz";

    private final SimpleDateFormat format = new SimpleDateFormat( FORMAT_STRING );

    public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response )
            throws IOException, ServletException
    {
        if ( response.isCommitted() || baseRequest.isHandled() )
        {
            return;
        }

        baseRequest.setHandled( true );
        String method = request.getMethod();

        if ( !method.equals( HttpMethods.GET ) || !request.getRequestURI().equals( "/" ) )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        response.setStatus( HttpServletResponse.SC_NOT_FOUND );
        response.setContentType( MimeTypes.TEXT_HTML );

        // TODO remove?
        String uri = request.getRequestURI();
        uri = StringUtil.replace( uri, "<", "&lt;" );
        uri = StringUtil.replace( uri, ">", "&gt;" );

        ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer( 1500 );
        createHtml( request, writer );

        response.setContentLength( writer.size() );

        OutputStream out = response.getOutputStream();
        writer.writeTo( out );
        out.close();
    }

    private void createHtml( HttpServletRequest request, ByteArrayISO8859Writer writer ) throws IOException
    {
        writer.write( "<HTML>\n<HEAD>\n<TITLE>Welcome to i-jetty" );
        writer.write( "</TITLE>\n<BODY>\n<H2>Welcome to i-jetty</H2>\n" );
        writer.write( "<p>i-jetty is running successfully. (" + format.format( System.currentTimeMillis() ) + ")</p>" );

        Server server = getServer();
        Handler[] handlers = (server == null ? null : server.getChildHandlersByClass( ContextHandler.class ));

        int i;
        for ( i = 0; handlers != null && i < handlers.length; i++ )
        {
            if ( i == 0 )
            {
                writer.write( "<p>Available contexts are: </p><ul>" );
            }

            ContextHandler context = (ContextHandler) handlers[i];
            if ( context.isRunning() )
            {
                writer.write( "<li><a href=\"" );
                String url = getUrl( request, context );
                writer.write( url + "\">" );

                writer.write( context.getContextPath() );

                if ( context.getVirtualHosts() != null && context.getVirtualHosts().length > 0 )
                {
                    writer.write( "&nbsp;@&nbsp;" + context.getVirtualHosts()[0] + ":" + request.getLocalPort() );
                }

                writer.write( "&nbsp;-->&nbsp;" );
                writer.write( context.toString() );
                writer.write( "</a></li>\n" );
            }
            else
            {
                writer.write( "<li>" );
                writer.write( context.getContextPath() );
                if ( context.getVirtualHosts() != null && context.getVirtualHosts().length > 0 )
                {
                    writer.write( "&nbsp;@&nbsp;" + context.getVirtualHosts()[0] + ":" + request.getLocalPort() );
                }

                writer.write( "&nbsp;--->&nbsp;" );
                writer.write( context.toString() );
                if ( context.isFailed() )
                {
                    writer.write( " [failed]" );
                }

                if ( context.isStopped() )
                {
                    writer.write( " [stopped]" );
                }

                writer.write( "</li>\n" );
            }

            if ( i == handlers.length - 1 )
            {
                writer.write( "</ul>\n" );
            }
        }

        if ( i == 0 )
        {
            writer.write( "<p>There are currently no apps deployed.</p>" );
        }

        for ( int j = 0; j < 10; j++ )
        {
            writer.write( "\n<!-- Padding for IE                  -->" );
        }

        writer.write( "\n</BODY>\n</HTML>\n" );
        writer.flush();
    }

    public static String getUrl( HttpServletRequest request, ContextHandler context ) throws IOException
    {
        if ( context.getVirtualHosts() != null && context.getVirtualHosts().length > 0 )
        {
            return createUrl( request.getScheme(), context.getVirtualHosts()[0], request.getLocalPort(), context.getContextPath() );
        }
        else
        {
            Connector[] connectors = context.getServer().getConnectors();
            Log.d( TAG, "connectors for request: " + (connectors == null ? 0 : connectors.length) );
            if ( connectors != null && connectors.length > 0 )
            {
                for ( int i = 0; i < connectors.length; i++ )
                {
                    Connector connector = connectors[i];
                    if ( connector.getPort() == request.getLocalPort() )
                    {
                        Log.d( TAG, "found connector: " + connector.getLocalPort() + ": " + connector );
                        return createUrl( request.getScheme(), request.getLocalName(), request.getLocalPort(), context.getContextPath() );
                    }
                }
            }

            throw new RuntimeException( "no virtual hosts found" );
        }
    }

    public static String createUrl( String scheme, String host, int localPort, String contextPath )
    {
        return scheme + "://" + host + ":" + localPort + createContextPath( contextPath );
    }

    private static String createContextPath( String contextPath )
    {
        if ( contextPath.startsWith( "/" ) )
        {
            return contextPath;
        }
        return "/" + contextPath;
    }

}
