package org.apache.cordova;

import android.webkit.JavascriptInterface;
import org.json.JSONException;

class ExposedJsApi {
    private NativeToJsMessageQueue jsMessageQueue;
    private PluginManager pluginManager;

    public ExposedJsApi(PluginManager pluginManager, NativeToJsMessageQueue jsMessageQueue) {
        this.pluginManager = pluginManager;
        this.jsMessageQueue = jsMessageQueue;
    }

    @JavascriptInterface
    public String exec(String service, String action, String callbackId, String arguments) throws JSONException {
        String ret;
        if (arguments == null) {
            return "@Null arguments.";
        }
        this.jsMessageQueue.setPaused(true);
        try {
            CordovaResourceApi.jsThread = Thread.currentThread();
            this.pluginManager.exec(service, action, callbackId, arguments);
            ret = "";
            ret = this.jsMessageQueue.popAndEncode(false);
            return ret;
        } catch (Throwable e) {
            e.printStackTrace();
            ret = "";
            return ret;
        } finally {
            this.jsMessageQueue.setPaused(false);
        }
    }

    @JavascriptInterface
    public void setNativeToJsBridgeMode(int value) {
        this.jsMessageQueue.setBridgeMode(value);
    }

    @JavascriptInterface
    public String retrieveJsMessages(boolean fromOnlineEvent) {
        return this.jsMessageQueue.popAndEncode(fromOnlineEvent);
    }
}
