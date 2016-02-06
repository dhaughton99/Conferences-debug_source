package com.squareup.okhttp.internal;

import com.squareup.okhttp.OkHttpClient;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import javax.net.ssl.SSLSocket;

public class Platform {
    private static final Platform PLATFORM;
    private Constructor<DeflaterOutputStream> deflaterConstructor;

    private static class JettyNpnProvider implements InvocationHandler {
        private final List<String> protocols;
        private String selected;
        private boolean unsupported;

        public JettyNpnProvider(List<String> protocols) {
            this.protocols = protocols;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            if (args == null) {
                args = Util.EMPTY_STRING_ARRAY;
            }
            if (methodName.equals("supports") && Boolean.TYPE == returnType) {
                return Boolean.valueOf(true);
            }
            if (methodName.equals("unsupported") && Void.TYPE == returnType) {
                this.unsupported = true;
                return null;
            } else if (methodName.equals("protocols") && args.length == 0) {
                return this.protocols;
            } else {
                if (methodName.equals("selectProtocol") && String.class == returnType && args.length == 1 && (args[0] == null || (args[0] instanceof List))) {
                    List<?> serverProtocols = args[0];
                    this.selected = (String) this.protocols.get(0);
                    return this.selected;
                } else if (!methodName.equals("protocolSelected") || args.length != 1) {
                    return method.invoke(this, args);
                } else {
                    this.selected = (String) args[0];
                    return null;
                }
            }
        }
    }

    private static class Java5 extends Platform {
        private final Method getMtu;

        private Java5(Method getMtu) {
            this.getMtu = getMtu;
        }

        public int getMtu(Socket socket) throws IOException {
            try {
                return ((Integer) this.getMtu.invoke(NetworkInterface.getByInetAddress(socket.getLocalAddress()), new Object[0])).intValue();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e2) {
                if (e2.getCause() instanceof IOException) {
                    throw ((IOException) e2.getCause());
                }
                throw new RuntimeException(e2.getCause());
            }
        }
    }

    private static class Android23 extends Java5 {
        protected final Class<?> openSslSocketClass;
        private final Method setHostname;
        private final Method setUseSessionTickets;

        private Android23(Method getMtu, Class<?> openSslSocketClass, Method setUseSessionTickets, Method setHostname) {
            super(null);
            this.openSslSocketClass = openSslSocketClass;
            this.setUseSessionTickets = setUseSessionTickets;
            this.setHostname = setHostname;
        }

        public void enableTlsExtensions(SSLSocket socket, String uriHost) {
            super.enableTlsExtensions(socket, uriHost);
            if (this.openSslSocketClass.isInstance(socket)) {
                try {
                    this.setUseSessionTickets.invoke(socket, new Object[]{Boolean.valueOf(true)});
                    this.setHostname.invoke(socket, new Object[]{uriHost});
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e2) {
                    throw new AssertionError(e2);
                }
            }
        }
    }

    private static class JdkWithJettyNpnPlatform extends Java5 {
        private final Class<?> clientProviderClass;
        private final Method getMethod;
        private final Method putMethod;
        private final Class<?> serverProviderClass;

        public JdkWithJettyNpnPlatform(Method getMtu, Method putMethod, Method getMethod, Class<?> clientProviderClass, Class<?> serverProviderClass) {
            super(null);
            this.putMethod = putMethod;
            this.getMethod = getMethod;
            this.clientProviderClass = clientProviderClass;
            this.serverProviderClass = serverProviderClass;
        }

        public void setNpnProtocols(SSLSocket socket, byte[] npnProtocols) {
            try {
                List<String> strings = new ArrayList();
                int i = 0;
                while (i < npnProtocols.length) {
                    int i2 = i + 1;
                    int length = npnProtocols[i];
                    strings.add(new String(npnProtocols, i2, length, "US-ASCII"));
                    i = i2 + length;
                }
                Object provider = Proxy.newProxyInstance(Platform.class.getClassLoader(), new Class[]{this.clientProviderClass, this.serverProviderClass}, new JettyNpnProvider(strings));
                this.putMethod.invoke(null, new Object[]{socket, provider});
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e2) {
                throw new AssertionError(e2);
            } catch (IllegalAccessException e3) {
                throw new AssertionError(e3);
            }
        }

        public byte[] getNpnSelectedProtocol(SSLSocket socket) {
            byte[] bArr = null;
            try {
                JettyNpnProvider provider = (JettyNpnProvider) Proxy.getInvocationHandler(this.getMethod.invoke(null, new Object[]{socket}));
                if (!provider.unsupported && provider.selected == null) {
                    Logger.getLogger(OkHttpClient.class.getName()).log(Level.INFO, "NPN callback dropped so SPDY is disabled. Is npn-boot on the boot class path?");
                } else if (!provider.unsupported) {
                    bArr = provider.selected.getBytes("US-ASCII");
                }
                return bArr;
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError();
            } catch (InvocationTargetException e2) {
                throw new AssertionError();
            } catch (IllegalAccessException e3) {
                throw new AssertionError();
            }
        }
    }

