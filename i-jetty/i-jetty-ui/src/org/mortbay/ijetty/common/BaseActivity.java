package org.mortbay.ijetty.common;

import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class BaseActivity extends Activity
{
    private static final AtomicInteger idGenerator = new AtomicInteger( 1 );

    private final int id = idGenerator.getAndIncrement();

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.i( getClass().getSimpleName(), "onDestroy(" + getId() + ')' );
    }

    @Override
    public void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        Log.i( getClass().getSimpleName(), "onDetachedFromWindow(" + getId() + ')' );
    }

    @Override
    protected void onNewIntent( Intent intent )
    {
        super.onNewIntent( intent );
        Log.i( getClass().getSimpleName(), "onNewIntent(" + getId() + ')' );
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.i( getClass().getSimpleName(), "onPause(" + getId() + ')' );
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        Log.i( getClass().getSimpleName(), "onRestart(" + getId() + ')' );
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.i( getClass().getSimpleName(), "onResume(" + getId() + ')' );
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.i( getClass().getSimpleName(), "onStart(" + getId() + ')' );
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.i( getClass().getSimpleName(), "onStop(" + getId() + ')' );
    }

    @Override
    public void onWindowFocusChanged( boolean hasFocus )
    {
        super.onWindowFocusChanged( hasFocus );
        Log.i( getClass().getSimpleName(), "onWindowFocusChanged(" + getId() + "): hasFocus: " + hasFocus );
    }

    protected String getId()
    {
        return "id: " + id + "@" + System.identityHashCode( this );
    }
}
