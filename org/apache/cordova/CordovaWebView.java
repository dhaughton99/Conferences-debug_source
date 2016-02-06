package org.apache.cordova;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebHistoryItem;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.widget.FrameLayout.LayoutParams;
import com.squareup.okhttp.internal.http.HttpTransport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import org.apache.cordova.networkinformation.NetworkManager;

public class CordovaWebView extends WebView {
    public static final String CORDOVA_VERSION = "3.3.0";
    static final LayoutParams COVER_SCREEN_GRAVITY_CENTER;
    public static final String TAG = "CordovaWebView";
    private boolean bound;
    private CordovaChromeClient chromeClient;
    private CordovaInterface cordova;
    ExposedJsApi exposedJsApi;
    private boolean handleButton;
    NativeToJsMessageQueue jsMessageQueue;
    private ArrayList<Integer> keyDownCodes;
    private ArrayList<Integer> keyUpCodes;
    private long lastMenuEventTime;
    int loadUrlTimeout;
    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;
    private ActivityResult mResult;
    private boolean paused;
    public PluginManager pluginManager;
    private BroadcastReceiver receiver;
    private CordovaResourceApi resourceApi;
    private String url;
    CordovaWebViewClient viewClient;

    /* renamed from: org.apache.cordova.CordovaWebView.1 */
    class C00401 extends BroadcastReceiver {
        C00401() {
        }

        public void onReceive(Context context, Intent intent) {
            CordovaWebView.this.updateUserAgentString();
        }
    }

    /* renamed from: org.apache.cordova.CordovaWebView.2 */
    class C00412 implements Runnable {
        final /* synthetic */ CordovaWebView val$me;
        final /* synthetic */ String val$url;

        C00412(CordovaWebView cordovaWebView, String str) {
            this.val$me = cordovaWebView;
            this.val$url = str;
        }

        public void run() {
            this.val$me.stopLoading();
            LOG.m3e(CordovaWebView.TAG, "CordovaWebView: TIMEOUT ERROR!");
            if (CordovaWebView.this.viewClient != null) {
                CordovaWebView.this.viewClient.onReceivedError(this.val$me, -6, "The connection to the server was unsuccessful.", this.val$url);
            }
        }
    }

    /* renamed from: org.apache.cordova.CordovaWebView.3 */
    class C00423 implements Runnable {
        final /* synthetic */ int val$currentLoadUrlTimeout;
        final /* synthetic */ Runnable val$loadError;
        final /* synthetic */ int val$loadUrlTimeoutValue;
        final /* synthetic */ CordovaWebView val$me;

        C00423(int i, CordovaWebView cordovaWebView, int i2, Runnable runnable) {
            this.val$loadUrlTimeoutValue = i;
            this.val$me = cordovaWebView;
            this.val$currentLoadUrlTimeout = i2;
            this.val$loadError = runnable;
        }

        public void run() {
            try {
                synchronized (this) {
                    wait((long) this.val$loadUrlTimeoutValue);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (this.val$me.loadUrlTimeout == this.val$currentLoadUrlTimeout) {
                this.val$me.cordova.getActivity().runOnUiThread(this.val$loadError);
            }
        }
    }

    /* renamed from: org.apache.cordova.CordovaWebView.4 */
    class C00434 implements Runnable {
        final /* synthetic */ CordovaWebView val$me;
        final /* synthetic */ Runnable val$timeoutCheck;
        final /* synthetic */ String val$url;

        C00434(Runnable runnable, CordovaWebView cordovaWebView, String str) {
            this.val$timeoutCheck = runnable;
            this.val$me = cordovaWebView;
            this.val$url = str;
        }

        public void run() {
            new Thread(this.val$timeoutCheck).start();
            this.val$me.loadUrlNow(this.val$url);
        }
    }

    class ActivityResult {
        Intent incoming;
        int request;
        int result;

        public ActivityResult(int req, int res, Intent intent) {
            this.request = req;
            this.result = res;
            this.incoming = intent;
        }
    }

    @TargetApi(16)
    private static class Level16Apis {
        private Level16Apis() {
        }

        static void enableUniversalAccess(WebSettings settings) {
            settings.setAllowUniversalAccessFromFileURLs(true);
        }
    }

    static {
        COVER_SCREEN_GRAVITY_CENTER = new LayoutParams(-1, -1, 17);
    }

    public CordovaWebView(Context context) {
        super(context);
        this.keyDownCodes = new ArrayList();
        this.keyUpCodes = new ArrayList();
        this.loadUrlTimeout = 0;
        this.handleButton = false;
        this.lastMenuEventTime = 0;
        this.mResult = null;
        if (CordovaInterface.class.isInstance(context)) {
            this.cordova = (CordovaInterface) context;
        } else {
            Log.d(TAG, "Your activity must implement CordovaInterface to work");
        }
        loadConfiguration();
        setup();
    }

    public CordovaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.keyDownCodes = new ArrayList();
        this.keyUpCodes = new ArrayList();
        this.loadUrlTimeout = 0;
        this.handleButton = false;
        this.lastMenuEventTime = 0;
        this.mResult = null;
        if (CordovaInterface.class.isInstance(context)) {
            this.cordova = (CordovaInterface) context;
        } else {
            Log.d(TAG, "Your activity must implement CordovaInterface to work");
        }
        setWebChromeClient(new CordovaChromeClient(this.cordova, this));
        initWebViewClient(this.cordova);
        loadConfiguration();
        setup();
    }