    private static class Android41 extends Android23 {
        private final Method getNpnSelectedProtocol;
        private final Method setNpnProtocols;

        private Android41(Method getMtu, Class<?> openSslSocketClass, Method setUseSessionTickets, Method setHostname, Method setNpnProtocols, Method getNpnSelectedProtocol) {
            super(openSslSocketClass, setUseSessionTickets, setHostname, null);
            this.setNpnProtocols = setNpnProtocols;
            this.getNpnSelectedProtocol = getNpnSelectedProtocol;
        }

        public void setNpnProtocols(SSLSocket socket, byte[] npnProtocols) {
            if (this.openSslSocketClass.isInstance(socket)) {
                try {
                    this.setNpnProtocols.invoke(socket, new Object[]{npnProtocols});
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                } catch (InvocationTargetException e2) {
                    throw new RuntimeException(e2);
                }
            }
        }

        public byte[] getNpnSelectedProtocol(SSLSocket socket) {
            if (!this.openSslSocketClass.isInstance(socket)) {
                return null;
            }
            try {
                return (byte[]) this.getNpnSelectedProtocol.invoke(socket, new Object[0]);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e2) {
                throw new AssertionError(e2);
            }
        }
    }

    static {
        PLATFORM = findPlatform();
    }

    public static Platform get() {
        return PLATFORM;
    }

    public void logW(String warning) {
        System.out.println(warning);
    }

    public void tagSocket(Socket socket) throws SocketException {
    }

    public void untagSocket(Socket socket) throws SocketException {
    }

    public URI toUriLenient(URL url) throws URISyntaxException {
        return url.toURI();
    }

    public void enableTlsExtensions(SSLSocket socket, String uriHost) {
    }

    public void supportTlsIntolerantServer(SSLSocket socket) {
        socket.setEnabledProtocols(new String[]{"SSLv3"});
    }

    public byte[] getNpnSelectedProtocol(SSLSocket socket) {
        return null;
    }

    public void setNpnProtocols(SSLSocket socket, byte[] npnProtocols) {
    }

    public OutputStream newDeflaterOutputStream(OutputStream out, Deflater deflater, boolean syncFlush) {
        try {
            Constructor<DeflaterOutputStream> constructor = this.deflaterConstructor;
            if (constructor == null) {
                constructor = DeflaterOutputStream.class.getConstructor(new Class[]{OutputStream.class, Deflater.class, Boolean.TYPE});
                this.deflaterConstructor = constructor;
            }
            return (OutputStream) constructor.newInstance(new Object[]{out, deflater, Boolean.valueOf(syncFlush)});
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("Cannot SPDY; no SYNC_FLUSH available");
        } catch (InvocationTargetException e2) {
            throw (e2.getCause() instanceof RuntimeException ? (RuntimeException) e2.getCause() : new RuntimeException(e2.getCause()));
        } catch (InstantiationException e3) {
            throw new RuntimeException(e3);
        } catch (IllegalAccessException e4) {
            throw new AssertionError();
        }
    }

