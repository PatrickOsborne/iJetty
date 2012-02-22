package org.mortbay.ijetty;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Manage webapps.
 */
public class IJettyWebappManagementActivity extends Activity
{
    public static void show( Context context )
    {
        context.startActivity( new Intent( context, IJettyWebappManagementActivity.class ) );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        TextView view = new TextView( this );
        view.setText( "Webapp Management TBD" );
        setContentView( view );
    }

}