    public CordovaWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.keyDownCodes = new ArrayList();
        this.keyUpCodes = new ArrayList();
        this.loadUrlTimeout = 0;
        this.handleButton = false;
        this.lastMenuEventTime = 0;
        this.mResult = null;
        if (CordovaInterface.class.isInstance(context)) {
            this.cordova = (CordovaInterface) context;
        } else {
            Log.d(TAG, "Your activity must implement CordovaInterface to work");
        }
        setWebChromeClient(new CordovaChromeClient(this.cordova, this));
        loadConfiguration();
        setup();
    }

    @TargetApi(11)
    public CordovaWebView(Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
        this.keyDownCodes = new ArrayList();
        this.keyUpCodes = new ArrayList();
        this.loadUrlTimeout = 0;
        this.handleButton = false;
        this.lastMenuEventTime = 0;
        this.mResult = null;
        if (CordovaInterface.class.isInstance(context)) {
            this.cordova = (CordovaInterface) context;
        } else {
            Log.d(TAG, "Your activity must implement CordovaInterface to work");
        }
        setWebChromeClient(new CordovaChromeClient(this.cordova));
        initWebViewClient(this.cordova);
        loadConfiguration();
        setup();
    }

    private void initWebViewClient(CordovaInterface cordova) {
        if (VERSION.SDK_INT < 11 || VERSION.SDK_INT > 17) {
            setWebViewClient(new CordovaWebViewClient(this.cordova, this));
        } else {
            setWebViewClient(new IceCreamCordovaWebViewClient(this.cordova, this));
        }
    }

