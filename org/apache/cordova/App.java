package org.apache.cordova;

import java.util.HashMap;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class App extends CordovaPlugin {

    /* renamed from: org.apache.cordova.App.1 */
    class C00211 implements Runnable {
        C00211() {
        }

        public void run() {
            App.this.webView.postMessage("spinner", "stop");
        }
    }

    /* renamed from: org.apache.cordova.App.2 */
    class C00222 implements Runnable {
        C00222() {
        }

        public void run() {
            App.this.webView.clearCache(true);
        }
    }

    /* renamed from: org.apache.cordova.App.3 */
    class C00233 implements Runnable {
        C00233() {
        }

        public void run() {
            App.this.webView.backHistory();
        }
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Status status = Status.OK;
        String result = "";
        try {
            if (action.equals("clearCache")) {
                clearCache();
            } else if (action.equals("show")) {
                this.cordova.getActivity().runOnUiThread(new C00211());
            } else if (action.equals("loadUrl")) {
                loadUrl(args.getString(0), args.optJSONObject(1));
            } else if (!action.equals("cancelLoadUrl")) {
                if (action.equals("clearHistory")) {
                    clearHistory();
                } else if (action.equals("backHistory")) {
                    backHistory();
                } else if (action.equals("overrideButton")) {
                    overrideButton(args.getString(0), args.getBoolean(1));
                } else if (action.equals("overrideBackbutton")) {
                    overrideBackbutton(args.getBoolean(0));
                } else if (action.equals("exitApp")) {
                    exitApp();
                }
            }
            callbackContext.sendPluginResult(new PluginResult(status, result));
            return true;
        } catch (JSONException e) {
            callbackContext.sendPluginResult(new PluginResult(Status.JSON_EXCEPTION));
            return false;
        }
    }

    public void clearCache() {
        this.cordova.getActivity().runOnUiThread(new C00222());
    }

    public void loadUrl(String url, JSONObject props) throws JSONException {
        LOG.m0d("App", "App.loadUrl(" + url + "," + props + ")");
        int wait = 0;
        boolean openExternal = false;
        boolean clearHistory = false;
        HashMap<String, Object> params = new HashMap();
        if (props != null) {
            JSONArray keys = props.names();
            for (int i = 0; i < keys.length(); i++) {
                String key = keys.getString(i);
                if (key.equals("wait")) {
                    wait = props.getInt(key);
                } else if (key.equalsIgnoreCase("openexternal")) {
                    openExternal = props.getBoolean(key);
                } else if (key.equalsIgnoreCase("clearhistory")) {
                    clearHistory = props.getBoolean(key);
                } else {
                    Object value = props.get(key);
                    if (value != null) {
                        if (value.getClass().equals(String.class)) {
                            params.put(key, (String) value);
                        } else if (value.getClass().equals(Boolean.class)) {
                            params.put(key, (Boolean) value);
                        } else if (value.getClass().equals(Integer.class)) {
                            params.put(key, (Integer) value);
                        }
                    }
                }
            }
        }
        if (wait > 0) {
            try {
                synchronized (this) {
                    wait((long) wait);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.webView.showWebPage(url, openExternal, clearHistory, params);
    }

    public void clearHistory() {
        this.webView.clearHistory();
    }

    public void backHistory() {
        this.cordova.getActivity().runOnUiThread(new C00233());
    }

    public void overrideBackbutton(boolean override) {
        LOG.m6i("App", "WARNING: Back Button Default Behaviour will be overridden.  The backbutton event will be fired!");
        this.webView.bindButton(override);
    }

    public void overrideButton(String button, boolean override) {
        LOG.m6i("App", "WARNING: Volume Button Default Behaviour will be overridden.  The volume event will be fired!");
        this.webView.bindButton(button, override);
    }

    public boolean isBackbuttonOverridden() {
        return this.webView.isBackButtonBound();
    }

    public void exitApp() {
        this.webView.postMessage("exit", null);
    }
}
