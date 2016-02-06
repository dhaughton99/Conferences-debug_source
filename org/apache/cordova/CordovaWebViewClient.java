package org.apache.cordova;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.Hashtable;
import org.json.JSONException;
import org.json.JSONObject;

public class CordovaWebViewClient extends WebViewClient {
    private static final String CORDOVA_EXEC_URL_PREFIX = "http://cdv_exec/";
    private static final String TAG = "CordovaWebViewClient";
    CordovaWebView appView;
    private Hashtable<String, AuthenticationToken> authenticationTokens;
    CordovaInterface cordova;
    private boolean doClearHistory;

    /* renamed from: org.apache.cordova.CordovaWebViewClient.1 */
    class C00451 implements Runnable {

        /* renamed from: org.apache.cordova.CordovaWebViewClient.1.1 */
        class C00441 implements Runnable {
            C00441() {
            }

            public void run() {
                CordovaWebViewClient.this.appView.postMessage("spinner", "stop");
            }
        }

        C00451() {
        }

        public void run() {
            try {
                Thread.sleep(2000);
                CordovaWebViewClient.this.cordova.getActivity().runOnUiThread(new C00441());
            } catch (InterruptedException e) {
            }
        }
    }

    public CordovaWebViewClient(CordovaInterface cordova) {
        this.doClearHistory = false;
        this.authenticationTokens = new Hashtable();
        this.cordova = cordova;
    }

    public CordovaWebViewClient(CordovaInterface cordova, CordovaWebView view) {
        this.doClearHistory = false;
        this.authenticationTokens = new Hashtable();
        this.cordova = cordova;
        this.appView = view;
    }

    public void setWebView(CordovaWebView view) {
        this.appView = view;
    }

