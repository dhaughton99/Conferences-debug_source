package com.squareup.okhttp.internal.spdy;

import com.squareup.okhttp.internal.NamedRunnable;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.internal.spdy.SpdyReader.Handler;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class SpdyConnection implements Closeable {
    static final /* synthetic */ boolean $assertionsDisabled;
    static final int FLAG_FIN = 1;
    static final int FLAG_UNIDIRECTIONAL = 2;
    static final int GOAWAY_INTERNAL_ERROR = 2;
    static final int GOAWAY_OK = 0;
    static final int GOAWAY_PROTOCOL_ERROR = 1;
    static final int TYPE_CREDENTIAL = 16;
    static final int TYPE_DATA = 0;
    static final int TYPE_GOAWAY = 7;
    static final int TYPE_HEADERS = 8;
    static final int TYPE_NOOP = 5;
    static final int TYPE_PING = 6;
    static final int TYPE_RST_STREAM = 3;
    static final int TYPE_SETTINGS = 4;
    static final int TYPE_SYN_REPLY = 2;
    static final int TYPE_SYN_STREAM = 1;
    static final int TYPE_WINDOW_UPDATE = 9;
    static final int VERSION = 3;
    private static final ExecutorService executor;
    final boolean client;
    private final IncomingStreamHandler handler;
    private final String hostName;
    private long idleStartTimeNs;
    private int lastGoodStreamId;
    private int nextPingId;
    private int nextStreamId;
    private Map<Integer, Ping> pings;
    Settings settings;
    private boolean shutdown;
    private final SpdyReader spdyReader;
    private final SpdyWriter spdyWriter;
    private final Map<Integer, SpdyStream> streams;

    public static class Builder {
        public boolean client;
        private IncomingStreamHandler handler;
        private String hostName;
        private InputStream in;
        private OutputStream out;

        public Builder(boolean client, Socket socket) throws IOException {
            this("", client, socket.getInputStream(), socket.getOutputStream());
        }

        public Builder(boolean client, InputStream in, OutputStream out) {
            this("", client, in, out);
        }

        public Builder(String hostName, boolean client, Socket socket) throws IOException {
            this(hostName, client, socket.getInputStream(), socket.getOutputStream());
        }

        public Builder(String hostName, boolean client, InputStream in, OutputStream out) {
            this.handler = IncomingStreamHandler.REFUSE_INCOMING_STREAMS;
            this.hostName = hostName;
            this.client = client;
            this.in = in;
            this.out = out;
        }

        public Builder handler(IncomingStreamHandler handler) {
            this.handler = handler;
            return this;
        }

        public SpdyConnection build() {
            return new SpdyConnection();
        }
    }

    /* renamed from: com.squareup.okhttp.internal.spdy.SpdyConnection.1 */
    class C00971 extends NamedRunnable {
        final /* synthetic */ int val$statusCode;
        final /* synthetic */ int val$streamId;

        C00971(String x0, int i, int i2) {
            this.val$streamId = i;
            this.val$statusCode = i2;
            super(x0);
        }

        public void execute() {
            try {
                SpdyConnection.this.writeSynReset(this.val$streamId, this.val$statusCode);
            } catch (IOException e) {
            }
        }
    }

    /* renamed from: com.squareup.okhttp.internal.spdy.SpdyConnection.2 */
    class C00982 extends NamedRunnable {
        final /* synthetic */ int val$deltaWindowSize;
        final /* synthetic */ int val$streamId;

        C00982(String x0, int i, int i2) {
            this.val$streamId = i;
            this.val$deltaWindowSize = i2;
            super(x0);
        }

        public void execute() {
            try {
                SpdyConnection.this.writeWindowUpdate(this.val$streamId, this.val$deltaWindowSize);
            } catch (IOException e) {
            }
        }
    }

    /* renamed from: com.squareup.okhttp.internal.spdy.SpdyConnection.3 */
    class C00993 extends NamedRunnable {
        final /* synthetic */ Ping val$ping;
        final /* synthetic */ int val$streamId;

        C00993(String x0, int i, Ping ping) {
            this.val$streamId = i;
            this.val$ping = ping;
            super(x0);
        }

        public void execute() {
            try {
                SpdyConnection.this.writePing(this.val$streamId, this.val$ping);
            } catch (IOException e) {
            }
        }
    }

    private class Reader implements Runnable, Handler {

        /* renamed from: com.squareup.okhttp.internal.spdy.SpdyConnection.Reader.1 */
        class C01001 extends NamedRunnable {
            final /* synthetic */ SpdyStream val$synStream;

            C01001(String x0, SpdyStream spdyStream) {
                this.val$synStream = spdyStream;
                super(x0);
            }

            public void execute() {
                try {
                    SpdyConnection.this.handler.receive(this.val$synStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private Reader() {
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
            r5 = this;
            r2 = 2;
            r1 = 6;
        L_0x0002:
            r3 = com.squareup.okhttp.internal.spdy.SpdyConnection.this;	 Catch:{ IOException -> 0x0016, all -> 0x0021 }
            r3 = r3.spdyReader;	 Catch:{ IOException -> 0x0016, all -> 0x0021 }
            r3 = r3.nextFrame(r5);	 Catch:{ IOException -> 0x0016, all -> 0x0021 }
            if (r3 != 0) goto L_0x0002;
        L_0x000e:
            r2 = 0;
            r1 = 5;
            r3 = com.squareup.okhttp.internal.spdy.SpdyConnection.this;	 Catch:{ IOException -> 0x002a }
            r3.close(r2, r1);	 Catch:{ IOException -> 0x002a }
        L_0x0015:
            return;
        L_0x0016:
            r0 = move-exception;
            r2 = 1;
            r1 = 1;
            r3 = com.squareup.okhttp.internal.spdy.SpdyConnection.this;	 Catch:{ IOException -> 0x001f }
            r3.close(r2, r1);	 Catch:{ IOException -> 0x001f }
            goto L_0x0015;
        L_0x001f:
            r3 = move-exception;
            goto L_0x0015;
        L_0x0021:
            r3 = move-exception;
            r4 = com.squareup.okhttp.internal.spdy.SpdyConnection.this;	 Catch:{ IOException -> 0x0028 }
            r4.close(r2, r1);	 Catch:{ IOException -> 0x0028 }
        L_0x0027:
            throw r3;
        L_0x0028:
            r4 = move-exception;
            goto L_0x0027;
        L_0x002a:
            r3 = move-exception;
            goto L_0x0015;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.squareup.okhttp.internal.spdy.SpdyConnection.Reader.run():void");
        }

        public void data(int flags, int streamId, InputStream in, int length) throws IOException {
            SpdyStream dataStream = SpdyConnection.this.getStream(streamId);
            if (dataStream == null) {
                SpdyConnection.this.writeSynResetLater(streamId, SpdyConnection.TYPE_SYN_REPLY);
                Util.skipByReading(in, (long) length);
                return;
            }
            dataStream.receiveData(in, length);
            if ((flags & SpdyConnection.TYPE_SYN_STREAM) != 0) {
                dataStream.receiveFin();
            }
        }

        public void synStream(int flags, int streamId, int associatedStreamId, int priority, int slot, List<String> nameValueBlock) {
            synchronized (SpdyConnection.this) {
                SpdyStream synStream = new SpdyStream(streamId, SpdyConnection.this, flags, priority, slot, nameValueBlock, SpdyConnection.this.settings);
                if (SpdyConnection.this.shutdown) {
                    return;
                }
                SpdyConnection.this.lastGoodStreamId = streamId;
                SpdyStream previous = (SpdyStream) SpdyConnection.this.streams.put(Integer.valueOf(streamId), synStream);
                if (previous != null) {
                    previous.closeLater(SpdyConnection.TYPE_SYN_STREAM);
                    SpdyConnection.this.removeStream(streamId);
                    return;
                }
                ExecutorService access$1500 = SpdyConnection.executor;
                Object[] objArr = new Object[SpdyConnection.TYPE_SYN_REPLY];
                objArr[SpdyConnection.TYPE_DATA] = SpdyConnection.this.hostName;
                objArr[SpdyConnection.TYPE_SYN_STREAM] = Integer.valueOf(streamId);
                access$1500.submit(new C01001(String.format("Callback %s stream %d", objArr), synStream));
            }
        }

        public void synReply(int flags, int streamId, List<String> nameValueBlock) throws IOException {
            SpdyStream replyStream = SpdyConnection.this.getStream(streamId);
            if (replyStream == null) {
                SpdyConnection.this.writeSynResetLater(streamId, SpdyConnection.TYPE_SYN_REPLY);
                return;
            }
            replyStream.receiveReply(nameValueBlock);
            if ((flags & SpdyConnection.TYPE_SYN_STREAM) != 0) {
                replyStream.receiveFin();
            }
        }

        public void headers(int flags, int streamId, List<String> nameValueBlock) throws IOException {
            SpdyStream replyStream = SpdyConnection.this.getStream(streamId);
            if (replyStream != null) {
                replyStream.receiveHeaders(nameValueBlock);
            }
        }

        public void rstStream(int flags, int streamId, int statusCode) {
            SpdyStream rstStream = SpdyConnection.this.removeStream(streamId);
            if (rstStream != null) {
                rstStream.receiveRstStream(statusCode);
            }
        }

        public void settings(int flags, Settings newSettings) {
            SpdyStream[] streamsToNotify = null;
            synchronized (SpdyConnection.this) {
                if (SpdyConnection.this.settings == null || (flags & SpdyConnection.TYPE_SYN_STREAM) != 0) {
                    SpdyConnection.this.settings = newSettings;
                } else {
                    SpdyConnection.this.settings.merge(newSettings);
                }
                if (!SpdyConnection.this.streams.isEmpty()) {
                    streamsToNotify = (SpdyStream[]) SpdyConnection.this.streams.values().toArray(new SpdyStream[SpdyConnection.this.streams.size()]);
                }
            }
            if (streamsToNotify != null) {
                SpdyStream[] arr$ = streamsToNotify;
                int len$ = arr$.length;
                for (int i$ = SpdyConnection.TYPE_DATA; i$ < len$; i$ += SpdyConnection.TYPE_SYN_STREAM) {
                    SpdyStream stream = arr$[i$];
                    synchronized (stream) {
                        synchronized (this) {
                            stream.receiveSettings(SpdyConnection.this.settings);
                        }
                    }
                }
            }
        }

        public void noop() {
        }

        public void ping(int flags, int streamId) {
            boolean z = true;
            boolean z2 = SpdyConnection.this.client;
            if (streamId % SpdyConnection.TYPE_SYN_REPLY != SpdyConnection.TYPE_SYN_STREAM) {
                z = SpdyConnection.$assertionsDisabled;
            }
            if (z2 != z) {
                SpdyConnection.this.writePingLater(streamId, null);
                return;
            }
            Ping ping = SpdyConnection.this.removePing(streamId);
            if (ping != null) {
                ping.receive();
            }
        }

        public void goAway(int flags, int lastGoodStreamId, int statusCode) {
            synchronized (SpdyConnection.this) {
                SpdyConnection.this.shutdown = true;
                Iterator<Entry<Integer, SpdyStream>> i = SpdyConnection.this.streams.entrySet().iterator();
                while (i.hasNext()) {
                    Entry<Integer, SpdyStream> entry = (Entry) i.next();
                    if (((Integer) entry.getKey()).intValue() > lastGoodStreamId && ((SpdyStream) entry.getValue()).isLocallyInitiated()) {
                        ((SpdyStream) entry.getValue()).receiveRstStream(SpdyConnection.VERSION);
                        i.remove();
                    }
                }
            }
        }

        public void windowUpdate(int flags, int streamId, int deltaWindowSize) {
            SpdyStream stream = SpdyConnection.this.getStream(streamId);
            if (stream != null) {
                stream.receiveWindowUpdate(deltaWindowSize);
            }
        }
    }

    static {
        boolean z;
        if (SpdyConnection.class.desiredAssertionStatus()) {
            z = $assertionsDisabled;
        } else {
            z = true;
        }
        $assertionsDisabled = z;
        executor = new ThreadPoolExecutor(TYPE_DATA, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue(), Executors.defaultThreadFactory());
    }

    private SpdyConnection(Builder builder) {
        int i = TYPE_SYN_STREAM;
        this.streams = new HashMap();
        this.idleStartTimeNs = System.nanoTime();
        this.client = builder.client;
        this.handler = builder.handler;
        this.spdyReader = new SpdyReader(builder.in);
        this.spdyWriter = new SpdyWriter(builder.out);
        this.nextStreamId = builder.client ? TYPE_SYN_STREAM : TYPE_SYN_REPLY;
        if (!builder.client) {
            i = TYPE_SYN_REPLY;
        }
        this.nextPingId = i;
        this.hostName = builder.hostName;
        new Thread(new Reader(), "Spdy Reader " + this.hostName).start();
    }

    public synchronized int openStreamCount() {
        return this.streams.size();
    }

    private synchronized SpdyStream getStream(int id) {
        return (SpdyStream) this.streams.get(Integer.valueOf(id));
    }

    synchronized SpdyStream removeStream(int streamId) {
        SpdyStream stream;
        stream = (SpdyStream) this.streams.remove(Integer.valueOf(streamId));
        if (stream != null && this.streams.isEmpty()) {
            setIdle(true);
        }
        return stream;
    }

    private synchronized void setIdle(boolean value) {
        this.idleStartTimeNs = value ? System.nanoTime() : 0;
    }

    public synchronized boolean isIdle() {
        return this.idleStartTimeNs != 0 ? true : $assertionsDisabled;
    }

    public synchronized long getIdleStartTimeNs() {
        return this.idleStartTimeNs;
    }

    public SpdyStream newStream(List<String> requestHeaders, boolean out, boolean in) throws IOException {
        int i;
        int i2;
        SpdyStream stream;
        if (out) {
            i = TYPE_DATA;
        } else {
            i = TYPE_SYN_STREAM;
        }
        if (in) {
            i2 = TYPE_DATA;
        } else {
            i2 = TYPE_SYN_REPLY;
        }
        int flags = i | i2;
        synchronized (this.spdyWriter) {
            int streamId;
            synchronized (this) {
                if (this.shutdown) {
                    throw new IOException("shutdown");
                }
                streamId = this.nextStreamId;
                this.nextStreamId += TYPE_SYN_REPLY;
                stream = new SpdyStream(streamId, this, flags, TYPE_DATA, TYPE_DATA, requestHeaders, this.settings);
                if (stream.isOpen()) {
                    this.streams.put(Integer.valueOf(streamId), stream);
                    setIdle($assertionsDisabled);
                }
            }
            this.spdyWriter.synStream(flags, streamId, TYPE_DATA, TYPE_DATA, TYPE_DATA, requestHeaders);
        }
        return stream;
    }

    void writeSynReply(int streamId, int flags, List<String> alternating) throws IOException {
        this.spdyWriter.synReply(flags, streamId, alternating);
    }

    void writeFrame(byte[] bytes, int offset, int length) throws IOException {
        synchronized (this.spdyWriter) {
            this.spdyWriter.out.write(bytes, offset, length);
        }
    }

    void writeSynResetLater(int streamId, int statusCode) {
        ExecutorService executorService = executor;
        Object[] objArr = new Object[TYPE_SYN_REPLY];
        objArr[TYPE_DATA] = this.hostName;
        objArr[TYPE_SYN_STREAM] = Integer.valueOf(streamId);
        executorService.submit(new C00971(String.format("Spdy Writer %s stream %d", objArr), streamId, statusCode));
    }

    void writeSynReset(int streamId, int statusCode) throws IOException {
        this.spdyWriter.rstStream(streamId, statusCode);
    }

    void writeWindowUpdateLater(int streamId, int deltaWindowSize) {
        ExecutorService executorService = executor;
        Object[] objArr = new Object[TYPE_SYN_REPLY];
        objArr[TYPE_DATA] = this.hostName;
        objArr[TYPE_SYN_STREAM] = Integer.valueOf(streamId);
        executorService.submit(new C00982(String.format("Spdy Writer %s stream %d", objArr), streamId, deltaWindowSize));
    }

    void writeWindowUpdate(int streamId, int deltaWindowSize) throws IOException {
        this.spdyWriter.windowUpdate(streamId, deltaWindowSize);
    }

    public Ping ping() throws IOException {
        int pingId;
        Ping ping = new Ping();
        synchronized (this) {
            if (this.shutdown) {
                throw new IOException("shutdown");
            }
            pingId = this.nextPingId;
            this.nextPingId += TYPE_SYN_REPLY;
            if (this.pings == null) {
                this.pings = new HashMap();
            }
            this.pings.put(Integer.valueOf(pingId), ping);
        }
        writePing(pingId, ping);
        return ping;
    }

    private void writePingLater(int streamId, Ping ping) {
        ExecutorService executorService = executor;
        Object[] objArr = new Object[TYPE_SYN_REPLY];
        objArr[TYPE_DATA] = this.hostName;
        objArr[TYPE_SYN_STREAM] = Integer.valueOf(streamId);
        executorService.submit(new C00993(String.format("Spdy Writer %s ping %d", objArr), streamId, ping));
    }

    private void writePing(int id, Ping ping) throws IOException {
        synchronized (this.spdyWriter) {
            if (ping != null) {
                ping.send();
            }
            this.spdyWriter.ping(TYPE_DATA, id);
        }
    }

    private synchronized Ping removePing(int id) {
        return this.pings != null ? (Ping) this.pings.remove(Integer.valueOf(id)) : null;
    }

    public void noop() throws IOException {
        this.spdyWriter.noop();
    }

    public void flush() throws IOException {
        synchronized (this.spdyWriter) {
            this.spdyWriter.out.flush();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void shutdown(int r5) throws java.io.IOException {
        /*
        r4 = this;
        r2 = r4.spdyWriter;
        monitor-enter(r2);
        monitor-enter(r4);	 Catch:{ all -> 0x0019 }
        r1 = r4.shutdown;	 Catch:{ all -> 0x001c }
        if (r1 == 0) goto L_0x000b;
    L_0x0008:
        monitor-exit(r4);	 Catch:{ all -> 0x001c }
        monitor-exit(r2);	 Catch:{ all -> 0x0019 }
    L_0x000a:
        return;
    L_0x000b:
        r1 = 1;
        r4.shutdown = r1;	 Catch:{ all -> 0x001c }
        r0 = r4.lastGoodStreamId;	 Catch:{ all -> 0x001c }
        monitor-exit(r4);	 Catch:{ all -> 0x001c }
        r1 = r4.spdyWriter;	 Catch:{ all -> 0x0019 }
        r3 = 0;
        r1.goAway(r3, r0, r5);	 Catch:{ all -> 0x0019 }
        monitor-exit(r2);	 Catch:{ all -> 0x0019 }
        goto L_0x000a;
    L_0x0019:
        r1 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x0019 }
        throw r1;
    L_0x001c:
        r1 = move-exception;
        monitor-exit(r4);	 Catch:{ all -> 0x001c }
        throw r1;	 Catch:{ all -> 0x0019 }
        */
        throw new UnsupportedOperationException("Method not decompiled: com.squareup.okhttp.internal.spdy.SpdyConnection.shutdown(int):void");
    }

    public void close() throws IOException {
        close(TYPE_DATA, TYPE_NOOP);
    }

    private void close(int shutdownStatusCode, int rstStatusCode) throws IOException {
        if ($assertionsDisabled || !Thread.holdsLock(this)) {
            int len$;
            int i$;
            IOException thrown = null;
            try {
                shutdown(shutdownStatusCode);
            } catch (IOException e) {
                thrown = e;
            }
            SpdyStream[] streamsToClose = null;
            Ping[] pingsToCancel = null;
            synchronized (this) {
                if (!this.streams.isEmpty()) {
                    streamsToClose = (SpdyStream[]) this.streams.values().toArray(new SpdyStream[this.streams.size()]);
                    this.streams.clear();
                    setIdle($assertionsDisabled);
                }
                if (this.pings != null) {
                    pingsToCancel = (Ping[]) this.pings.values().toArray(new Ping[this.pings.size()]);
                    this.pings = null;
                }
            }
            if (streamsToClose != null) {
                SpdyStream[] arr$ = streamsToClose;
                len$ = arr$.length;
                for (i$ = TYPE_DATA; i$ < len$; i$ += TYPE_SYN_STREAM) {
                    try {
                        arr$[i$].close(rstStatusCode);
                    } catch (IOException e2) {
                        if (thrown != null) {
                            thrown = e2;
                        }
                    }
                }
            }
            if (pingsToCancel != null) {
                Ping[] arr$2 = pingsToCancel;
                len$ = arr$2.length;
                for (i$ = TYPE_DATA; i$ < len$; i$ += TYPE_SYN_STREAM) {
                    arr$2[i$].cancel();
                }
            }
            try {
                this.spdyReader.close();
            } catch (IOException e22) {
                thrown = e22;
            }
            try {
                this.spdyWriter.close();
            } catch (IOException e222) {
                if (thrown == null) {
                    thrown = e222;
                }
            }
            if (thrown != null) {
                throw thrown;
            }
            return;
        }
        throw new AssertionError();
    }
}
