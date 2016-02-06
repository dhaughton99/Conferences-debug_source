package com.squareup.okhttp;

import com.squareup.okhttp.internal.Util;
import java.net.Proxy;
import java.net.UnknownHostException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

public final class Address {
    final HostnameVerifier hostnameVerifier;
    final Proxy proxy;
    final SSLSocketFactory sslSocketFactory;
    final String uriHost;
    final int uriPort;

    public Address(String uriHost, int uriPort, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier, Proxy proxy) throws UnknownHostException {
        if (uriHost == null) {
            throw new NullPointerException("uriHost == null");
        } else if (uriPort <= 0) {
            throw new IllegalArgumentException("uriPort <= 0: " + uriPort);
        } else {
            this.proxy = proxy;
            this.uriHost = uriHost;
            this.uriPort = uriPort;
            this.sslSocketFactory = sslSocketFactory;
            this.hostnameVerifier = hostnameVerifier;
        }
    }

    public String getUriHost() {
        return this.uriHost;
    }

    public int getUriPort() {
        return this.uriPort;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return this.sslSocketFactory;
    }

    public HostnameVerifier getHostnameVerifier() {
        return this.hostnameVerifier;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Address)) {
            return false;
        }
        Address that = (Address) other;
        if (Util.equal(this.proxy, that.proxy) && this.uriHost.equals(that.uriHost) && this.uriPort == that.uriPort && Util.equal(this.sslSocketFactory, that.sslSocketFactory) && Util.equal(this.hostnameVerifier, that.hostnameVerifier)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int hashCode;
        int i = 0;
        int hashCode2 = (((this.uriHost.hashCode() + 527) * 31) + this.uriPort) * 31;
        if (this.sslSocketFactory != null) {
            hashCode = this.sslSocketFactory.hashCode();
        } else {
            hashCode = 0;
        }
        hashCode2 = (hashCode2 + hashCode) * 31;
        if (this.hostnameVerifier != null) {
            hashCode = this.hostnameVerifier.hashCode();
        } else {
            hashCode = 0;
        }
        hashCode = (hashCode2 + hashCode) * 31;
        if (this.proxy != null) {
            i = this.proxy.hashCode();
        }
        return hashCode + i;
    }
}