    @SuppressLint({"NewApi"})
    private void setup() {
        setInitialScale(0);
        setVerticalScrollBarEnabled(false);
        if (shouldRequestFocusOnInit()) {
            requestFocusFromTouch();
        }
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
        try {
            Method gingerbread_getMethod = WebSettings.class.getMethod("setNavDump", new Class[]{Boolean.TYPE});
            Log.d(TAG, "CordovaWebView is running on device made by: " + Build.MANUFACTURER);
            if (VERSION.SDK_INT < 11 && Build.MANUFACTURER.contains("HTC")) {
                gingerbread_getMethod.invoke(settings, new Object[]{Boolean.valueOf(true)});
            }
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "We are on a modern version of Android, we will deprecate HTC 2.3 devices in 2.8");
        } catch (IllegalArgumentException e2) {
            Log.d(TAG, "Doing the NavDump failed with bad arguments");
        } catch (IllegalAccessException e3) {
            Log.d(TAG, "This should never happen: IllegalAccessException means this isn't Android anymore");
        } catch (InvocationTargetException e4) {
            Log.d(TAG, "This should never happen: InvocationTargetException means this isn't Android anymore.");
        }
        settings.setSaveFormData(false);
        settings.setSavePassword(false);
        if (VERSION.SDK_INT > 15) {
            Level16Apis.enableUniversalAccess(settings);
        }
        String databasePath = this.cordova.getActivity().getApplicationContext().getDir("database", 0).getPath();
        settings.setDatabaseEnabled(true);
        settings.setDatabasePath(databasePath);
        try {
            if ((this.cordova.getActivity().getPackageManager().getApplicationInfo(this.cordova.getActivity().getPackageName(), 128).flags & 2) != 0 && VERSION.SDK_INT >= 19) {
                setWebContentsDebuggingEnabled(true);
            }
        } catch (IllegalArgumentException e5) {
            Log.d(TAG, "You have one job! To turn on Remote Web Debugging! YOU HAVE FAILED! ");
            e5.printStackTrace();
        } catch (NameNotFoundException e6) {
            Log.d(TAG, "This should never happen: Your application's package can't be found.");
            e6.printStackTrace();
        }
        settings.setGeolocationDatabasePath(databasePath);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAppCacheMaxSize(5242880);
        settings.setAppCachePath(this.cordova.getActivity().getApplicationContext().getDir("database", 0).getPath());
        settings.setAppCacheEnabled(true);
        updateUserAgentString();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        if (this.receiver == null) {
            this.receiver = new C00401();
            this.cordova.getActivity().registerReceiver(this.receiver, intentFilter);
        }
        this.pluginManager = new PluginManager(this, this.cordova);
        this.jsMessageQueue = new NativeToJsMessageQueue(this, this.cordova);
        this.exposedJsApi = new ExposedJsApi(this.pluginManager, this.jsMessageQueue);
        this.resourceApi = new CordovaResourceApi(getContext(), this.pluginManager);
        exposeJsInterface();
    }

    protected boolean shouldRequestFocusOnInit() {
        return true;
    }

    private void updateUserAgentString() {
        getSettings().getUserAgentString();
    }

    private void exposeJsInterface() {
        int SDK_INT = VERSION.SDK_INT;
        boolean isHoneycomb = SDK_INT >= 11 && SDK_INT <= 13;
        if (isHoneycomb || SDK_INT < 9) {
            Log.i(TAG, "Disabled addJavascriptInterface() bridge since Android version is old.");
        } else if (SDK_INT >= 11 || !Build.MANUFACTURER.equals(NetworkManager.TYPE_UNKNOWN)) {
            addJavascriptInterface(this.exposedJsApi, "_cordovaNative");
        } else {
            Log.i(TAG, "Disabled addJavascriptInterface() bridge callback due to a bug on the 2.3 emulator");
        }
    }

    public void setWebViewClient(CordovaWebViewClient client) {
        this.viewClient = client;
        super.setWebViewClient(client);
    }

    public void setWebChromeClient(CordovaChromeClient client) {
        this.chromeClient = client;
        super.setWebChromeClient(client);
    }

    public CordovaChromeClient getWebChromeClient() {
        return this.chromeClient;
    }

    public void loadUrl(String url) {
        if (url.equals("about:blank") || url.startsWith("javascript:")) {
            loadUrlNow(url);
            return;
        }
        String initUrl = getProperty("url", null);
        if (initUrl == null) {
            loadUrlIntoView(url);
        } else {
            loadUrlIntoView(initUrl);
        }
    }

    public void loadUrl(String url, int time) {
        String initUrl = getProperty("url", null);
        if (initUrl == null) {
            loadUrlIntoView(url, time);
        } else {
            loadUrlIntoView(initUrl);
        }
    }

    public void loadUrlIntoView(String url) {
        LOG.m0d(TAG, ">>> loadUrl(" + url + ")");
        this.url = url;
        this.pluginManager.init();
        int currentLoadUrlTimeout = this.loadUrlTimeout;
        this.cordova.getActivity().runOnUiThread(new C00434(new C00423(Integer.parseInt(getProperty("LoadUrlTimeoutValue", "20000")), this, currentLoadUrlTimeout, new C00412(this, url)), this, url));
    }

    void loadUrlNow(String url) {
        if (LOG.isLoggable(3) && !url.startsWith("javascript:")) {
            LOG.m0d(TAG, ">>> loadUrlNow()");
        }
        if (url.startsWith("file://") || url.startsWith("javascript:") || Config.isUrlWhiteListed(url)) {
            super.loadUrl(url);
        }
    }

    public void loadUrlIntoView(String url, int time) {
        if (!(url.startsWith("javascript:") || canGoBack())) {
            LOG.m2d(TAG, "loadUrlIntoView(%s, %d)", url, Integer.valueOf(time));
            postMessage("splashscreen", "show");
        }
        loadUrlIntoView(url);
    }

    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        postMessage("onScrollChanged", new ScrollEvent(l, t, oldl, oldt, this));
    }

    public void sendJavascript(String statement) {
        this.jsMessageQueue.addJavaScript(statement);
    }

    public void sendPluginResult(PluginResult result, String callbackId) {
        this.jsMessageQueue.addPluginResult(result, callbackId);
    }

    public void postMessage(String id, Object data) {
        if (this.pluginManager != null) {
            this.pluginManager.postMessage(id, data);
        }
    }

    public boolean backHistory() {
        if (!super.canGoBack()) {
            return false;
        }
        printBackForwardList();
        super.goBack();
        return true;
    }

    public void showWebPage(String url, boolean openExternal, boolean clearHistory, HashMap<String, Object> hashMap) {
        LOG.m2d(TAG, "showWebPage(%s, %b, %b, HashMap", url, Boolean.valueOf(openExternal), Boolean.valueOf(clearHistory));
        if (clearHistory) {
            clearHistory();
        }
        Intent intent;
        if (openExternal) {
            try {
                intent = new Intent("android.intent.action.VIEW");
                intent.setData(Uri.parse(url));
                this.cordova.getActivity().startActivity(intent);
            } catch (Throwable e) {
                LOG.m4e(TAG, "Error loading url " + url, e);
            }
        } else if (url.startsWith("file://") || Config.isUrlWhiteListed(url)) {
            loadUrl(url);
        } else {
            LOG.m12w(TAG, "showWebPage: Cannot load URL into webview since it is not in white list.  Loading into browser instead. (URL=" + url + ")");
            try {
                intent = new Intent("android.intent.action.VIEW");
                intent.setData(Uri.parse(url));
                this.cordova.getActivity().startActivity(intent);
            } catch (Throwable e2) {
                LOG.m4e(TAG, "Error loading url " + url, e2);
            }
        }
    }

    private void loadConfiguration() {
        if ("true".equals(getProperty("Fullscreen", "false"))) {
            this.cordova.getActivity().getWindow().clearFlags(2048);
            this.cordova.getActivity().getWindow().setFlags(HttpTransport.DEFAULT_CHUNK_LENGTH, HttpTransport.DEFAULT_CHUNK_LENGTH);
        }
    }

    public String getProperty(String name, String defaultValue) {
        Bundle bundle = this.cordova.getActivity().getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        Object p = bundle.get(name.toLowerCase(Locale.getDefault()));
        return p != null ? p.toString() : defaultValue;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean z = false;
        if (this.keyDownCodes.contains(Integer.valueOf(keyCode))) {
            if (keyCode == 25) {
                LOG.m0d(TAG, "Down Key Hit");
                loadUrl("javascript:cordova.fireDocumentEvent('volumedownbutton');");
                return true;
            } else if (keyCode != 24) {
                return super.onKeyDown(keyCode, event);
            } else {
                LOG.m0d(TAG, "Up Key Hit");
                loadUrl("javascript:cordova.fireDocumentEvent('volumeupbutton');");
                return true;
            }
        } else if (keyCode == 4) {
            if (!startOfHistory() || this.bound) {
                z = true;
            }
            return z;
        } else if (keyCode != 82) {
            return super.onKeyDown(keyCode, event);
        } else {
            View childView = getFocusedChild();
            if (childView == null) {
                return super.onKeyDown(keyCode, event);
            }
            ((InputMethodManager) this.cordova.getActivity().getSystemService("input_method")).hideSoftInputFromWindow(childView.getWindowToken(), 0);
            this.cordova.getActivity().openOptionsMenu();
            return true;
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            if (this.mCustomView != null) {
                hideCustomView();
            } else if (this.bound) {
                loadUrl("javascript:cordova.fireDocumentEvent('backbutton');");
                return true;
            } else if (backHistory()) {
                return true;
            } else {
                this.cordova.getActivity().finish();
            }
        } else if (keyCode == 82) {
            if (this.lastMenuEventTime < event.getEventTime()) {
                loadUrl("javascript:cordova.fireDocumentEvent('menubutton');");
            }
            this.lastMenuEventTime = event.getEventTime();
            return super.onKeyUp(keyCode, event);
        } else if (keyCode == 84) {
            loadUrl("javascript:cordova.fireDocumentEvent('searchbutton');");
            return true;
        } else if (this.keyUpCodes.contains(Integer.valueOf(keyCode))) {
            return super.onKeyUp(keyCode, event);
        }
        return super.onKeyUp(keyCode, event);
    }

    public void bindButton(boolean override) {
        this.bound = override;
    }

    public void bindButton(String button, boolean override) {
        if (button.compareTo("volumeup") == 0) {
            this.keyDownCodes.add(Integer.valueOf(24));
        } else if (button.compareTo("volumedown") == 0) {
            this.keyDownCodes.add(Integer.valueOf(25));
        }
    }

    public void bindButton(int keyCode, boolean keyDown, boolean override) {
        if (keyDown) {
            this.keyDownCodes.add(Integer.valueOf(keyCode));
        } else {
            this.keyUpCodes.add(Integer.valueOf(keyCode));
        }
    }

    public boolean isBackButtonBound() {
        return this.bound;
    }

    public void handlePause(boolean keepRunning) {
        LOG.m0d(TAG, "Handle the pause");
        loadUrl("javascript:try{cordova.fireDocumentEvent('pause');}catch(e){console.log('exception firing pause event from native');};");
        if (this.pluginManager != null) {
            this.pluginManager.onPause(keepRunning);
        }
        if (!keepRunning) {
            pauseTimers();
        }
        this.paused = true;
    }

    public void handleResume(boolean keepRunning, boolean activityResultKeepRunning) {
        loadUrl("javascript:try{cordova.fireDocumentEvent('resume');}catch(e){console.log('exception firing resume event from native');};");
        if (this.pluginManager != null) {
            this.pluginManager.onResume(keepRunning);
        }
        resumeTimers();
        this.paused = false;
    }

    public void handleDestroy() {
        loadUrl("javascript:try{cordova.require('cordova/channel').onDestroy.fire();}catch(e){console.log('exception firing destroy event from native');};");
        loadUrl("about:blank");
        if (this.pluginManager != null) {
            this.pluginManager.onDestroy();
        }
        if (this.receiver != null) {
            try {
                this.cordova.getActivity().unregisterReceiver(this.receiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering configuration receiver: " + e.getMessage(), e);
            }
        }
    }

    public void onNewIntent(Intent intent) {
        if (this.pluginManager != null) {
            this.pluginManager.onNewIntent(intent);
        }
    }

    public boolean isPaused() {
        return this.paused;
    }

    public boolean hadKeyEvent() {
        return this.handleButton;
    }

    public void printBackForwardList() {
        WebBackForwardList currentList = copyBackForwardList();
        int currentSize = currentList.getSize();
        for (int i = 0; i < currentSize; i++) {
            LOG.m0d(TAG, "The URL at index: " + Integer.toString(i) + " is " + currentList.getItemAtIndex(i).getUrl());
        }
    }

    public boolean startOfHistory() {
        WebHistoryItem item = copyBackForwardList().getItemAtIndex(0);
        if (item == null) {
            return false;
        }
        String url = item.getUrl();
        String currentUrl = getUrl();
        LOG.m0d(TAG, "The current URL is: " + currentUrl);
        LOG.m0d(TAG, "The URL at item 0 is: " + url);
        return currentUrl.equals(url);
    }

    public void showCustomView(View view, CustomViewCallback callback) {
        Log.d(TAG, "showing Custom View");
        if (this.mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }
        this.mCustomView = view;
        this.mCustomViewCallback = callback;
        ViewGroup parent = (ViewGroup) getParent();
        parent.addView(view, COVER_SCREEN_GRAVITY_CENTER);
        setVisibility(8);
        parent.setVisibility(0);
        parent.bringToFront();
    }

    public void hideCustomView() {
        Log.d(TAG, "Hidding Custom View");
        if (this.mCustomView != null) {
            this.mCustomView.setVisibility(8);
            ((ViewGroup) getParent()).removeView(this.mCustomView);
            this.mCustomView = null;
            this.mCustomViewCallback.onCustomViewHidden();
            setVisibility(0);
        }
    }

    public boolean isCustomViewShowing() {
        return this.mCustomView != null;
    }

    public WebBackForwardList restoreState(Bundle savedInstanceState) {
        WebBackForwardList myList = super.restoreState(savedInstanceState);
        Log.d(TAG, "WebView restoration crew now restoring!");
        this.pluginManager.init();
        return myList;
    }

    public void storeResult(int requestCode, int resultCode, Intent intent) {
        this.mResult = new ActivityResult(requestCode, resultCode, intent);
    }

    public CordovaResourceApi getResourceApi() {
        return this.resourceApi;
    }
}
