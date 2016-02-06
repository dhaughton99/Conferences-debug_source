package org.apache.cordova;

import android.content.Context;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;

public class LinearLayoutSoftKeyboardDetect extends LinearLayout {
    private static final String TAG = "SoftKeyboardDetect";
    private CordovaActivity app;
    private int oldHeight;
    private int oldWidth;
    private int screenHeight;
    private int screenWidth;

    public LinearLayoutSoftKeyboardDetect(Context context, int width, int height) {
        super(context);
        this.oldHeight = 0;
        this.oldWidth = 0;
        this.screenWidth = 0;
        this.screenHeight = 0;
        this.app = null;
        this.screenWidth = width;
        this.screenHeight = height;
        this.app = (CordovaActivity) context;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        LOG.m9v(TAG, "We are in our onMeasure method");
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        LOG.m11v(TAG, "Old Height = %d", Integer.valueOf(this.oldHeight));
        LOG.m11v(TAG, "Height = %d", Integer.valueOf(height));
        LOG.m11v(TAG, "Old Width = %d", Integer.valueOf(this.oldWidth));
        LOG.m11v(TAG, "Width = %d", Integer.valueOf(width));
        if (this.oldHeight == 0 || this.oldHeight == height) {
            LOG.m0d(TAG, "Ignore this event");
        } else if (this.screenHeight == width) {
            int tmp_var = this.screenHeight;
            this.screenHeight = this.screenWidth;
            this.screenWidth = tmp_var;
            LOG.m9v(TAG, "Orientation Change");
        } else if (height > this.oldHeight) {
            if (this.app != null) {
                this.app.appView.sendJavascript("cordova.fireDocumentEvent('hidekeyboard');");
            }
        } else if (height < this.oldHeight && this.app != null) {
            this.app.appView.sendJavascript("cordova.fireDocumentEvent('showkeyboard');");
        }
        this.oldHeight = height;
        this.oldWidth = width;
    }
}
