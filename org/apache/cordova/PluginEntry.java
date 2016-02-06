package org.apache.cordova;

public class PluginEntry {
    public boolean onload;
    public CordovaPlugin plugin;
    public String pluginClass;
    public String service;

    public PluginEntry(String service, String pluginClass, boolean onload) {
        this.service = "";
        this.pluginClass = "";
        this.plugin = null;
        this.onload = false;
        this.service = service;
        this.pluginClass = pluginClass;
        this.onload = onload;
    }

    public PluginEntry(String service, CordovaPlugin plugin) {
        this.service = "";
        this.pluginClass = "";
        this.plugin = null;
        this.onload = false;
        this.service = service;
        this.plugin = plugin;
        this.pluginClass = plugin.getClass().getName();
        this.onload = false;
    }

    public CordovaPlugin createPlugin(CordovaWebView webView, CordovaInterface ctx) {
        if (this.plugin != null) {
            return this.plugin;
        }
        try {
            Class c = getClassByName(this.pluginClass);
            if (isCordovaPlugin(c)) {
                this.plugin = (CordovaPlugin) c.newInstance();
                this.plugin.initialize(ctx, webView);
                return this.plugin;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error adding plugin " + this.pluginClass + ".");
        }
        return null;
    }

    private Class getClassByName(String clazz) throws ClassNotFoundException {
        if (clazz == null || "".equals(clazz)) {
            return null;
        }
        return Class.forName(clazz);
    }

    private boolean isCordovaPlugin(Class c) {
        if (c != null) {
            return CordovaPlugin.class.isAssignableFrom(c);
        }
        return false;
    }
}
