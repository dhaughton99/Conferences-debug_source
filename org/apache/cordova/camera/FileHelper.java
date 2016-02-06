package org.apache.cordova.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION;
import android.provider.DocumentsContract;
import android.provider.MediaStore.Images.Media;
import android.webkit.MimeTypeMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.apache.cordova.CordovaInterface;

public class FileHelper {
    private static final String LOG_TAG = "FileUtils";
    private static final String _DATA = "_data";

    public static String getRealPath(Uri uri, CordovaInterface cordova) {
        if (VERSION.SDK_INT < 11) {
            return getRealPathFromURI_BelowAPI11(cordova.getActivity(), uri);
        }
        if (VERSION.SDK_INT < 19) {
            return getRealPathFromURI_API11to18(cordova.getActivity(), uri);
        }
        return getRealPathFromURI_API19(cordova.getActivity(), uri);
    }

    public static String getRealPath(String uriString, CordovaInterface cordova) {
        return getRealPath(Uri.parse(uriString), cordova);
    }

    @SuppressLint({"NewApi"})
    public static String getRealPathFromURI_API19(Context context, Uri uri) {
        String filePath = "";
        try {
            String wholeID = DocumentsContract.getDocumentId(uri);
            String id = wholeID.indexOf(":") > -1 ? wholeID.split(":")[1] : wholeID.indexOf(";") > -1 ? wholeID.split(";")[1] : wholeID;
            String[] column = new String[]{_DATA};
            Cursor cursor = context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI, column, "_id=?", new String[]{id}, null);
            int columnIndex = cursor.getColumnIndex(column[0]);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
            return filePath;
        } catch (Exception e) {
            return "";
        }
    }

    @SuppressLint({"NewApi"})
    public static String getRealPathFromURI_API11to18(Context context, Uri contentUri) {
        try {
            Cursor cursor = new CursorLoader(context, contentUri, new String[]{_DATA}, null, null, null).loadInBackground();
            if (cursor == null) {
                return null;
            }
            int column_index = cursor.getColumnIndexOrThrow(_DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRealPathFromURI_BelowAPI11(Context context, Uri contentUri) {
        try {
            Cursor cursor = context.getContentResolver().query(contentUri, new String[]{_DATA}, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(_DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            return null;
        }
    }

    public static InputStream getInputStreamFromUriString(String uriString, CordovaInterface cordova) throws IOException {
        if (uriString.startsWith("content")) {
            return cordova.getActivity().getContentResolver().openInputStream(Uri.parse(uriString));
        } else if (!uriString.startsWith("file://")) {
            return new FileInputStream(uriString);
        } else {
            int question = uriString.indexOf("?");
            if (question > -1) {
                uriString = uriString.substring(0, question);
            }
            if (uriString.startsWith("file:///android_asset/")) {
                return cordova.getActivity().getAssets().open(Uri.parse(uriString).getPath().substring(15));
            }
            InputStream returnValue;
            try {
                returnValue = cordova.getActivity().getContentResolver().openInputStream(Uri.parse(uriString));
            } catch (Exception e) {
                returnValue = null;
            }
            if (returnValue == null) {
                return new FileInputStream(getRealPath(uriString, cordova));
            }
            return returnValue;
        }
    }

    public static String stripFileProtocol(String uriString) {
        if (uriString.startsWith("file://")) {
            return uriString.substring(7);
        }
        return uriString;
    }

    public static String getMimeTypeForExtension(String path) {
        String extension = path;
        int lastDot = extension.lastIndexOf(46);
        if (lastDot != -1) {
            extension = extension.substring(lastDot + 1);
        }
        extension = extension.toLowerCase(Locale.getDefault());
        if (extension.equals("3ga")) {
            return "audio/3gpp";
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    public static String getMimeType(String uriString, CordovaInterface cordova) {
        Uri uri = Uri.parse(uriString);
        if (uriString.startsWith("content://")) {
            return cordova.getActivity().getContentResolver().getType(uri);
        }
        return getMimeTypeForExtension(uri.getPath());
    }
}
