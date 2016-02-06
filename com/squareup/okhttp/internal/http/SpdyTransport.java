package com.squareup.okhttp.internal.http;

import com.squareup.okhttp.internal.spdy.SpdyConnection;
import com.squareup.okhttp.internal.spdy.SpdyStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.URL;

public final class SpdyTransport implements Transport {
    private final HttpEngine httpEngine;
    private final SpdyConnection spdyConnection;
    private SpdyStream stream;

    public SpdyTransport(HttpEngine httpEngine, SpdyConnection spdyConnection) {
        this.httpEngine = httpEngine;
        this.spdyConnection = spdyConnection;
    }

    public OutputStream createRequestBody() throws IOException {
        writeRequestHeaders();
        return this.stream.getOutputStream();
    }

    public void writeRequestHeaders() throws IOException {
        if (this.stream == null) {
            this.httpEngine.writingRequestHeaders();
            RawHeaders requestHeaders = this.httpEngine.requestHeaders.getHeaders();
            String version = this.httpEngine.connection.getHttpMinorVersion() == 1 ? "HTTP/1.1" : "HTTP/1.0";
            URL url = this.httpEngine.policy.getURL();
            requestHeaders.addSpdyRequestHeaders(this.httpEngine.method, HttpEngine.requestPath(url), version, HttpEngine.getOriginAddress(url), this.httpEngine.uri.getScheme());
            this.stream = this.spdyConnection.newStream(requestHeaders.toNameValueBlock(), this.httpEngine.hasRequestBody(), true);
            this.stream.setReadTimeout((long) this.httpEngine.policy.getReadTimeout());
        }
    }

    public void writeRequestBody(RetryableOutputStream requestBody) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void flushRequest() throws IOException {
        this.stream.getOutputStream().close();
    }

    public ResponseHeaders readResponseHeaders() throws IOException {
        RawHeaders rawHeaders = RawHeaders.fromNameValueBlock(this.stream.getResponseHeaders());
        rawHeaders.computeResponseStatusLineFromSpdyHeaders();
        this.httpEngine.receiveHeaders(rawHeaders);
        return new ResponseHeaders(this.httpEngine.uri, rawHeaders);
    }

    public InputStream getTransferStream(CacheRequest cacheRequest) throws IOException {
        return new UnknownLengthHttpInputStream(this.stream.getInputStream(), cacheRequest, this.httpEngine);
    }

    public boolean makeReusable(boolean streamCancelled, OutputStream requestBodyOut, InputStream responseBodyIn) {
        if (!streamCancelled) {
            return true;
        }
        if (this.stream == null) {
            return false;
        }
        this.stream.closeLater(5);
        return true;
    }
}
