package org.apache.cordova;

import android.content.Intent;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONException;

public class CordovaPlugin {
    static final /* synthetic */ boolean $assertionsDisabled;
    public CordovaInterface cordova;
    public String id;
    public CordovaWebView webView;

    static {
        $assertionsDisabled = !CordovaPlugin.class.desiredAssertionStatus();
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        if ($assertionsDisabled || this.cordova == null) {
            this.cordova = cordova;
            this.webView = webView;
            return;
        }
        throw new AssertionError();
    }

    public boolean execute(String action, String rawArgs, CallbackContext callbackContext) throws JSONException {
        return execute(action, new JSONArray(rawArgs), callbackContext);
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        return execute(action, new CordovaArgs(args), callbackContext);
    }

    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        return false;
    }

    public void onPause(boolean multitasking) {
    }

    public void onResume(boolean multitasking) {
    }

    public void onNewIntent(Intent intent) {
    }

    public void onDestroy() {
    }

    public Object onMessage(String id, Object data) {
        return null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    }

    public boolean onOverrideUrlLoading(String url) {
        return false;
    }

    public Uri remapUri(Uri uri) {
        return null;
    }

    public void onReset() {
    }
}
