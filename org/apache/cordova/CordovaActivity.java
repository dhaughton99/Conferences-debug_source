package org.apache.cordova;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.ValueCallback;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.squareup.okhttp.internal.http.HttpTransport;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;

public class CordovaActivity extends Activity implements CordovaInterface {
    private static int ACTIVITY_EXITING;
    private static int ACTIVITY_RUNNING;
    private static int ACTIVITY_STARTING;
    public static String TAG;
    private Object LOG_TAG;
    protected CordovaPlugin activityResultCallback;
    protected boolean activityResultKeepRunning;
    private int activityState;
    protected CordovaWebView appView;
    private int backgroundColor;
    protected boolean cancelLoadUrl;
    private String initCallbackClass;
    protected boolean keepRunning;
    private Intent lastIntent;
    private int lastRequestCode;
    private Object lastResponseCode;
    protected int loadUrlTimeoutValue;
    private Object responseCode;
    protected LinearLayout root;
    protected ProgressDialog spinnerDialog;
    protected Dialog splashDialog;
    protected int splashscreen;
    protected int splashscreenTime;
    private final ExecutorService threadPool;
    protected CordovaWebViewClient webViewClient;

    /* renamed from: org.apache.cordova.CordovaActivity.1 */
    class C00241 implements OnCancelListener {
        final /* synthetic */ CordovaActivity val$me;

        C00241(CordovaActivity cordovaActivity) {
            this.val$me = cordovaActivity;
        }

        public void onCancel(DialogInterface dialog) {
            this.val$me.spinnerDialog = null;
        }
    }

    /* renamed from: org.apache.cordova.CordovaActivity.2 */
    class C00252 implements Runnable {
        final /* synthetic */ String val$errorUrl;
        final /* synthetic */ CordovaActivity val$me;

        C00252(CordovaActivity cordovaActivity, String str) {
            this.val$me = cordovaActivity;
            this.val$errorUrl = str;
        }

        public void run() {
            this.val$me.spinnerStop();
            this.val$me.appView.showWebPage(this.val$errorUrl, false, true, null);
        }
    }

    /* renamed from: org.apache.cordova.CordovaActivity.3 */
    class C00263 implements Runnable {
        final /* synthetic */ String val$description;
        final /* synthetic */ boolean val$exit;
        final /* synthetic */ String val$failingUrl;
        final /* synthetic */ CordovaActivity val$me;

        C00263(boolean z, CordovaActivity cordovaActivity, String str, String str2) {
            this.val$exit = z;
            this.val$me = cordovaActivity;
            this.val$description = str;
            this.val$failingUrl = str2;
        }

        public void run() {
            if (this.val$exit) {
                this.val$me.appView.setVisibility(8);
                this.val$me.displayError("Application Error", this.val$description + " (" + this.val$failingUrl + ")", "OK", this.val$exit);
            }
        }
    }

    /* renamed from: org.apache.cordova.CordovaActivity.4 */
    class C00284 implements Runnable {
        final /* synthetic */ String val$button;
        final /* synthetic */ boolean val$exit;
        final /* synthetic */ CordovaActivity val$me;
        final /* synthetic */ String val$message;
        final /* synthetic */ String val$title;

        /* renamed from: org.apache.cordova.CordovaActivity.4.1 */
        class C00271 implements OnClickListener {
            C00271() {
            }

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (C00284.this.val$exit) {
                    C00284.this.val$me.endActivity();
                }
            }
        }

        C00284(CordovaActivity cordovaActivity, String str, String str2, String str3, boolean z) {
            this.val$me = cordovaActivity;
            this.val$message = str;
            this.val$title = str2;
            this.val$button = str3;
            this.val$exit = z;
        }

