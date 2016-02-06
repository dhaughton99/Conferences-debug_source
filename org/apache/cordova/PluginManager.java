package org.apache.cordova;

import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Debug;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

public class PluginManager {
    private static final int SLOW_EXEC_WARNING_THRESHOLD;
    private static String TAG;
    private final CordovaWebView app;
    private final CordovaInterface ctx;
    private final HashMap<String, PluginEntry> entries;
    private boolean firstRun;
    private AtomicInteger numPendingUiExecs;
    protected HashMap<String, List<String>> urlMap;

    /* renamed from: org.apache.cordova.PluginManager.1 */
    class C00491 implements Runnable {
        final /* synthetic */ String val$action;
        final /* synthetic */ String val$callbackId;
        final /* synthetic */ String val$rawArgs;
        final /* synthetic */ String val$service;

        C00491(String str, String str2, String str3, String str4) {
            this.val$service = str;
            this.val$action = str2;
            this.val$callbackId = str3;
            this.val$rawArgs = str4;
        }

        public void run() {
            PluginManager.this.execHelper(this.val$service, this.val$action, this.val$callbackId, this.val$rawArgs);
            PluginManager.this.numPendingUiExecs.getAndDecrement();
        }
    }

    private class PluginManagerService extends CordovaPlugin {

        /* renamed from: org.apache.cordova.PluginManager.PluginManagerService.1 */
        class C00501 implements Runnable {
            C00501() {
            }

            public void run() {
                PluginManager.this.numPendingUiExecs.getAndDecrement();
            }
        }

        private PluginManagerService() {
        }

