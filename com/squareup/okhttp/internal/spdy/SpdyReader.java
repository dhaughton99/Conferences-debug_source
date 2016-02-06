package com.squareup.okhttp.internal.spdy;

import com.squareup.okhttp.internal.Util;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.apache.cordova.PluginResult;
import org.apache.cordova.contacts.ContactManager;

final class SpdyReader implements Closeable {
    static final byte[] DICTIONARY;
    private int compressedLimit;
    private final DataInputStream in;
    private final DataInputStream nameValueBlockIn;

    /* renamed from: com.squareup.okhttp.internal.spdy.SpdyReader.1 */
    class C00181 extends InputStream {
        C00181() {
        }

        public int read() throws IOException {
            return Util.readSingleByte(this);
        }

        public int read(byte[] buffer, int offset, int byteCount) throws IOException {
            int consumed = SpdyReader.this.in.read(buffer, offset, Math.min(byteCount, SpdyReader.this.compressedLimit));
            SpdyReader.access$020(SpdyReader.this, consumed);
            return consumed;
        }

        public void close() throws IOException {
            SpdyReader.this.in.close();
        }
    }

    /* renamed from: com.squareup.okhttp.internal.spdy.SpdyReader.2 */
    class C00192 extends Inflater {
        C00192() {
        }

        public int inflate(byte[] buffer, int offset, int count) throws DataFormatException {
            int result = super.inflate(buffer, offset, count);
            if (result != 0 || !needsDictionary()) {
                return result;
            }
            setDictionary(SpdyReader.DICTIONARY);
            return super.inflate(buffer, offset, count);
        }
    }

    public interface Handler {
        void data(int i, int i2, InputStream inputStream, int i3) throws IOException;

        void goAway(int i, int i2, int i3);

        void headers(int i, int i2, List<String> list) throws IOException;

        void noop();

        void ping(int i, int i2);

        void rstStream(int i, int i2, int i3);

        void settings(int i, Settings settings);

        void synReply(int i, int i2, List<String> list) throws IOException;

        void synStream(int i, int i2, int i3, int i4, int i5, List<String> list);

        void windowUpdate(int i, int i2, int i3);
    }

    static /* synthetic */ int access$020(SpdyReader x0, int x1) {
        int i = x0.compressedLimit - x1;
        x0.compressedLimit = i;
        return i;
    }