    private void handleExecUrl(String url) {
        int idx1 = CORDOVA_EXEC_URL_PREFIX.length();
        int idx2 = url.indexOf(35, idx1 + 1);
        int idx3 = url.indexOf(35, idx2 + 1);
        int idx4 = url.indexOf(35, idx3 + 1);
        if (idx1 == -1 || idx2 == -1 || idx3 == -1 || idx4 == -1) {
            Log.e(TAG, "Could not decode URL command: " + url);
            return;
        }
        this.appView.pluginManager.exec(url.substring(idx1, idx2), url.substring(idx2 + 1, idx3), url.substring(idx3 + 1, idx4), url.substring(idx4 + 1));
    }

    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (this.appView.pluginManager == null || !this.appView.pluginManager.onOverrideUrlLoading(url)) {
            Intent intent;
            if (url.startsWith("tel:")) {
                try {
                    intent = new Intent("android.intent.action.DIAL");
                    intent.setData(Uri.parse(url));
                    this.cordova.getActivity().startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    LOG.m3e(TAG, "Error dialing " + url + ": " + e.toString());
                }
            } else if (url.startsWith("geo:")) {
                try {
                    intent = new Intent("android.intent.action.VIEW");
                    intent.setData(Uri.parse(url));
                    this.cordova.getActivity().startActivity(intent);
                } catch (ActivityNotFoundException e2) {
                    LOG.m3e(TAG, "Error showing map " + url + ": " + e2.toString());
                }
            } else if (url.startsWith("mailto:")) {
                try {
                    intent = new Intent("android.intent.action.VIEW");
                    intent.setData(Uri.parse(url));
                    this.cordova.getActivity().startActivity(intent);
                } catch (ActivityNotFoundException e22) {
                    LOG.m3e(TAG, "Error sending email " + url + ": " + e22.toString());
                }
            } else if (url.startsWith("sms:")) {
                try {
                    String address;
                    intent = new Intent("android.intent.action.VIEW");
                    int parmIndex = url.indexOf(63);
                    if (parmIndex == -1) {
                        address = url.substring(4);
                    } else {
                        address = url.substring(4, parmIndex);
                        String query = Uri.parse(url).getQuery();
                        if (query != null && query.startsWith("body=")) {
                            intent.putExtra("sms_body", query.substring(5));
                        }
                    }
                    intent.setData(Uri.parse("sms:" + address));
                    intent.putExtra("address", address);
                    intent.setType("vnd.android-dir/mms-sms");
                    this.cordova.getActivity().startActivity(intent);
                } catch (ActivityNotFoundException e222) {
                    LOG.m3e(TAG, "Error sending sms " + url + ":" + e222.toString());
                }
            } else if (url.startsWith("market:")) {
                try {
                    intent = new Intent("android.intent.action.VIEW");
                    intent.setData(Uri.parse(url));
                    this.cordova.getActivity().startActivity(intent);
                } catch (Throwable e3) {
                    LOG.m4e(TAG, "Error loading Google Play Store: " + url, e3);
                }
            } else if (url.startsWith("file://") || url.startsWith("data:") || Config.isUrlWhiteListed(url)) {
                return false;
            } else {
                try {
                    intent = new Intent("android.intent.action.VIEW");
                    intent.setData(Uri.parse(url));
                    this.cordova.getActivity().startActivity(intent);
                } catch (Throwable e32) {
                    LOG.m4e(TAG, "Error loading url " + url, e32);
                }
            }
        }
        return true;
    }

    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        AuthenticationToken token = getAuthenticationToken(host, realm);
        if (token != null) {
            handler.proceed(token.getUserName(), token.getPassword());
        } else {
            super.onReceivedHttpAuthRequest(view, handler, host, realm);
        }
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        this.appView.jsMessageQueue.reset();
        this.appView.postMessage("onPageStarted", url);
        if (this.appView.pluginManager != null) {
            this.appView.pluginManager.onReset();
        }
    }

    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        LOG.m0d(TAG, "onPageFinished(" + url + ")");
        if (this.doClearHistory) {
            view.clearHistory();
            this.doClearHistory = false;
        }
        CordovaWebView cordovaWebView = this.appView;
        cordovaWebView.loadUrlTimeout++;
        this.appView.postMessage("onPageFinished", url);
        if (this.appView.getVisibility() == 4) {
            new Thread(new C00451()).start();
        }
        if (url.equals("about:blank")) {
            this.appView.postMessage("exit", null);
        }
    }

    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        LOG.m2d(TAG, "CordovaWebViewClient.onReceivedError: Error code=%s Description=%s URL=%s", Integer.valueOf(errorCode), description, failingUrl);
        CordovaWebView cordovaWebView = this.appView;
        cordovaWebView.loadUrlTimeout++;
        JSONObject data = new JSONObject();
        try {
            data.put("errorCode", errorCode);
            data.put("description", description);
            data.put("url", failingUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.appView.postMessage("onReceivedError", data);
    }

    @TargetApi(8)
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        try {
            if ((this.cordova.getActivity().getPackageManager().getApplicationInfo(this.cordova.getActivity().getPackageName(), 128).flags & 2) != 0) {
                handler.proceed();
            } else {
                super.onReceivedSslError(view, handler, error);
            }
        } catch (NameNotFoundException e) {
            super.onReceivedSslError(view, handler, error);
        }
    }

    public void setAuthenticationToken(AuthenticationToken authenticationToken, String host, String realm) {
        if (host == null) {
            host = "";
        }
        if (realm == null) {
            realm = "";
        }
        this.authenticationTokens.put(host.concat(realm), authenticationToken);
    }

    public AuthenticationToken removeAuthenticationToken(String host, String realm) {
        return (AuthenticationToken) this.authenticationTokens.remove(host.concat(realm));
    }

    public AuthenticationToken getAuthenticationToken(String host, String realm) {
        AuthenticationToken token = (AuthenticationToken) this.authenticationTokens.get(host.concat(realm));
        if (token != null) {
            return token;
        }
        token = (AuthenticationToken) this.authenticationTokens.get(host);
        if (token == null) {
            token = (AuthenticationToken) this.authenticationTokens.get(realm);
        }
        if (token == null) {
            return (AuthenticationToken) this.authenticationTokens.get("");
        }
        return token;
    }

    public void clearAuthenticationTokens() {
        this.authenticationTokens.clear();
    }
}
