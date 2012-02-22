package org.mortbay.ijetty;

import android.content.Context;
import android.view.View.OnClickListener;

public abstract class OnClickListenerWithContext implements OnClickListener
{
    private final Context context;

    public OnClickListenerWithContext( Context context )
    {
        this.context = context;
    }

    public Context getContext()
    {
        return context;
    }
}
