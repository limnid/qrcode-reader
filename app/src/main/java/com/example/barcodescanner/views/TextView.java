package com.example.barcodescanner.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class TextView extends android.support.v7.widget.AppCompatTextView {
    AttributeSet attrs = null;
    Context context = null;
    Typeface sTypeface;

    public TextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public TextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.attrs = attrs;
        this.context = context;
        init(context);
    }

    public TextView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        if (sTypeface == null) {
            try {
                sTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/FontAwesome.ttf");
            } catch (Exception e) {
                sTypeface = Typeface.DEFAULT;
            }
        }
        setTypeface(sTypeface);
    }
}
