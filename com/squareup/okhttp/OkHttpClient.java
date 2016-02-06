package com.squareup.okhttp;

import com.squareup.okhttp.internal.http.HttpURLConnectionImpl;
import com.squareup.okhttp.internal.http.HttpsURLConnectionImpl;
import com.squareup.okhttp.internal.http.OkResponseCache;
import com.squareup.okhttp.internal.http.OkResponseCacheAdapter;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ResponseCache;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public final class OkHttpClient {
    private ConnectionPool connectionPool;
    private CookieHandler cookieHandler;
    private Set<Route> failedRoutes;
    private boolean followProtocolRedirects;
    private HostnameVerifier hostnameVerifier;
    private Proxy proxy;
    private ProxySelector proxySelector;
    private ResponseCache responseCache;
    private SSLSocketFactory sslSocketFactory;

    public OkHttpClient() {
        this.failedRoutes = Collections.synchronizedSet(new LinkedHashSet());
        this.followProtocolRedirects = true;
    }

    public OkHttpClient setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public OkHttpClient setProxySelector(ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
        return this;
    }

    public ProxySelector getProxySelector() {
        return this.proxySelector;
    }

    public OkHttpClient setCookieHandler(CookieHandler cookieHandler) {
        this.cookieHandler = cookieHandler;
        return this;
    }

    public CookieHandler getCookieHandler() {
        return this.cookieHandler;
    }

    public OkHttpClient setResponseCache(ResponseCache responseCache) {
        this.responseCache = responseCache;
        return this;
    }

    public ResponseCache getResponseCache() {
        return this.responseCache;
    }

    private OkResponseCache okResponseCache() {
        if (this.responseCache instanceof HttpResponseCache) {
            return ((HttpResponseCache) this.responseCache).okResponseCache;
        }
        if (this.responseCache != null) {
            return new OkResponseCacheAdapter(this.responseCache);
        }
        return null;
    }

    public OkHttpClient setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        return this;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return this.sslSocketFactory;
    }

    public OkHttpClient setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    public HostnameVerifier getHostnameVerifier() {
        return this.hostnameVerifier;
    }

    public OkHttpClient setConnectionPool(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        return this;
    }

    public ConnectionPool getConnectionPool() {
        return this.connectionPool;
    }

    public OkHttpClient setFollowProtocolRedirects(boolean followProtocolRedirects) {
        this.followProtocolRedirects = followProtocolRedirects;
        return this;
    }

    public boolean getFollowProtocolRedirects() {
        return this.followProtocolRedirects;
    }

    public HttpURLConnection open(URL url) {
        String protocol = url.getProtocol();
        OkHttpClient copy = copyWithDefaults();
        if (protocol.equals("http")) {
            return new HttpURLConnectionImpl(url, copy, copy.okResponseCache(), copy.failedRoutes);
        }
        if (protocol.equals("https")) {
            return new HttpsURLConnectionImpl(url, copy, copy.okResponseCache(), copy.failedRoutes);
        }
        throw new IllegalArgumentException("Unexpected protocol: " + protocol);
    }

    private OkHttpClient copyWithDefaults() {
        OkHttpClient result = new OkHttpClient();
        result.proxy = this.proxy;
        result.failedRoutes = this.failedRoutes;
        result.proxySelector = this.proxySelector != null ? this.proxySelector : ProxySelector.getDefault();
        result.cookieHandler = this.cookieHandler != null ? this.cookieHandler : CookieHandler.getDefault();
        result.responseCache = this.responseCache != null ? this.responseCache : ResponseCache.getDefault();
        result.sslSocketFactory = this.sslSocketFactory != null ? this.sslSocketFactory : HttpsURLConnection.getDefaultSSLSocketFactory();
        result.hostnameVerifier = this.hostnameVerifier != null ? this.hostnameVerifier : HttpsURLConnection.getDefaultHostnameVerifier();
        result.connectionPool = this.connectionPool != null ? this.connectionPool : ConnectionPool.getDefault();
        result.followProtocolRedirects = this.followProtocolRedirects;
        return result;
    }
}
