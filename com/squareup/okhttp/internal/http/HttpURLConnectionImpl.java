package com.squareup.okhttp.internal.http;

import com.squareup.okhttp.Connection;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Route;
import com.squareup.okhttp.internal.AbstractOutputStream;
import com.squareup.okhttp.internal.FaultRecoveringOutputStream;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.internal.http.HttpsURLConnectionImpl.HttpsEngine;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketPermission;
import java.net.URL;
import java.security.Permission;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

public class HttpURLConnectionImpl extends HttpURLConnection {
    static final int HTTP_TEMP_REDIRECT = 307;
    private static final int MAX_REDIRECTS = 20;
    private static final int MAX_REPLAY_BUFFER_LENGTH = 8192;
    final ConnectionPool connectionPool;
    final CookieHandler cookieHandler;
    final Set<Route> failedRoutes;
    private FaultRecoveringOutputStream faultRecoveringRequestBody;
    private final boolean followProtocolRedirects;
    HostnameVerifier hostnameVerifier;
    protected HttpEngine httpEngine;
    protected IOException httpEngineFailure;
    final ProxySelector proxySelector;
    private final RawHeaders rawRequestHeaders;
    private int redirectionCount;
    final Proxy requestedProxy;
    final OkResponseCache responseCache;
    SSLSocketFactory sslSocketFactory;

    enum Retry {
        NONE,
        SAME_CONNECTION,
        DIFFERENT_CONNECTION
    }

    /* renamed from: com.squareup.okhttp.internal.http.HttpURLConnectionImpl.1 */
    class C01111 extends FaultRecoveringOutputStream {
        C01111(int x0, OutputStream x1) {
            super(x0, x1);
        }

        protected OutputStream replacementStream(IOException e) throws IOException {
            if ((HttpURLConnectionImpl.this.httpEngine.getRequestBody() instanceof AbstractOutputStream) && ((AbstractOutputStream) HttpURLConnectionImpl.this.httpEngine.getRequestBody()).isClosed()) {
                return null;
            }
            return HttpURLConnectionImpl.this.handleFailure(e) ? HttpURLConnectionImpl.this.httpEngine.getRequestBody() : null;
        }
    }

    public HttpURLConnectionImpl(URL url, OkHttpClient client, OkResponseCache responseCache, Set<Route> failedRoutes) {
        super(url);
        this.rawRequestHeaders = new RawHeaders();
        this.followProtocolRedirects = client.getFollowProtocolRedirects();
        this.failedRoutes = failedRoutes;
        this.requestedProxy = client.getProxy();
        this.proxySelector = client.getProxySelector();
        this.cookieHandler = client.getCookieHandler();
        this.connectionPool = client.getConnectionPool();
        this.sslSocketFactory = client.getSslSocketFactory();
        this.hostnameVerifier = client.getHostnameVerifier();
        this.responseCache = responseCache;
    }

    Set<Route> getFailedRoutes() {
        return this.failedRoutes;
    }

    public final void connect() throws IOException {
        initHttpEngine();
        do {
        } while (!execute(false));
    }

    public final void disconnect() {
        if (this.httpEngine != null) {
            if (this.httpEngine.hasResponse()) {
                Util.closeQuietly(this.httpEngine.getResponseBody());
            }
            this.httpEngine.release(true);
        }
    }

    public final InputStream getErrorStream() {
        InputStream inputStream = null;
        try {
            HttpEngine response = getResponse();
            if (response.hasResponseBody() && response.getResponseCode() >= 400) {
                inputStream = response.getResponseBody();
            }
        } catch (IOException e) {
        }
        return inputStream;
    }

    public final String getHeaderField(int position) {
        try {
            return getResponse().getResponseHeaders().getHeaders().getValue(position);
        } catch (IOException e) {
            return null;
        }
    }

    public final String getHeaderField(String fieldName) {
        try {
            RawHeaders rawHeaders = getResponse().getResponseHeaders().getHeaders();
            return fieldName == null ? rawHeaders.getStatusLine() : rawHeaders.get(fieldName);
        } catch (IOException e) {
            return null;
        }
    }

    public final String getHeaderFieldKey(int position) {
        try {
            return getResponse().getResponseHeaders().getHeaders().getFieldName(position);
        } catch (IOException e) {
            return null;
        }
    }

    public final Map<String, List<String>> getHeaderFields() {
        try {
            return getResponse().getResponseHeaders().getHeaders().toMultimap(true);
        } catch (IOException e) {
            return null;
        }
    }

    public final Map<String, List<String>> getRequestProperties() {
        if (!this.connected) {
            return this.rawRequestHeaders.toMultimap(false);
        }
        throw new IllegalStateException("Cannot access request header fields after connection is set");
    }

