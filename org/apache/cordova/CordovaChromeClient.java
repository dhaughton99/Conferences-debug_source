package org.apache.cordova;

import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebStorage.QuotaUpdater;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout.LayoutParams;
import org.json.JSONArray;
import org.json.JSONException;

public class CordovaChromeClient extends WebChromeClient {
    public static final int FILECHOOSER_RESULTCODE = 5173;
    private static final String LOG_TAG = "CordovaChromeClient";
    private long MAX_QUOTA;
    private String TAG;
    protected CordovaWebView appView;
    protected CordovaInterface cordova;
    public ValueCallback<Uri> mUploadMessage;
    private View mVideoProgressView;

    /* renamed from: org.apache.cordova.CordovaChromeClient.1 */
    class C00311 implements OnClickListener {
        final /* synthetic */ JsResult val$result;

        C00311(JsResult jsResult) {
            this.val$result = jsResult;
        }

        public void onClick(DialogInterface dialog, int which) {
            this.val$result.confirm();
        }
    }

    /* renamed from: org.apache.cordova.CordovaChromeClient.2 */
    class C00322 implements OnCancelListener {
        final /* synthetic */ JsResult val$result;

        C00322(JsResult jsResult) {
            this.val$result = jsResult;
        }

        public void onCancel(DialogInterface dialog) {
            this.val$result.cancel();
        }
    }

    /* renamed from: org.apache.cordova.CordovaChromeClient.3 */
    class C00333 implements OnKeyListener {
        final /* synthetic */ JsResult val$result;