    static {
        try {
            DICTIONARY = "\u0000\u0000\u0000\u0007options\u0000\u0000\u0000\u0004head\u0000\u0000\u0000\u0004post\u0000\u0000\u0000\u0003put\u0000\u0000\u0000\u0006delete\u0000\u0000\u0000\u0005trace\u0000\u0000\u0000\u0006accept\u0000\u0000\u0000\u000eaccept-charset\u0000\u0000\u0000\u000faccept-encoding\u0000\u0000\u0000\u000faccept-language\u0000\u0000\u0000\raccept-ranges\u0000\u0000\u0000\u0003age\u0000\u0000\u0000\u0005allow\u0000\u0000\u0000\rauthorization\u0000\u0000\u0000\rcache-control\u0000\u0000\u0000\nconnection\u0000\u0000\u0000\fcontent-base\u0000\u0000\u0000\u0010content-encoding\u0000\u0000\u0000\u0010content-language\u0000\u0000\u0000\u000econtent-length\u0000\u0000\u0000\u0010content-location\u0000\u0000\u0000\u000bcontent-md5\u0000\u0000\u0000\rcontent-range\u0000\u0000\u0000\fcontent-type\u0000\u0000\u0000\u0004date\u0000\u0000\u0000\u0004etag\u0000\u0000\u0000\u0006expect\u0000\u0000\u0000\u0007expires\u0000\u0000\u0000\u0004from\u0000\u0000\u0000\u0004host\u0000\u0000\u0000\bif-match\u0000\u0000\u0000\u0011if-modified-since\u0000\u0000\u0000\rif-none-match\u0000\u0000\u0000\bif-range\u0000\u0000\u0000\u0013if-unmodified-since\u0000\u0000\u0000\rlast-modified\u0000\u0000\u0000\blocation\u0000\u0000\u0000\fmax-forwards\u0000\u0000\u0000\u0006pragma\u0000\u0000\u0000\u0012proxy-authenticate\u0000\u0000\u0000\u0013proxy-authorization\u0000\u0000\u0000\u0005range\u0000\u0000\u0000\u0007referer\u0000\u0000\u0000\u000bretry-after\u0000\u0000\u0000\u0006server\u0000\u0000\u0000\u0002te\u0000\u0000\u0000\u0007trailer\u0000\u0000\u0000\u0011transfer-encoding\u0000\u0000\u0000\u0007upgrade\u0000\u0000\u0000\nuser-agent\u0000\u0000\u0000\u0004vary\u0000\u0000\u0000\u0003via\u0000\u0000\u0000\u0007warning\u0000\u0000\u0000\u0010www-authenticate\u0000\u0000\u0000\u0006method\u0000\u0000\u0000\u0003get\u0000\u0000\u0000\u0006status\u0000\u0000\u0000\u0006200 OK\u0000\u0000\u0000\u0007version\u0000\u0000\u0000\bHTTP/1.1\u0000\u0000\u0000\u0003url\u0000\u0000\u0000\u0006public\u0000\u0000\u0000\nset-cookie\u0000\u0000\u0000\nkeep-alive\u0000\u0000\u0000\u0006origin100101201202205206300302303304305306307402405406407408409410411412413414415416417502504505203 Non-Authoritative Information204 No Content301 Moved Permanently400 Bad Request401 Unauthorized403 Forbidden404 Not Found500 Internal Server Error501 Not Implemented503 Service UnavailableJan Feb Mar Apr May Jun Jul Aug Sept Oct Nov Dec 00:00:00 Mon, Tue, Wed, Thu, Fri, Sat, Sun, GMTchunked,text/html,image/png,image/jpg,image/gif,application/xml,application/xhtml+xml,text/plain,text/javascript,publicprivatemax-age=gzip,deflate,sdchcharset=utf-8charset=iso-8859-1,utf-,*,enq=0.".getBytes(Util.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }

    SpdyReader(InputStream in) {
        this.in = new DataInputStream(in);
        this.nameValueBlockIn = newNameValueBlockStream();
    }

    public boolean nextFrame(Handler handler) throws IOException {
        try {
            boolean control;
            int w1 = this.in.readInt();
            int w2 = this.in.readInt();
            if ((Integer.MIN_VALUE & w1) != 0) {
                control = true;
            } else {
                control = false;
            }
            int flags = (-16777216 & w2) >>> 24;
            int length = w2 & 16777215;
            if (control) {
                int version = (2147418112 & w1) >>> 16;
                int type = w1 & 65535;
                if (version != 3) {
                    throw new ProtocolException("version != 3: " + version);
                }
                switch (type) {
                    case ContactManager.INVALID_ARGUMENT_ERROR /*1*/:
                        readSynStream(handler, flags, length);
                        return true;
                    case ContactManager.TIMEOUT_ERROR /*2*/:
                        readSynReply(handler, flags, length);
                        return true;
                    case ContactManager.PENDING_OPERATION_ERROR /*3*/:
                        readRstStream(handler, flags, length);
                        return true;
                    case ContactManager.IO_ERROR /*4*/:
                        readSettings(handler, flags, length);
                        return true;
                    case ContactManager.NOT_SUPPORTED_ERROR /*5*/:
                        if (length != 0) {
                            throw ioException("TYPE_NOOP length: %d != 0", Integer.valueOf(length));
                        }
                        handler.noop();
                        return true;
                    case PluginResult.MESSAGE_TYPE_ARRAYBUFFER /*6*/:
                        readPing(handler, flags, length);
                        return true;
                    case PluginResult.MESSAGE_TYPE_BINARYSTRING /*7*/:
                        readGoAway(handler, flags, length);
                        return true;
                    case SpdyStream.RST_STREAM_IN_USE /*8*/:
                        readHeaders(handler, flags, length);
                        return true;
                    case SpdyStream.RST_STREAM_ALREADY_CLOSED /*9*/:
                        readWindowUpdate(handler, flags, length);
                        return true;
                    case 16:
                        Util.skipByReading(this.in, (long) length);
                        throw new UnsupportedOperationException("TODO");
                    default:
                        throw new IOException("Unexpected frame");
                }
            }
            handler.data(flags, w1 & Integer.MAX_VALUE, this.in, length);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void readSynStream(Handler handler, int flags, int length) throws IOException {
        int w1 = this.in.readInt();
        int w2 = this.in.readInt();
        int s3 = this.in.readShort();
        handler.synStream(flags, w1 & Integer.MAX_VALUE, w2 & Integer.MAX_VALUE, (57344 & s3) >>> 13, s3 & 255, readNameValueBlock(length - 10));
    }

    private void readSynReply(Handler handler, int flags, int length) throws IOException {
        handler.synReply(flags, this.in.readInt() & Integer.MAX_VALUE, readNameValueBlock(length - 4));
    }

    private void readRstStream(Handler handler, int flags, int length) throws IOException {
        if (length != 8) {
            throw ioException("TYPE_RST_STREAM length: %d != 8", Integer.valueOf(length));
        } else {
            handler.rstStream(flags, this.in.readInt() & Integer.MAX_VALUE, this.in.readInt());
        }
    }

    private void readHeaders(Handler handler, int flags, int length) throws IOException {
        handler.headers(flags, this.in.readInt() & Integer.MAX_VALUE, readNameValueBlock(length - 4));
    }

    private void readWindowUpdate(Handler handler, int flags, int length) throws IOException {
        if (length != 8) {
            throw ioException("TYPE_WINDOW_UPDATE length: %d != 8", Integer.valueOf(length));
        }
        handler.windowUpdate(flags, this.in.readInt() & Integer.MAX_VALUE, this.in.readInt() & Integer.MAX_VALUE);
    }

    private DataInputStream newNameValueBlockStream() {
        return new DataInputStream(new InflaterInputStream(new C00181(), new C00192()));
    }

    private List<String> readNameValueBlock(int length) throws IOException {
        this.compressedLimit += length;
        try {
            int numberOfPairs = this.nameValueBlockIn.readInt();
            if (numberOfPairs < 0) {
                Logger.getLogger(getClass().getName()).warning("numberOfPairs < 0: " + numberOfPairs);
                throw ioException("numberOfPairs < 0", new Object[0]);
            }
            List<String> entries = new ArrayList(numberOfPairs * 2);
            int i = 0;
            while (i < numberOfPairs) {
                String name = readString();
                String values = readString();
                if (name.length() == 0) {
                    throw ioException("name.length == 0", new Object[0]);
                } else if (values.length() == 0) {
                    throw ioException("values.length == 0", new Object[0]);
                } else {
                    entries.add(name);
                    entries.add(values);
                    i++;
                }
            }
            if (this.compressedLimit != 0) {
                Logger.getLogger(getClass().getName()).warning("compressedLimit > 0: " + this.compressedLimit);
            }
            return entries;
        } catch (DataFormatException e) {
            throw new IOException(e.getMessage());
        }
    }

    private String readString() throws DataFormatException, IOException {
        int length = this.nameValueBlockIn.readInt();
        byte[] bytes = new byte[length];
        Util.readFully(this.nameValueBlockIn, bytes);
        return new String(bytes, 0, length, "UTF-8");
    }

    private void readPing(Handler handler, int flags, int length) throws IOException {
        if (length != 4) {
            throw ioException("TYPE_PING length: %d != 4", Integer.valueOf(length));
        } else {
            handler.ping(flags, this.in.readInt());
        }
    }

    private void readGoAway(Handler handler, int flags, int length) throws IOException {
        if (length != 8) {
            throw ioException("TYPE_GOAWAY length: %d != 8", Integer.valueOf(length));
        } else {
            handler.goAway(flags, this.in.readInt() & Integer.MAX_VALUE, this.in.readInt());
        }
    }

    private void readSettings(Handler handler, int flags, int length) throws IOException {
        int numberOfEntries = this.in.readInt();
        if (length != (numberOfEntries * 8) + 4) {
            throw ioException("TYPE_SETTINGS length: %d != 4 + 8 * %d", Integer.valueOf(length), Integer.valueOf(numberOfEntries));
        }
        Settings settings = new Settings();
        for (int i = 0; i < numberOfEntries; i++) {
            int w1 = this.in.readInt();
            int id = w1 & 16777215;
            settings.set(id, (-16777216 & w1) >>> 24, this.in.readInt());
        }
        handler.settings(flags, settings);
    }

    private static IOException ioException(String message, Object... args) throws IOException {
        throw new IOException(String.format(message, args));
    }

    public void close() throws IOException {
        Util.closeAll(this.in, this.nameValueBlockIn);
    }
}