    public final InputStream getInputStream() throws IOException {
        if (this.doInput) {
            HttpEngine response = getResponse();
            if (getResponseCode() >= 400) {
                throw new FileNotFoundException(this.url.toString());
            }
            InputStream result = response.getResponseBody();
            if (result != null) {
                return result;
            }
            throw new ProtocolException("No response body exists; responseCode=" + getResponseCode());
        }
        throw new ProtocolException("This protocol does not support input");
    }

    public final OutputStream getOutputStream() throws IOException {
        connect();
        OutputStream out = this.httpEngine.getRequestBody();
        if (out == null) {
            throw new ProtocolException("method does not support a request body: " + this.method);
        } else if (this.httpEngine.hasResponse()) {
            throw new ProtocolException("cannot write request body after response has been read");
        } else {
            if (this.faultRecoveringRequestBody == null) {
                this.faultRecoveringRequestBody = new C01111(MAX_REPLAY_BUFFER_LENGTH, out);
            }
            return this.faultRecoveringRequestBody;
        }
    }

    public final Permission getPermission() throws IOException {
        String hostName = getURL().getHost();
        int hostPort = Util.getEffectivePort(getURL());
        if (usingProxy()) {
            InetSocketAddress proxyAddress = (InetSocketAddress) this.requestedProxy.address();
            hostName = proxyAddress.getHostName();
            hostPort = proxyAddress.getPort();
        }
        return new SocketPermission(hostName + ":" + hostPort, "connect, resolve");
    }

    public final String getRequestProperty(String field) {
        if (field == null) {
            return null;
        }
        return this.rawRequestHeaders.get(field);
    }

    private void initHttpEngine() throws IOException {
        if (this.httpEngineFailure != null) {
            throw this.httpEngineFailure;
        } else if (this.httpEngine == null) {
            this.connected = true;
            try {
                if (this.doOutput) {
                    if (this.method.equals("GET")) {
                        this.method = "POST";
                    } else if (!(this.method.equals("POST") || this.method.equals("PUT"))) {
                        throw new ProtocolException(this.method + " does not support writing");
                    }
                }
                this.httpEngine = newHttpEngine(this.method, this.rawRequestHeaders, null, null);
            } catch (IOException e) {
                this.httpEngineFailure = e;
                throw e;
            }
        }
    }

    protected HttpURLConnection getHttpConnectionToCache() {
        return this;
    }

    private HttpEngine newHttpEngine(String method, RawHeaders requestHeaders, Connection connection, RetryableOutputStream requestBody) throws IOException {
        if (this.url.getProtocol().equals("http")) {
            return new HttpEngine(this, method, requestHeaders, connection, requestBody);
        }
        if (this.url.getProtocol().equals("https")) {
            return new HttpsEngine(this, method, requestHeaders, connection, requestBody);
        }
        throw new AssertionError();
    }

    private HttpEngine getResponse() throws IOException {
        initHttpEngine();
        if (this.httpEngine.hasResponse()) {
            return this.httpEngine;
        }
        while (true) {
            if (execute(true)) {
                Retry retry = processResponseHeaders();
                if (retry != Retry.NONE) {
                    String retryMethod = this.method;
                    OutputStream requestBody = this.httpEngine.getRequestBody();
                    int responseCode = getResponseCode();
                    if (responseCode == 300 || responseCode == 301 || responseCode == 302 || responseCode == 303) {
                        retryMethod = "GET";
                        requestBody = null;
                    }
                    if (requestBody != null && !(requestBody instanceof RetryableOutputStream)) {
                        break;
                    }
                    if (retry == Retry.DIFFERENT_CONNECTION) {
                        this.httpEngine.automaticallyReleaseConnectionToPool();
                    }
                    this.httpEngine.release(false);
                    this.httpEngine = newHttpEngine(retryMethod, this.rawRequestHeaders, this.httpEngine.getConnection(), (RetryableOutputStream) requestBody);
                } else {
                    this.httpEngine.automaticallyReleaseConnectionToPool();
                    return this.httpEngine;
                }
            }
        }
        throw new HttpRetryException("Cannot retry streamed HTTP body", this.httpEngine.getResponseCode());
    }

    private boolean execute(boolean readResponse) throws IOException {
        try {
            this.httpEngine.sendRequest();
            if (readResponse) {
                this.httpEngine.readResponse();
            }
            return true;
        } catch (IOException e) {
            if (handleFailure(e)) {
                return false;
            }
            throw e;
        }
    }