        C00333(JsResult jsResult) {
            this.val$result = jsResult;
        }

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode != 4) {
                return true;
            }
            this.val$result.confirm();
            return false;
        }
    }

    /* renamed from: org.apache.cordova.CordovaChromeClient.4 */
    class C00344 implements OnClickListener {
        final /* synthetic */ JsResult val$result;

        C00344(JsResult jsResult) {
            this.val$result = jsResult;
        }

        public void onClick(DialogInterface dialog, int which) {
            this.val$result.confirm();
        }
    }

    /* renamed from: org.apache.cordova.CordovaChromeClient.5 */
    class C00355 implements OnClickListener {
        final /* synthetic */ JsResult val$result;

        C00355(JsResult jsResult) {
            this.val$result = jsResult;
        }

        public void onClick(DialogInterface dialog, int which) {
            this.val$result.cancel();
        }
    }

    /* renamed from: org.apache.cordova.CordovaChromeClient.6 */
    class C00366 implements OnCancelListener {
        final /* synthetic */ JsResult val$result;

        C00366(JsResult jsResult) {
            this.val$result = jsResult;
        }

        public void onCancel(DialogInterface dialog) {
            this.val$result.cancel();
        }
    }

    /* renamed from: org.apache.cordova.CordovaChromeClient.7 */
    class C00377 implements OnKeyListener {
        final /* synthetic */ JsResult val$result;

        C00377(JsResult jsResult) {
            this.val$result = jsResult;
        }

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode != 4) {
                return true;
            }
            this.val$result.cancel();
            return false;
        }
    }

    /* renamed from: org.apache.cordova.CordovaChromeClient.8 */
    class C00388 implements OnClickListener {
        final /* synthetic */ EditText val$input;
        final /* synthetic */ JsPromptResult val$res;

        C00388(EditText editText, JsPromptResult jsPromptResult) {
            this.val$input = editText;
            this.val$res = jsPromptResult;
        }

        public void onClick(DialogInterface dialog, int which) {
            this.val$res.confirm(this.val$input.getText().toString());
        }
    }

    /* renamed from: org.apache.cordova.CordovaChromeClient.9 */
    class C00399 implements OnClickListener {
        final /* synthetic */ JsPromptResult val$res;

        C00399(JsPromptResult jsPromptResult) {
            this.val$res = jsPromptResult;
        }

        public void onClick(DialogInterface dialog, int which) {
            this.val$res.cancel();
        }
    }

    public CordovaChromeClient(CordovaInterface cordova) {
        this.TAG = "CordovaLog";
        this.MAX_QUOTA = 104857600;
        this.cordova = cordova;
    }

    public CordovaChromeClient(CordovaInterface ctx, CordovaWebView app) {
        this.TAG = "CordovaLog";
        this.MAX_QUOTA = 104857600;
        this.cordova = ctx;
        this.appView = app;
    }

    public void setWebView(CordovaWebView view) {
        this.appView = view;
    }

    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        Builder dlg = new Builder(this.cordova.getActivity());
        dlg.setMessage(message);
        dlg.setTitle("Alert");
        dlg.setCancelable(true);
        dlg.setPositiveButton(17039370, new C00311(result));
        dlg.setOnCancelListener(new C00322(result));
        dlg.setOnKeyListener(new C00333(result));
        dlg.create();
        dlg.show();
        return true;
    }

    public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
        Builder dlg = new Builder(this.cordova.getActivity());
        dlg.setMessage(message);
        dlg.setTitle("Confirm");
        dlg.setCancelable(true);
        dlg.setPositiveButton(17039370, new C00344(result));
        dlg.setNegativeButton(17039360, new C00355(result));
        dlg.setOnCancelListener(new C00366(result));
        dlg.setOnKeyListener(new C00377(result));
        dlg.create();
        dlg.show();
        return true;
    }

    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        boolean reqOk = false;
        if (url.startsWith("file://") || Config.isUrlWhiteListed(url)) {
            reqOk = true;
        }
        String r;
        if (!reqOk || defaultValue == null || defaultValue.length() <= 3 || !defaultValue.substring(0, 4).equals("gap:")) {
            if (reqOk && defaultValue != null) {
                if (defaultValue.equals("gap_bridge_mode:")) {
                    try {
                        this.appView.exposedJsApi.setNativeToJsBridgeMode(Integer.parseInt(message));
                        result.confirm("");
                    } catch (NumberFormatException e) {
                        result.confirm("");
                        e.printStackTrace();
                    }
                }
            }
            if (reqOk && defaultValue != null) {
                if (defaultValue.equals("gap_poll:")) {
                    r = this.appView.exposedJsApi.retrieveJsMessages("1".equals(message));
                    if (r == null) {
                        r = "";
                    }
                    result.confirm(r);
                }
            }
            if (defaultValue != null) {
                if (defaultValue.equals("gap_init:")) {
                    result.confirm("OK");
                }
            }
            JsPromptResult res = result;
            Builder dlg = new Builder(this.cordova.getActivity());
            dlg.setMessage(message);
            EditText input = new EditText(this.cordova.getActivity());
            if (defaultValue != null) {
                input.setText(defaultValue);
            }
            dlg.setView(input);
            dlg.setCancelable(false);
            dlg.setPositiveButton(17039370, new C00388(input, res));
            dlg.setNegativeButton(17039360, new C00399(res));
            dlg.create();
            dlg.show();
        } else {
            try {
                JSONArray array = new JSONArray(defaultValue.substring(4));
                r = this.appView.exposedJsApi.exec(array.getString(0), array.getString(1), array.getString(2), message);
                if (r == null) {
                    r = "";
                }
                result.confirm(r);
            } catch (JSONException e2) {
                e2.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota, long estimatedSize, long totalUsedQuota, QuotaUpdater quotaUpdater) {
        LOG.m2d(this.TAG, "onExceededDatabaseQuota estimatedSize: %d  currentQuota: %d  totalUsedQuota: %d", Long.valueOf(estimatedSize), Long.valueOf(currentQuota), Long.valueOf(totalUsedQuota));
        quotaUpdater.updateQuota(this.MAX_QUOTA);
    }

    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
        if (VERSION.SDK_INT == 7) {
            LOG.m2d(this.TAG, "%s: Line %d : %s", sourceID, Integer.valueOf(lineNumber), message);
            super.onConsoleMessage(message, lineNumber, sourceID);
        }
    }

    @TargetApi(8)
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        if (consoleMessage.message() != null) {
            LOG.m2d(this.TAG, "%s: Line %d : %s", consoleMessage.sourceId(), Integer.valueOf(consoleMessage.lineNumber()), consoleMessage.message());
        }
        return super.onConsoleMessage(consoleMessage);
    }

    public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
        super.onGeolocationPermissionsShowPrompt(origin, callback);
        callback.invoke(origin, true, false);
    }

    public void onShowCustomView(View view, CustomViewCallback callback) {
        this.appView.showCustomView(view, callback);
    }

    public void onHideCustomView() {
        this.appView.hideCustomView();
    }

    public View getVideoLoadingProgressView() {
        if (this.mVideoProgressView == null) {
            LinearLayout layout = new LinearLayout(this.appView.getContext());
            layout.setOrientation(1);
            LayoutParams layoutParams = new LayoutParams(-2, -2);
            layoutParams.addRule(13);
            layout.setLayoutParams(layoutParams);
            ProgressBar bar = new ProgressBar(this.appView.getContext());
            LinearLayout.LayoutParams barLayoutParams = new LinearLayout.LayoutParams(-2, -2);
            barLayoutParams.gravity = 17;
            bar.setLayoutParams(barLayoutParams);
            layout.addView(bar);
            this.mVideoProgressView = layout;
        }
        return this.mVideoProgressView;
    }

    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        openFileChooser(uploadMsg, "*/*");
    }

    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        openFileChooser(uploadMsg, acceptType, null);
    }

    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        this.mUploadMessage = uploadMsg;
        Intent i = new Intent("android.intent.action.GET_CONTENT");
        i.addCategory("android.intent.category.OPENABLE");
        i.setType("*/*");
        this.cordova.getActivity().startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE);
    }

    public ValueCallback<Uri> getValueCallback() {
        return this.mUploadMessage;
    }
}