    public int getMtu(Socket socket) throws IOException {
        return 1400;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static com.squareup.okhttp.internal.Platform findPlatform() {
        /*
        r1 = java.net.NetworkInterface.class;
        r8 = "getMTU";
        r9 = 0;
        r9 = new java.lang.Class[r9];	 Catch:{ NoSuchMethodException -> 0x004b }
        r2 = r1.getMethod(r8, r9);	 Catch:{ NoSuchMethodException -> 0x004b }
        r1 = "org.apache.harmony.xnet.provider.jsse.OpenSSLSocketImpl";
        r3 = java.lang.Class.forName(r1);	 Catch:{ ClassNotFoundException -> 0x00f1, NoSuchMethodException -> 0x005f }
        r1 = "setUseSessionTickets";
        r8 = 1;
        r8 = new java.lang.Class[r8];	 Catch:{ ClassNotFoundException -> 0x00f1, NoSuchMethodException -> 0x005f }
        r9 = 0;
        r19 = java.lang.Boolean.TYPE;	 Catch:{ ClassNotFoundException -> 0x00f1, NoSuchMethodException -> 0x005f }
        r8[r9] = r19;	 Catch:{ ClassNotFoundException -> 0x00f1, NoSuchMethodException -> 0x005f }
        r4 = r3.getMethod(r1, r8);	 Catch:{ ClassNotFoundException -> 0x00f1, NoSuchMethodException -> 0x005f }
        r1 = "setHostname";
        r8 = 1;
        r8 = new java.lang.Class[r8];	 Catch:{ ClassNotFoundException -> 0x00f1, NoSuchMethodException -> 0x005f }
        r9 = 0;
        r19 = java.lang.String.class;
        r8[r9] = r19;	 Catch:{ ClassNotFoundException -> 0x00f1, NoSuchMethodException -> 0x005f }
        r5 = r3.getMethod(r1, r8);	 Catch:{ ClassNotFoundException -> 0x00f1, NoSuchMethodException -> 0x005f }
        r1 = "setNpnProtocols";
        r8 = 1;
        r8 = new java.lang.Class[r8];	 Catch:{ NoSuchMethodException -> 0x0052, ClassNotFoundException -> 0x00f1 }
        r9 = 0;
        r19 = byte[].class;
        r8[r9] = r19;	 Catch:{ NoSuchMethodException -> 0x0052, ClassNotFoundException -> 0x00f1 }
        r6 = r3.getMethod(r1, r8);	 Catch:{ NoSuchMethodException -> 0x0052, ClassNotFoundException -> 0x00f1 }
        r1 = "getNpnSelectedProtocol";
        r8 = 0;
        r8 = new java.lang.Class[r8];	 Catch:{ NoSuchMethodException -> 0x0052, ClassNotFoundException -> 0x00f1 }
        r7 = r3.getMethod(r1, r8);	 Catch:{ NoSuchMethodException -> 0x0052, ClassNotFoundException -> 0x00f1 }
        r1 = new com.squareup.okhttp.internal.Platform$Android41;	 Catch:{ NoSuchMethodException -> 0x0052, ClassNotFoundException -> 0x00f1 }
        r8 = 0;
        r1.<init>(r3, r4, r5, r6, r7, r8);	 Catch:{ NoSuchMethodException -> 0x0052, ClassNotFoundException -> 0x00f1 }
    L_0x004a:
        return r1;
    L_0x004b:
        r14 = move-exception;
        r1 = new com.squareup.okhttp.internal.Platform;
        r1.<init>();
        goto L_0x004a;
    L_0x0052:
        r15 = move-exception;
        r8 = new com.squareup.okhttp.internal.Platform$Android23;	 Catch:{ ClassNotFoundException -> 0x00f1, NoSuchMethodException -> 0x005f }
        r13 = 0;
        r9 = r2;
        r10 = r3;
        r11 = r4;
        r12 = r5;
        r8.<init>(r10, r11, r12, r13);	 Catch:{ ClassNotFoundException -> 0x00f1, NoSuchMethodException -> 0x005f }
        r1 = r8;
        goto L_0x004a;
    L_0x005f:
        r1 = move-exception;
    L_0x0060:
        r17 = "org.eclipse.jetty.npn.NextProtoNego";
        r16 = java.lang.Class.forName(r17);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1 = new java.lang.StringBuilder;	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1.<init>();	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r0 = r17;
        r1 = r1.append(r0);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r8 = "$Provider";
        r1 = r1.append(r8);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1 = r1.toString();	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r18 = java.lang.Class.forName(r1);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1 = new java.lang.StringBuilder;	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1.<init>();	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r0 = r17;
        r1 = r1.append(r0);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r8 = "$ClientProvider";
        r1 = r1.append(r8);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1 = r1.toString();	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r12 = java.lang.Class.forName(r1);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1 = new java.lang.StringBuilder;	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1.<init>();	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r0 = r17;
        r1 = r1.append(r0);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r8 = "$ServerProvider";
        r1 = r1.append(r8);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1 = r1.toString();	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r13 = java.lang.Class.forName(r1);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1 = "put";
        r8 = 2;
        r8 = new java.lang.Class[r8];	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r9 = 0;
        r19 = javax.net.ssl.SSLSocket.class;
        r8[r9] = r19;	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r9 = 1;
        r8[r9] = r18;	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r0 = r16;
        r10 = r0.getMethod(r1, r8);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1 = "get";
        r8 = 1;
        r8 = new java.lang.Class[r8];	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r9 = 0;
        r19 = javax.net.ssl.SSLSocket.class;
        r8[r9] = r19;	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r0 = r16;
        r11 = r0.getMethod(r1, r8);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r8 = new com.squareup.okhttp.internal.Platform$JdkWithJettyNpnPlatform;	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r9 = r2;
        r8.<init>(r9, r10, r11, r12, r13);	 Catch:{ ClassNotFoundException -> 0x00ef, NoSuchMethodException -> 0x00dd }
        r1 = r8;
        goto L_0x004a;
    L_0x00dd:
        r1 = move-exception;
    L_0x00de:
        if (r2 == 0) goto L_0x00e8;
    L_0x00e0:
        r1 = new com.squareup.okhttp.internal.Platform$Java5;
        r8 = 0;
        r1.<init>(r8);
        goto L_0x004a;
    L_0x00e8:
        r1 = new com.squareup.okhttp.internal.Platform;
        r1.<init>();
        goto L_0x004a;
    L_0x00ef:
        r1 = move-exception;
        goto L_0x00de;
    L_0x00f1:
        r1 = move-exception;
        goto L_0x0060;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.squareup.okhttp.internal.Platform.findPlatform():com.squareup.okhttp.internal.Platform");
    }
}