    private boolean handleFailure(IOException e) throws IOException {
        RouteSelector routeSelector = this.httpEngine.routeSelector;
        if (!(routeSelector == null || this.httpEngine.connection == null)) {
            routeSelector.connectFailed(this.httpEngine.connection, e);
        }
        OutputStream requestBody = this.httpEngine.getRequestBody();
        boolean canRetryRequestBody;
        if (requestBody == null || (requestBody instanceof RetryableOutputStream) || (this.faultRecoveringRequestBody != null && this.faultRecoveringRequestBody.isRecoverable())) {
            canRetryRequestBody = true;
        } else {
            canRetryRequestBody = false;
        }
        if (!(routeSelector == null && this.httpEngine.connection == null) && ((routeSelector == null || routeSelector.hasNext()) && isRecoverable(e) && canRetryRequestBody)) {
            RetryableOutputStream retryableOutputStream;
            this.httpEngine.release(true);
            if (requestBody instanceof RetryableOutputStream) {
                retryableOutputStream = (RetryableOutputStream) requestBody;
            } else {
                retryableOutputStream = null;
            }
            this.httpEngine = newHttpEngine(this.method, this.rawRequestHeaders, null, retryableOutputStream);
            this.httpEngine.routeSelector = routeSelector;
            if (this.faultRecoveringRequestBody != null && this.faultRecoveringRequestBody.isRecoverable()) {
                this.httpEngine.sendRequest();
                this.faultRecoveringRequestBody.replaceStream(this.httpEngine.getRequestBody());
            }
            return true;
        }
        this.httpEngineFailure = e;
        return false;
    }

    private boolean isRecoverable(IOException e) {
        boolean sslFailure;
        if ((e instanceof SSLHandshakeException) && (e.getCause() instanceof CertificateException)) {
            sslFailure = true;
        } else {
            sslFailure = false;
        }
        return (sslFailure || (e instanceof ProtocolException)) ? false : true;
    }

    public HttpEngine getHttpEngine() {
        return this.httpEngine;
    }

    private Retry processResponseHeaders() throws IOException {
        Proxy selectedProxy = this.httpEngine.connection != null ? this.httpEngine.connection.getRoute().getProxy() : this.requestedProxy;
        int responseCode = getResponseCode();
        switch (responseCode) {
            case 300:
            case 301:
            case 302:
            case 303:
            case HTTP_TEMP_REDIRECT /*307*/:
                if (!getInstanceFollowRedirects()) {
                    return Retry.NONE;
                }
                int i = this.redirectionCount + 1;
                this.redirectionCount = i;
                if (i > MAX_REDIRECTS) {
                    throw new ProtocolException("Too many redirects: " + this.redirectionCount);
                } else if (responseCode == HTTP_TEMP_REDIRECT && !this.method.equals("GET") && !this.method.equals("HEAD")) {
                    return Retry.NONE;
                } else {
                    String location = getHeaderField("Location");
                    if (location == null) {
                        return Retry.NONE;
                    }
                    URL previousUrl = this.url;
                    this.url = new URL(previousUrl, location);
                    if (!this.url.getProtocol().equals("https") && !this.url.getProtocol().equals("http")) {
                        return Retry.NONE;
                    }
                    boolean sameProtocol = previousUrl.getProtocol().equals(this.url.getProtocol());
                    if (!sameProtocol && !this.followProtocolRedirects) {
                        return Retry.NONE;
                    }
                    boolean sameHost = previousUrl.getHost().equals(this.url.getHost());
                    boolean samePort = Util.getEffectivePort(previousUrl) == Util.getEffectivePort(this.url);
                    if (sameHost && samePort && sameProtocol) {
                        return Retry.SAME_CONNECTION;
                    }
                    return Retry.DIFFERENT_CONNECTION;
                }
            case 401:
                break;
            case 407:
                if (selectedProxy.type() != Type.HTTP) {
                    throw new ProtocolException("Received HTTP_PROXY_AUTH (407) code while not using proxy");
                }
                break;
            default:
                return Retry.NONE;
        }
        return HttpAuthenticator.processAuthHeader(getResponseCode(), this.httpEngine.getResponseHeaders().getHeaders(), this.rawRequestHeaders, selectedProxy, this.url) ? Retry.SAME_CONNECTION : Retry.NONE;
    }

    final int getFixedContentLength() {
        return this.fixedContentLength;
    }

    final int getChunkLength() {
        return this.chunkLength;
    }

    public final boolean usingProxy() {
        return (this.requestedProxy == null || this.requestedProxy.type() == Type.DIRECT) ? false : true;
    }

    public String getResponseMessage() throws IOException {
        return getResponse().getResponseHeaders().getHeaders().getResponseMessage();
    }

    public final int getResponseCode() throws IOException {
        return getResponse().getResponseCode();
    }

    public final void setRequestProperty(String field, String newValue) {
        if (this.connected) {
            throw new IllegalStateException("Cannot set request property after connection is made");
        } else if (field == null) {
            throw new NullPointerException("field == null");
        } else {
            this.rawRequestHeaders.set(field, newValue);
        }
    }

    public final void addRequestProperty(String field, String value) {
        if (this.connected) {
            throw new IllegalStateException("Cannot add request property after connection is made");
        } else if (field == null) {
            throw new NullPointerException("field == null");
        } else {
            this.rawRequestHeaders.add(field, value);
        }
    }
}
