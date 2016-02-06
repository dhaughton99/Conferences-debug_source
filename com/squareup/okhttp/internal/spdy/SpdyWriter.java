package com.squareup.okhttp.internal.spdy;

import com.squareup.okhttp.internal.Platform;
import com.squareup.okhttp.internal.Util;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.Deflater;

final class SpdyWriter implements Closeable {
    private final ByteArrayOutputStream nameValueBlockBuffer;
    private final DataOutputStream nameValueBlockOut;
    final DataOutputStream out;

    SpdyWriter(OutputStream out) {
        this.out = new DataOutputStream(out);
        Deflater deflater = new Deflater();
        deflater.setDictionary(SpdyReader.DICTIONARY);
        this.nameValueBlockBuffer = new ByteArrayOutputStream();
        this.nameValueBlockOut = new DataOutputStream(Platform.get().newDeflaterOutputStream(this.nameValueBlockBuffer, deflater, true));
    }

    public synchronized void synStream(int flags, int streamId, int associatedStreamId, int priority, int slot, List<String> nameValueBlock) throws IOException {
        writeNameValueBlockToBuffer(nameValueBlock);
        int length = this.nameValueBlockBuffer.size() + 10;
        this.out.writeInt(-2147287039);
        this.out.writeInt(((flags & 255) << 24) | (16777215 & length));
        this.out.writeInt(streamId & Integer.MAX_VALUE);
        this.out.writeInt(associatedStreamId & Integer.MAX_VALUE);
        this.out.writeShort((((priority & 7) << 13) | 0) | (slot & 255));
        this.nameValueBlockBuffer.writeTo(this.out);
        this.out.flush();
    }

    public synchronized void synReply(int flags, int streamId, List<String> nameValueBlock) throws IOException {
        writeNameValueBlockToBuffer(nameValueBlock);
        int length = this.nameValueBlockBuffer.size() + 4;
        this.out.writeInt(-2147287038);
        this.out.writeInt(((flags & 255) << 24) | (16777215 & length));
        this.out.writeInt(Integer.MAX_VALUE & streamId);
        this.nameValueBlockBuffer.writeTo(this.out);
        this.out.flush();
    }

    public synchronized void headers(int flags, int streamId, List<String> nameValueBlock) throws IOException {
        writeNameValueBlockToBuffer(nameValueBlock);
        int length = this.nameValueBlockBuffer.size() + 4;
        this.out.writeInt(-2147287032);
        this.out.writeInt(((flags & 255) << 24) | (16777215 & length));
        this.out.writeInt(Integer.MAX_VALUE & streamId);
        this.nameValueBlockBuffer.writeTo(this.out);
        this.out.flush();
    }

    public synchronized void rstStream(int streamId, int statusCode) throws IOException {
        this.out.writeInt(-2147287037);
        this.out.writeInt(8);
        this.out.writeInt(Integer.MAX_VALUE & streamId);
        this.out.writeInt(statusCode);
        this.out.flush();
    }

    public synchronized void data(int flags, int streamId, byte[] data) throws IOException {
        int length = data.length;
        this.out.writeInt(Integer.MAX_VALUE & streamId);
        this.out.writeInt(((flags & 255) << 24) | (16777215 & length));
        this.out.write(data);
        this.out.flush();
    }

    private void writeNameValueBlockToBuffer(List<String> nameValueBlock) throws IOException {
        this.nameValueBlockBuffer.reset();
        this.nameValueBlockOut.writeInt(nameValueBlock.size() / 2);
        for (String s : nameValueBlock) {
            this.nameValueBlockOut.writeInt(s.length());
            this.nameValueBlockOut.write(s.getBytes("UTF-8"));
        }
        this.nameValueBlockOut.flush();
    }

    public synchronized void settings(int flags, Settings settings) throws IOException {
        int size = settings.size();
        int length = (size * 8) + 4;
        this.out.writeInt(-2147287036);
        this.out.writeInt(((flags & 255) << 24) | (length & 16777215));
        this.out.writeInt(size);
        for (int i = 0; i <= 9; i++) {
            if (settings.isSet(i)) {
                this.out.writeInt(((settings.flags(i) & 255) << 24) | (i & 16777215));
                this.out.writeInt(settings.get(i));
            }
        }
        this.out.flush();
    }

    public synchronized void noop() throws IOException {
        this.out.writeInt(-2147287035);
        this.out.writeInt(0);
        this.out.flush();
    }

    public synchronized void ping(int flags, int id) throws IOException {
        this.out.writeInt(-2147287034);
        this.out.writeInt(((flags & 255) << 24) | 4);
        this.out.writeInt(id);
        this.out.flush();
    }

    public synchronized void goAway(int flags, int lastGoodStreamId, int statusCode) throws IOException {
        this.out.writeInt(-2147287033);
        this.out.writeInt(((flags & 255) << 24) | 8);
        this.out.writeInt(lastGoodStreamId);
        this.out.writeInt(statusCode);
        this.out.flush();
    }

    public synchronized void windowUpdate(int streamId, int deltaWindowSize) throws IOException {
        this.out.writeInt(-2147287031);
        this.out.writeInt(8);
        this.out.writeInt(streamId);
        this.out.writeInt(deltaWindowSize);
        this.out.flush();
    }

    public void close() throws IOException {
        Util.closeAll(this.out, this.nameValueBlockOut);
    }
}