        public void run() {
            try {
                Builder dlg = new Builder(this.val$me);
                dlg.setMessage(this.val$message);
                dlg.setTitle(this.val$title);
                dlg.setCancelable(false);
                dlg.setPositiveButton(this.val$button, new C00271());
                dlg.create();
                dlg.show();
            } catch (Exception e) {
                CordovaActivity.this.finish();
            }
        }
    }

    /* renamed from: org.apache.cordova.CordovaActivity.5 */
    class C00305 implements Runnable {
        final /* synthetic */ CordovaActivity val$that;
        final /* synthetic */ int val$time;

        /* renamed from: org.apache.cordova.CordovaActivity.5.1 */
        class C00291 implements Runnable {
            C00291() {
            }

            public void run() {
                CordovaActivity.this.removeSplashScreen();
            }
        }

        C00305(CordovaActivity cordovaActivity, int i) {
            this.val$that = cordovaActivity;
            this.val$time = i;
        }

        public void run() {
            Display display = CordovaActivity.this.getWindowManager().getDefaultDisplay();
            LinearLayout root = new LinearLayout(this.val$that.getActivity());
            root.setMinimumHeight(display.getHeight());
            root.setMinimumWidth(display.getWidth());
            root.setOrientation(1);
            root.setBackgroundColor(this.val$that.getIntegerProperty("backgroundColor", -16777216));
            root.setLayoutParams(new LayoutParams(-1, -1, 0.0f));
            root.setBackgroundResource(this.val$that.splashscreen);
            CordovaActivity.this.splashDialog = new Dialog(this.val$that, 16973840);
            if ((CordovaActivity.this.getWindow().getAttributes().flags & HttpTransport.DEFAULT_CHUNK_LENGTH) == HttpTransport.DEFAULT_CHUNK_LENGTH) {
                CordovaActivity.this.splashDialog.getWindow().setFlags(HttpTransport.DEFAULT_CHUNK_LENGTH, HttpTransport.DEFAULT_CHUNK_LENGTH);
            }
            CordovaActivity.this.splashDialog.setContentView(root);
            CordovaActivity.this.splashDialog.setCancelable(false);
            CordovaActivity.this.splashDialog.show();
            new Handler().postDelayed(new C00291(), (long) this.val$time);
        }
    }

    public CordovaActivity() {
        this.cancelLoadUrl = false;
        this.spinnerDialog = null;
        this.threadPool = Executors.newCachedThreadPool();
        this.activityState = 0;
        this.activityResultCallback = null;
        this.backgroundColor = -16777216;
        this.splashscreen = 0;
        this.splashscreenTime = 3000;
        this.loadUrlTimeoutValue = 20000;
        this.keepRunning = true;
    }

    static {
        TAG = "CordovaActivity";
        ACTIVITY_STARTING = 0;
        ACTIVITY_RUNNING = 1;
        ACTIVITY_EXITING = 2;
    }

    public void setAuthenticationToken(AuthenticationToken authenticationToken, String host, String realm) {
        if (this.appView != null && this.appView.viewClient != null) {
            this.appView.viewClient.setAuthenticationToken(authenticationToken, host, realm);
        }
    }

    public AuthenticationToken removeAuthenticationToken(String host, String realm) {
        if (this.appView == null || this.appView.viewClient == null) {
            return null;
        }
        return this.appView.viewClient.removeAuthenticationToken(host, realm);
    }

    public AuthenticationToken getAuthenticationToken(String host, String realm) {
        if (this.appView == null || this.appView.viewClient == null) {
            return null;
        }
        return this.appView.viewClient.getAuthenticationToken(host, realm);
    }

    public void clearAuthenticationTokens() {
        if (this.appView != null && this.appView.viewClient != null) {
            this.appView.viewClient.clearAuthenticationTokens();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        Config.init(this);
        LOG.m0d(TAG, "CordovaActivity.onCreate()");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.initCallbackClass = savedInstanceState.getString("callbackClass");
        }
        if (!getBooleanProperty("ShowTitle", false)) {
            getWindow().requestFeature(1);
        }
        if (getBooleanProperty("SetFullscreen", false)) {
            getWindow().setFlags(HttpTransport.DEFAULT_CHUNK_LENGTH, HttpTransport.DEFAULT_CHUNK_LENGTH);
        } else {
            getWindow().setFlags(2048, 2048);
        }
        Display display = getWindowManager().getDefaultDisplay();
        this.root = new LinearLayoutSoftKeyboardDetect(this, display.getWidth(), display.getHeight());
        this.root.setOrientation(1);
        this.root.setBackgroundColor(this.backgroundColor);
        this.root.setLayoutParams(new LayoutParams(-1, -1, 0.0f));
        setVolumeControlStream(3);
    }

    public Activity getActivity() {
        return this;
    }

    protected CordovaWebView makeWebView() {
        return new CordovaWebView(this);
    }

    protected CordovaWebViewClient makeWebViewClient(CordovaWebView webView) {
        if (VERSION.SDK_INT < 11) {
            return new CordovaWebViewClient(this, webView);
        }
        return new IceCreamCordovaWebViewClient(this, webView);
    }

    protected CordovaChromeClient makeChromeClient(CordovaWebView webView) {
        return new CordovaChromeClient(this, webView);
    }

    public void init() {
        CordovaWebView webView = makeWebView();
        init(webView, makeWebViewClient(webView), makeChromeClient(webView));
    }

    @SuppressLint({"NewApi"})
    public void init(CordovaWebView webView, CordovaWebViewClient webViewClient, CordovaChromeClient webChromeClient) {
        LOG.m0d(TAG, "CordovaActivity.init()");
        this.appView = webView;
        this.appView.setId(100);
        this.appView.setWebViewClient(webViewClient);
        this.appView.setWebChromeClient(webChromeClient);
        webViewClient.setWebView(this.appView);
        webChromeClient.setWebView(this.appView);
        this.appView.setLayoutParams(new LayoutParams(-1, -1, 1.0f));
        if (getBooleanProperty("DisallowOverscroll", false) && VERSION.SDK_INT >= 9) {
            this.appView.setOverScrollMode(2);
        }
        this.appView.setVisibility(4);
        this.root.addView(this.appView);
        setContentView(this.root);
        this.cancelLoadUrl = false;
    }

    public void loadUrl(String url) {
        if (this.appView == null) {
            init();
        }
        this.splashscreenTime = getIntegerProperty("SplashScreenDelay", this.splashscreenTime);
        if (this.splashscreenTime > 0) {
            this.splashscreen = getIntegerProperty("SplashScreen", 0);
            if (this.splashscreen != 0) {
                showSplashScreen(this.splashscreenTime);
            }
        }
        this.backgroundColor = getIntegerProperty("BackgroundColor", -16777216);
        this.root.setBackgroundColor(this.backgroundColor);
        this.keepRunning = getBooleanProperty("KeepRunning", true);
        if (this.appView.getParent() != null) {
            loadSpinner();
        }
        if (this.splashscreen != 0) {
            this.appView.loadUrl(url, this.splashscreenTime);
        } else {
            this.appView.loadUrl(url);
        }
    }

    public void loadUrl(String url, int time) {
        this.splashscreenTime = time;
        loadUrl(url);
    }

    void loadSpinner() {
        String loading;
        if (this.appView == null || !this.appView.canGoBack()) {
            loading = getStringProperty("LoadingDialog", null);
        } else {
            loading = getStringProperty("LoadingPageDialog", null);
        }
        if (loading != null) {
            String title = "";
            String message = "Loading Application...";
            if (loading.length() > 0) {
                int comma = loading.indexOf(44);
                if (comma > 0) {
                    title = loading.substring(0, comma);
                    message = loading.substring(comma + 1);
                } else {
                    title = "";
                    message = loading;
                }
            }
            spinnerStart(title, message);
        }
    }

    @Deprecated
    public void cancelLoadUrl() {
        this.cancelLoadUrl = true;
    }

    public void clearCache() {
        if (this.appView == null) {
            init();
        }
        this.appView.clearCache(true);
    }

    public void clearHistory() {
        this.appView.clearHistory();
    }

    public boolean backHistory() {
        if (this.appView != null) {
            return this.appView.backHistory();
        }
        return false;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue) {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        Boolean p;
        name = name.toLowerCase(Locale.getDefault());
        try {
            p = (Boolean) bundle.get(name);
        } catch (ClassCastException e) {
            if ("true".equals(bundle.get(name).toString())) {
                p = Boolean.valueOf(true);
            } else {
                p = Boolean.valueOf(false);
            }
        }
        return p != null ? p.booleanValue() : defaultValue;
    }

    public int getIntegerProperty(String name, int defaultValue) {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        Integer p;
        name = name.toLowerCase(Locale.getDefault());
        try {
            p = (Integer) bundle.get(name);
        } catch (ClassCastException e) {
            p = Integer.valueOf(Integer.parseInt(bundle.get(name).toString()));
        }
        return p != null ? p.intValue() : defaultValue;
    }

    public String getStringProperty(String name, String defaultValue) {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        String p = bundle.getString(name.toLowerCase(Locale.getDefault()));
        if (p != null) {
            return p;
        }
        return defaultValue;
    }

    public double getDoubleProperty(String name, double defaultValue) {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        Double p;
        name = name.toLowerCase(Locale.getDefault());
        try {
            p = (Double) bundle.get(name);
        } catch (ClassCastException e) {
            p = Double.valueOf(Double.parseDouble(bundle.get(name).toString()));
        }
        return p != null ? p.doubleValue() : defaultValue;
    }

    @Deprecated
    public void setBooleanProperty(String name, boolean value) {
        Log.d(TAG, "Setting boolean properties in CordovaActivity will be deprecated in 3.0 on July 2013, please use config.xml");
        getIntent().putExtra(name.toLowerCase(), value);
    }

    @Deprecated
    public void setIntegerProperty(String name, int value) {
        Log.d(TAG, "Setting integer properties in CordovaActivity will be deprecated in 3.0 on July 2013, please use config.xml");
        getIntent().putExtra(name.toLowerCase(), value);
    }

    @Deprecated
    public void setStringProperty(String name, String value) {
        Log.d(TAG, "Setting string properties in CordovaActivity will be deprecated in 3.0 on July 2013, please use config.xml");
        getIntent().putExtra(name.toLowerCase(), value);
    }

    @Deprecated
    public void setDoubleProperty(String name, double value) {
        Log.d(TAG, "Setting double properties in CordovaActivity will be deprecated in 3.0 on July 2013, please use config.xml");
        getIntent().putExtra(name.toLowerCase(), value);
    }

    protected void onPause() {
        super.onPause();
        LOG.m0d(TAG, "Paused the application!");
        if (this.activityState != ACTIVITY_EXITING && this.appView != null) {
            this.appView.handlePause(this.keepRunning);
            removeSplashScreen();
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (this.appView != null) {
            this.appView.onNewIntent(intent);
        }
    }

    protected void onResume() {
        super.onResume();
        Config.init(this);
        LOG.m0d(TAG, "Resuming the App");
        LOG.m0d(TAG, "CB-3064: The errorUrl is " + getStringProperty("ErrorUrl", null));
        if (this.activityState == ACTIVITY_STARTING) {
            this.activityState = ACTIVITY_RUNNING;
        } else if (this.appView != null) {
            this.appView.handleResume(this.keepRunning, this.activityResultKeepRunning);
            if ((!this.keepRunning || this.activityResultKeepRunning) && this.activityResultKeepRunning) {
                this.keepRunning = this.activityResultKeepRunning;
                this.activityResultKeepRunning = false;
            }
        }
    }

    public void onDestroy() {
        LOG.m0d(TAG, "CordovaActivity.onDestroy()");
        super.onDestroy();
        removeSplashScreen();
        if (this.appView != null) {
            this.appView.handleDestroy();
        } else {
            this.activityState = ACTIVITY_EXITING;
        }
    }

    public void postMessage(String id, Object data) {
        if (this.appView != null) {
            this.appView.postMessage(id, data);
        }
    }

    @Deprecated
    public void addService(String serviceType, String className) {
        if (this.appView != null && this.appView.pluginManager != null) {
            this.appView.pluginManager.addService(serviceType, className);
        }
    }

    public void sendJavascript(String statement) {
        if (this.appView != null) {
            this.appView.jsMessageQueue.addJavaScript(statement);
        }
    }

    public void spinnerStart(String title, String message) {
        if (this.spinnerDialog != null) {
            this.spinnerDialog.dismiss();
            this.spinnerDialog = null;
        }
        this.spinnerDialog = ProgressDialog.show(this, title, message, true, true, new C00241(this));
    }

    public void spinnerStop() {
        if (this.spinnerDialog != null && this.spinnerDialog.isShowing()) {
            this.spinnerDialog.dismiss();
            this.spinnerDialog = null;
        }
    }

    public void endActivity() {
        this.activityState = ACTIVITY_EXITING;
        super.finish();
    }

    public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {
        this.activityResultCallback = command;
        this.activityResultKeepRunning = this.keepRunning;
        if (command != null) {
            this.keepRunning = false;
        }
        super.startActivityForResult(intent, requestCode);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        LOG.m0d(TAG, "Incoming Result");
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(TAG, "Request code = " + requestCode);
        if (this.appView != null && requestCode == CordovaChromeClient.FILECHOOSER_RESULTCODE) {
            ValueCallback<Uri> mUploadMessage = this.appView.getWebChromeClient().getValueCallback();
            Log.d(TAG, "did we get here?");
            if (mUploadMessage != null) {
                Uri result = (intent == null || resultCode != -1) ? null : intent.getData();
                Log.d(TAG, "result = " + result);
                mUploadMessage.onReceiveValue(result);
            } else {
                return;
            }
        }
        CordovaPlugin callback = this.activityResultCallback;
        if (callback == null && this.initCallbackClass != null) {
            this.activityResultCallback = this.appView.pluginManager.getPlugin(this.initCallbackClass);
            callback = this.activityResultCallback;
        }
        if (callback != null) {
            LOG.m0d(TAG, "We have a callback to send this result to");
            callback.onActivityResult(requestCode, resultCode, intent);
        }
    }

    public void setActivityResultCallback(CordovaPlugin plugin) {
        this.activityResultCallback = plugin;
    }

    public void onReceivedError(int errorCode, String description, String failingUrl) {
        String errorUrl = getStringProperty("errorUrl", null);
        if (errorUrl == null || (!(errorUrl.startsWith("file://") || Config.isUrlWhiteListed(errorUrl)) || failingUrl.equals(errorUrl))) {
            runOnUiThread(new C00263(errorCode != -2, me, description, failingUrl));
        } else {
            runOnUiThread(new C00252(me, errorUrl));
        }
    }

    public void displayError(String title, String message, String button, boolean exit) {
        runOnUiThread(new C00284(this, message, title, button, exit));
    }

    public boolean isUrlWhiteListed(String url) {
        return Config.isUrlWhiteListed(url);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        postMessage("onCreateOptionsMenu", menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        postMessage("onPrepareOptionsMenu", menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        postMessage("onOptionsItemSelected", item);
        return true;
    }

    @Deprecated
    public Context getContext() {
        LOG.m0d(TAG, "This will be deprecated December 2012");
        return this;
    }

    public void showWebPage(String url, boolean openExternal, boolean clearHistory, HashMap<String, Object> params) {
        if (this.appView != null) {
            this.appView.showWebPage(url, openExternal, clearHistory, params);
        }
    }

    public void removeSplashScreen() {
        if (this.splashDialog != null && this.splashDialog.isShowing()) {
            this.splashDialog.dismiss();
            this.splashDialog = null;
        }
    }

    protected void showSplashScreen(int time) {
        runOnUiThread(new C00305(this, time));
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (this.appView == null || ((!this.appView.isCustomViewShowing() && this.appView.getFocusedChild() == null) || (keyCode != 4 && keyCode != 82))) {
            return super.onKeyUp(keyCode, event);
        }
        return this.appView.onKeyUp(keyCode, event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.appView == null || this.appView.getFocusedChild() == null || (keyCode != 4 && keyCode != 82)) {
            return super.onKeyDown(keyCode, event);
        }
        return this.appView.onKeyDown(keyCode, event);
    }

    public Object onMessage(String id, Object data) {
        LOG.m0d(TAG, "onMessage(" + id + "," + data + ")");
        if ("splashscreen".equals(id)) {
            if ("hide".equals(data.toString())) {
                removeSplashScreen();
            } else if (this.splashDialog == null || !this.splashDialog.isShowing()) {
                this.splashscreen = getIntegerProperty("SplashScreen", 0);
                showSplashScreen(this.splashscreenTime);
            }
        } else if ("spinner".equals(id)) {
            if ("stop".equals(data.toString())) {
                spinnerStop();
                this.appView.setVisibility(0);
            }
        } else if ("onReceivedError".equals(id)) {
            JSONObject d = (JSONObject) data;
            try {
                onReceivedError(d.getInt("errorCode"), d.getString("description"), d.getString("url"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if ("exit".equals(id)) {
            endActivity();
        }
        return null;
    }

    public ExecutorService getThreadPool() {
        return this.threadPool;
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.activityResultCallback != null) {
            outState.putString("callbackClass", this.activityResultCallback.getClass().getName());
        }
    }
}
