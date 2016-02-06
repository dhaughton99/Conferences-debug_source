package com.squareup.okhttp.internal.http;

import com.squareup.okhttp.internal.Base64;
import java.io.IOException;
import java.net.Authenticator;
import java.net.Authenticator.RequestorType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class HttpAuthenticator {

    private static final class Challenge {
        final String realm;
        final String scheme;

        Challenge(String scheme, String realm) {
            this.scheme = scheme;
            this.realm = realm;
        }

        public boolean equals(Object o) {
            return (o instanceof Challenge) && ((Challenge) o).scheme.equals(this.scheme) && ((Challenge) o).realm.equals(this.realm);
        }

        public int hashCode() {
            return this.scheme.hashCode() + (this.realm.hashCode() * 31);
        }
    }

    private HttpAuthenticator() {
    }

    public static boolean processAuthHeader(int responseCode, RawHeaders responseHeaders, RawHeaders successorRequestHeaders, Proxy proxy, URL url) throws IOException {
        if (responseCode == 407 || responseCode == 401) {
            String credentials = getCredentials(responseHeaders, responseCode == 407 ? "Proxy-Authenticate" : "WWW-Authenticate", proxy, url);
            if (credentials == null) {
                return false;
            }
            successorRequestHeaders.set(responseCode == 407 ? "Proxy-Authorization" : "Authorization", credentials);
            return true;
        }
        throw new IllegalArgumentException();
    }

    private static String getCredentials(RawHeaders responseHeaders, String challengeHeader, Proxy proxy, URL url) throws IOException {
        List<Challenge> challenges = parseChallenges(responseHeaders, challengeHeader);
        if (challenges.isEmpty()) {
            return null;
        }
        for (Challenge challenge : challenges) {
            PasswordAuthentication auth;
            if (responseHeaders.getResponseCode() == 407) {
                InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
                auth = Authenticator.requestPasswordAuthentication(proxyAddress.getHostName(), getConnectToInetAddress(proxy, url), proxyAddress.getPort(), url.getProtocol(), challenge.realm, challenge.scheme, url, RequestorType.PROXY);
                continue;
            } else {
                auth = Authenticator.requestPasswordAuthentication(url.getHost(), getConnectToInetAddress(proxy, url), url.getPort(), url.getProtocol(), challenge.realm, challenge.scheme, url, RequestorType.SERVER);
                continue;
            }
            if (auth != null) {
                return challenge.scheme + " " + Base64.encode((auth.getUserName() + ":" + new String(auth.getPassword())).getBytes("ISO-8859-1"));
            }
        }
        return null;
    }

    private static InetAddress getConnectToInetAddress(Proxy proxy, URL url) throws IOException {
        return (proxy == null || proxy.type() == Type.DIRECT) ? InetAddress.getByName(url.getHost()) : ((InetSocketAddress) proxy.address()).getAddress();
    }

    private static List<Challenge> parseChallenges(RawHeaders responseHeaders, String challengeHeader) {
        List<Challenge> result = new ArrayList();
        for (int h = 0; h < responseHeaders.length(); h++) {
            if (challengeHeader.equalsIgnoreCase(responseHeaders.getFieldName(h))) {
                String value = responseHeaders.getValue(h);
                int pos = 0;
                while (pos < value.length()) {
                    int tokenStart = pos;
                    pos = HeaderParser.skipUntil(value, pos, " ");
                    String scheme = value.substring(tokenStart, pos).trim();
                    pos = HeaderParser.skipWhitespace(value, pos);
                    if (!value.regionMatches(pos, "realm=\"", 0, "realm=\"".length())) {
                        break;
                    }
                    pos += "realm=\"".length();
                    int realmStart = pos;
                    pos = HeaderParser.skipUntil(value, pos, "\"");
                    String realm = value.substring(realmStart, pos);
                    pos = HeaderParser.skipWhitespace(value, HeaderParser.skipUntil(value, pos + 1, ",") + 1);
                    result.add(new Challenge(scheme, realm));
                }
            }
        }
        return result;
    }
}