        public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
            if (!"startup".equals(action)) {
                return false;
            }
            PluginManager.this.numPendingUiExecs.getAndIncrement();
            PluginManager.this.ctx.getActivity().runOnUiThread(new C00501());
            return true;
        }
    }

    static {
        TAG = "PluginManager";
        SLOW_EXEC_WARNING_THRESHOLD = Debug.isDebuggerConnected() ? 60 : 16;
    }

    public PluginManager(CordovaWebView app, CordovaInterface ctx) {
        this.entries = new HashMap();
        this.urlMap = new HashMap();
        this.ctx = ctx;
        this.app = app;
        this.firstRun = true;
        this.numPendingUiExecs = new AtomicInteger(0);
    }

    public void init() {
        LOG.m0d(TAG, "init()");
        if (this.firstRun) {
            loadPlugins();
            this.firstRun = false;
        } else {
            onPause(false);
            onDestroy();
            clearPluginObjects();
        }
        addService(new PluginEntry("PluginManager", new PluginManagerService()));
        startupPlugins();
    }

    public void loadPlugins() {
        int id = this.ctx.getActivity().getResources().getIdentifier("config", "xml", this.ctx.getActivity().getClass().getPackage().getName());
        if (id == 0) {
            id = this.ctx.getActivity().getResources().getIdentifier("config", "xml", this.ctx.getActivity().getPackageName());
            if (id == 0) {
                pluginConfigurationMissing();
                return;
            }
        }
        XmlResourceParser xml = this.ctx.getActivity().getResources().getXml(id);
        int eventType = -1;
        String service = "";
        String pluginClass = "";
        String paramType = "";
        boolean onload = false;
        boolean insideFeature = false;
        while (eventType != 1) {
            String strNode;
            if (eventType == 2) {
                strNode = xml.getName();
                if (strNode.equals("url-filter")) {
                    Log.w(TAG, "Plugin " + service + " is using deprecated tag <url-filter>");
                    if (this.urlMap.get(service) == null) {
                        this.urlMap.put(service, new ArrayList(2));
                    }
                    ((List) this.urlMap.get(service)).add(xml.getAttributeValue(null, "value"));
                } else if (strNode.equals("feature")) {
                    insideFeature = true;
                    service = xml.getAttributeValue(null, "name");
                } else if (insideFeature && strNode.equals("param")) {
                    paramType = xml.getAttributeValue(null, "name");
                    if (paramType.equals("service")) {
                        service = xml.getAttributeValue(null, "value");
                    } else if (paramType.equals("package") || paramType.equals("android-package")) {
                        pluginClass = xml.getAttributeValue(null, "value");
                    } else if (paramType.equals("onload")) {
                        onload = "true".equals(xml.getAttributeValue(null, "value"));
                    }
                }
            } else if (eventType == 3) {
                strNode = xml.getName();
                if (strNode.equals("feature") || strNode.equals("plugin")) {
                    addService(new PluginEntry(service, pluginClass, onload));
                    service = "";
                    pluginClass = "";
                    insideFeature = false;
                }
            }
            try {
                eventType = xml.next();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void clearPluginObjects() {
        for (PluginEntry entry : this.entries.values()) {
            entry.plugin = null;
        }
    }

    public void startupPlugins() {
        for (PluginEntry entry : this.entries.values()) {
            if (entry.onload) {
                entry.createPlugin(this.app, this.ctx);
            }
        }
    }

    public void exec(String service, String action, String callbackId, String rawArgs) {
        if (this.numPendingUiExecs.get() > 0) {
            this.numPendingUiExecs.getAndIncrement();
            this.ctx.getActivity().runOnUiThread(new C00491(service, action, callbackId, rawArgs));
            return;
        }
        execHelper(service, action, callbackId, rawArgs);
    }

    private void execHelper(String service, String action, String callbackId, String rawArgs) {
        CordovaPlugin plugin = getPlugin(service);
        if (plugin == null) {
            Log.d(TAG, "exec() call to unknown plugin: " + service);
            this.app.sendPluginResult(new PluginResult(Status.CLASS_NOT_FOUND_EXCEPTION), callbackId);
            return;
        }
        try {
            CallbackContext callbackContext = new CallbackContext(callbackId, this.app);
            long pluginStartTime = System.currentTimeMillis();
            boolean wasValidAction = plugin.execute(action, rawArgs, callbackContext);
            long duration = System.currentTimeMillis() - pluginStartTime;
            if (duration > ((long) SLOW_EXEC_WARNING_THRESHOLD)) {
                Log.w(TAG, "THREAD WARNING: exec() call to " + service + "." + action + " blocked the main thread for " + duration + "ms. Plugin should use CordovaInterface.getThreadPool().");
            }
            if (!wasValidAction) {
                this.app.sendPluginResult(new PluginResult(Status.INVALID_ACTION), callbackId);
            }
        } catch (JSONException e) {
            this.app.sendPluginResult(new PluginResult(Status.JSON_EXCEPTION), callbackId);
        }
    }

    @Deprecated
    public void exec(String service, String action, String callbackId, String jsonArgs, boolean async) {
        exec(service, action, callbackId, jsonArgs);
    }

    public CordovaPlugin getPlugin(String service) {
        PluginEntry entry = (PluginEntry) this.entries.get(service);
        if (entry == null) {
            return null;
        }
        CordovaPlugin plugin = entry.plugin;
        if (plugin == null) {
            return entry.createPlugin(this.app, this.ctx);
        }
        return plugin;
    }

    public void addService(String service, String className) {
        addService(new PluginEntry(service, className, false));
    }

    public void addService(PluginEntry entry) {
        this.entries.put(entry.service, entry);
    }

    public void onPause(boolean multitasking) {
        for (PluginEntry entry : this.entries.values()) {
            if (entry.plugin != null) {
                entry.plugin.onPause(multitasking);
            }
        }
    }

    public void onResume(boolean multitasking) {
        for (PluginEntry entry : this.entries.values()) {
            if (entry.plugin != null) {
                entry.plugin.onResume(multitasking);
            }
        }
    }

    public void onDestroy() {
        for (PluginEntry entry : this.entries.values()) {
            if (entry.plugin != null) {
                entry.plugin.onDestroy();
            }
        }
    }

    public Object postMessage(String id, Object data) {
        Object obj = this.ctx.onMessage(id, data);
        if (obj != null) {
            return obj;
        }
        for (PluginEntry entry : this.entries.values()) {
            if (entry.plugin != null) {
                obj = entry.plugin.onMessage(id, data);
                if (obj != null) {
                    return obj;
                }
            }
        }
        return null;
    }

    public void onNewIntent(Intent intent) {
        for (PluginEntry entry : this.entries.values()) {
            if (entry.plugin != null) {
                entry.plugin.onNewIntent(intent);
            }
        }
    }

    public boolean onOverrideUrlLoading(String url) {
        for (PluginEntry entry : this.entries.values()) {
            List<String> urlFilters = (List) this.urlMap.get(entry.service);
            if (urlFilters != null) {
                for (String s : urlFilters) {
                    if (url.startsWith(s)) {
                        return getPlugin(entry.service).onOverrideUrlLoading(url);
                    }
                }
                continue;
            } else if (entry.plugin != null && entry.plugin.onOverrideUrlLoading(url)) {
                return true;
            }
        }
        return false;
    }

    public void onReset() {
        for (PluginEntry pluginEntry : this.entries.values()) {
            CordovaPlugin plugin = pluginEntry.plugin;
            if (plugin != null) {
                plugin.onReset();
            }
        }
    }

    private void pluginConfigurationMissing() {
        LOG.m3e(TAG, "=====================================================================================");
        LOG.m3e(TAG, "ERROR: config.xml is missing.  Add res/xml/config.xml to your project.");
        LOG.m3e(TAG, "https://git-wip-us.apache.org/repos/asf?p=cordova-android.git;a=blob;f=framework/res/xml/config.xml");
        LOG.m3e(TAG, "=====================================================================================");
    }

    Uri remapUri(Uri uri) {
        for (PluginEntry entry : this.entries.values()) {
            if (entry.plugin != null) {
                Uri ret = entry.plugin.remapUri(uri);
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }
}
