package com.squareup.okhttp.internal.http;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class HttpDate {
    private static final String[] BROWSER_COMPATIBLE_DATE_FORMATS;
    private static final ThreadLocal<DateFormat> STANDARD_DATE_FORMAT;

    /* renamed from: com.squareup.okhttp.internal.http.HttpDate.1 */
    static class C00101 extends ThreadLocal<DateFormat> {
        C00101() {
        }

        protected DateFormat initialValue() {
            DateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            rfc1123.setTimeZone(TimeZone.getTimeZone("UTC"));
            return rfc1123;
        }
    }

    static {
        STANDARD_DATE_FORMAT = new C00101();
        BROWSER_COMPATIBLE_DATE_FORMATS = new String[]{"EEEE, dd-MMM-yy HH:mm:ss zzz", "EEE MMM d HH:mm:ss yyyy", "EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MMM-yyyy HH-mm-ss z", "EEE, dd MMM yy HH:mm:ss z", "EEE dd-MMM-yyyy HH:mm:ss z", "EEE dd MMM yyyy HH:mm:ss z", "EEE dd-MMM-yyyy HH-mm-ss z", "EEE dd-MMM-yy HH:mm:ss z", "EEE dd MMM yy HH:mm:ss z", "EEE,dd-MMM-yy HH:mm:ss z", "EEE,dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MM-yyyy HH:mm:ss z", "EEE MMM d yyyy HH:mm:ss z"};
    }

    public static Date parse(String value) {
        try {
            return ((DateFormat) STANDARD_DATE_FORMAT.get()).parse(value);
        } catch (ParseException e) {
            String[] arr$ = BROWSER_COMPATIBLE_DATE_FORMATS;
            int len$ = arr$.length;
            int i$ = 0;
            while (i$ < len$) {
                try {
                    return new SimpleDateFormat(arr$[i$], Locale.US).parse(value);
                } catch (ParseException e2) {
                    i$++;
                }
            }
            return null;
        }
    }

    public static String format(Date value) {
        return ((DateFormat) STANDARD_DATE_FORMAT.get()).format(value);
    }

    private HttpDate() {
    }
}
