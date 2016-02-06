package com.squareup.okhttp.internal;

import com.squareup.okhttp.internal.http.HttpTransport;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

public final class Util {
    public static final byte[] EMPTY_BYTE_ARRAY;
    public static final String[] EMPTY_STRING_ARRAY;
    public static final Charset ISO_8859_1;
    public static final Charset US_ASCII;
    public static final Charset UTF_8;
    private static AtomicReference<byte[]> skipBuffer;

    static {
        EMPTY_BYTE_ARRAY = new byte[0];
        EMPTY_STRING_ARRAY = new String[0];
        ISO_8859_1 = Charset.forName("ISO-8859-1");
        US_ASCII = Charset.forName("US-ASCII");
        UTF_8 = Charset.forName("UTF-8");
        skipBuffer = new AtomicReference();
    }

    private Util() {
    }

    public static int getEffectivePort(URI uri) {
        return getEffectivePort(uri.getScheme(), uri.getPort());
    }

    public static int getEffectivePort(URL url) {
        return getEffectivePort(url.getProtocol(), url.getPort());
    }

    private static int getEffectivePort(String scheme, int specifiedPort) {
        return specifiedPort != -1 ? specifiedPort : getDefaultPort(scheme);
    }

    public static int getDefaultPort(String scheme) {
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return -1;
    }

    public static void checkOffsetAndCount(int arrayLength, int offset, int count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public static void pokeInt(byte[] dst, int offset, int value, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            int i = offset + 1;
            dst[offset] = (byte) ((value >> 24) & 255);
            offset = i + 1;
            dst[i] = (byte) ((value >> 16) & 255);
            i = offset + 1;
            dst[offset] = (byte) ((value >> 8) & 255);
            dst[i] = (byte) ((value >> 0) & 255);
            offset = i;
            return;
        }
        i = offset + 1;
        dst[offset] = (byte) ((value >> 0) & 255);
        offset = i + 1;
        dst[i] = (byte) ((value >> 8) & 255);
        i = offset + 1;
        dst[offset] = (byte) ((value >> 16) & 255);
        dst[i] = (byte) ((value >> 24) & 255);
        offset = i;
    }

    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }

    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }

    public static void closeAll(Closeable a, Closeable b) throws IOException {
        Throwable thrown = null;
        try {
            a.close();
        } catch (Throwable e) {
            thrown = e;
        }
        try {
            b.close();
        } catch (Throwable e2) {
            if (thrown == null) {
                thrown = e2;
            }
        }
        if (thrown != null) {
            if (thrown instanceof IOException) {
                throw ((IOException) thrown);
            } else if (thrown instanceof RuntimeException) {
                throw ((RuntimeException) thrown);
            } else if (thrown instanceof Error) {
                throw ((Error) thrown);
            } else {
                throw new AssertionError(thrown);
            }
        }
    }

    public static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("not a readable directory: " + dir);
        }
        File[] arr$ = files;
        int len$ = arr$.length;
        int i$ = 0;
        while (i$ < len$) {
            File file = arr$[i$];
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (file.delete()) {
                i$++;
            } else {
                throw new IOException("failed to delete file: " + file);
            }
        }
    }

    public static int readSingleByte(InputStream in) throws IOException {
        byte[] buffer = new byte[1];
        if (in.read(buffer, 0, 1) != -1) {
            return buffer[0] & 255;
        }
        return -1;
    }

    public static void writeSingleByte(OutputStream out, int b) throws IOException {
        out.write(new byte[]{(byte) (b & 255)});
    }

    public static void readFully(InputStream in, byte[] dst) throws IOException {
        readFully(in, dst, 0, dst.length);
    }

    public static void readFully(InputStream in, byte[] dst, int offset, int byteCount) throws IOException {
        if (byteCount != 0) {
            if (in == null) {
                throw new NullPointerException("in == null");
            } else if (dst == null) {
                throw new NullPointerException("dst == null");
            } else {
                checkOffsetAndCount(dst.length, offset, byteCount);
                while (byteCount > 0) {
                    int bytesRead = in.read(dst, offset, byteCount);
                    if (bytesRead < 0) {
                        throw new EOFException();
                    }
                    offset += bytesRead;
                    byteCount -= bytesRead;
                }
            }
        }
    }

    public static String readFully(Reader reader) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[HttpTransport.DEFAULT_CHUNK_LENGTH];
            while (true) {
                int count = reader.read(buffer);
                if (count == -1) {
                    break;
                }
                writer.write(buffer, 0, count);
            }
            String stringWriter = writer.toString();
            return stringWriter;
        } finally {
            reader.close();
        }
    }

    public static void skipAll(InputStream in) throws IOException {
        do {
            in.skip(Long.MAX_VALUE);
        } while (in.read() != -1);
    }

    public static long skipByReading(InputStream in, long byteCount) throws IOException {
        byte[] buffer = (byte[]) skipBuffer.getAndSet(null);
        if (buffer == null) {
            buffer = new byte[4096];
        }
        long skipped = 0;
        while (skipped < byteCount) {
            int toRead = (int) Math.min(byteCount - skipped, (long) buffer.length);
            int read = in.read(buffer, 0, toRead);
            if (read != -1) {
                skipped += (long) read;
                if (read < toRead) {
                    break;
                }
            }
            break;
        }
        skipBuffer.set(buffer);
        return skipped;
    }

    public static int copy(InputStream in, OutputStream out) throws IOException {
        int total = 0;
        byte[] buffer = new byte[8192];
        while (true) {
            int c = in.read(buffer);
            if (c == -1) {
                return total;
            }
            total += c;
            out.write(buffer, 0, c);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String readAsciiLine(java.io.InputStream r5) throws java.io.IOException {
        /*
        r2 = new java.lang.StringBuilder;
        r3 = 80;
        r2.<init>(r3);
    L_0x0007:
        r0 = r5.read();
        r3 = -1;
        if (r0 != r3) goto L_0x0014;
    L_0x000e:
        r3 = new java.io.EOFException;
        r3.<init>();
        throw r3;
    L_0x0014:
        r3 = 10;
        if (r0 != r3) goto L_0x0032;
    L_0x0018:
        r1 = r2.length();
        if (r1 <= 0) goto L_0x002d;
    L_0x001e:
        r3 = r1 + -1;
        r3 = r2.charAt(r3);
        r4 = 13;
        if (r3 != r4) goto L_0x002d;
    L_0x0028:
        r3 = r1 + -1;
        r2.setLength(r3);
    L_0x002d:
        r3 = r2.toString();
        return r3;
    L_0x0032:
        r3 = (char) r0;
        r2.append(r3);
        goto L_0x0007;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.squareup.okhttp.internal.Util.readAsciiLine(java.io.InputStream):java.lang.String");
    }
}
