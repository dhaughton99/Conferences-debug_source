package org.apache.cordova.contacts;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactManager extends CordovaPlugin {
    private static final int CONTACT_PICKER_RESULT = 1000;
    public static final int INVALID_ARGUMENT_ERROR = 1;
    public static final int IO_ERROR = 4;
    private static final String LOG_TAG = "Contact Query";
    public static final int NOT_SUPPORTED_ERROR = 5;
    public static final int PENDING_OPERATION_ERROR = 3;
    public static final int PERMISSION_DENIED_ERROR = 20;
    public static final int TIMEOUT_ERROR = 2;
    public static final int UNKNOWN_ERROR = 0;
    private CallbackContext callbackContext;
    private ContactAccessor contactAccessor;
    private JSONArray executeArgs;

    /* renamed from: org.apache.cordova.contacts.ContactManager.1 */
    class C00511 implements Runnable {
        final /* synthetic */ CallbackContext val$callbackContext;
        final /* synthetic */ JSONArray val$filter;
        final /* synthetic */ JSONObject val$options;

        C00511(JSONArray jSONArray, JSONObject jSONObject, CallbackContext callbackContext) {
            this.val$filter = jSONArray;
            this.val$options = jSONObject;
            this.val$callbackContext = callbackContext;
        }

        public void run() {
            this.val$callbackContext.success(ContactManager.this.contactAccessor.search(this.val$filter, this.val$options));
        }
    }

    /* renamed from: org.apache.cordova.contacts.ContactManager.2 */
    class C00522 implements Runnable {
        final /* synthetic */ CallbackContext val$callbackContext;
        final /* synthetic */ JSONObject val$contact;

        C00522(JSONObject jSONObject, CallbackContext callbackContext) {
            this.val$contact = jSONObject;
            this.val$callbackContext = callbackContext;
        }

        public void run() {
            JSONObject res = null;
            String id = ContactManager.this.contactAccessor.save(this.val$contact);
            if (id != null) {
                try {
                    res = ContactManager.this.contactAccessor.getContactById(id);
                } catch (JSONException e) {
                    Log.e(ContactManager.LOG_TAG, "JSON fail.", e);
                }
            }
            if (res != null) {
                this.val$callbackContext.success(res);
            } else {
                this.val$callbackContext.sendPluginResult(new PluginResult(Status.ERROR, 0));
            }
        }
    }

    /* renamed from: org.apache.cordova.contacts.ContactManager.3 */
    class C00533 implements Runnable {
        final /* synthetic */ CallbackContext val$callbackContext;
        final /* synthetic */ String val$contactId;

        C00533(String str, CallbackContext callbackContext) {
            this.val$contactId = str;
            this.val$callbackContext = callbackContext;
        }

        public void run() {
            if (ContactManager.this.contactAccessor.remove(this.val$contactId)) {
                this.val$callbackContext.success();
            } else {
                this.val$callbackContext.sendPluginResult(new PluginResult(Status.ERROR, 0));
            }
        }
    }

    /* renamed from: org.apache.cordova.contacts.ContactManager.4 */
    class C00544 implements Runnable {
        final /* synthetic */ CordovaPlugin val$plugin;

        C00544(CordovaPlugin cordovaPlugin) {
            this.val$plugin = cordovaPlugin;
        }

        public void run() {
            this.val$plugin.cordova.startActivityForResult(this.val$plugin, new Intent("android.intent.action.PICK", Contacts.CONTENT_URI), ContactManager.CONTACT_PICKER_RESULT);
        }
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        this.executeArgs = args;
        if (VERSION.RELEASE.startsWith("1.")) {
            callbackContext.sendPluginResult(new PluginResult(Status.ERROR, (int) NOT_SUPPORTED_ERROR));
            return true;
        }
        if (this.contactAccessor == null) {
            this.contactAccessor = new ContactAccessorSdk5(this.cordova);
        }
        if (action.equals("search")) {
            this.cordova.getThreadPool().execute(new C00511(args.getJSONArray(0), args.get(INVALID_ARGUMENT_ERROR) == null ? null : args.getJSONObject(INVALID_ARGUMENT_ERROR), callbackContext));
            return true;
        } else if (action.equals("save")) {
            this.cordova.getThreadPool().execute(new C00522(args.getJSONObject(0), callbackContext));
            return true;
        } else if (action.equals("remove")) {
            this.cordova.getThreadPool().execute(new C00533(args.getString(0), callbackContext));
            return true;
        } else if (!action.equals("pickContact")) {
            return false;
        } else {
            pickContactAsync();
            return true;
        }
    }

    private void pickContactAsync() {
        this.cordova.getThreadPool().execute(new C00544(this));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode != CONTACT_PICKER_RESULT) {
            return;
        }
        if (resultCode == -1) {
            String contactId = intent.getData().getLastPathSegment();
            ContentResolver contentResolver = this.cordova.getActivity().getContentResolver();
            Uri uri = RawContacts.CONTENT_URI;
            String[] strArr = new String[INVALID_ARGUMENT_ERROR];
            strArr[0] = "_id";
            Cursor c = contentResolver.query(uri, strArr, "contact_id = " + contactId, null, null);
            if (c.moveToFirst()) {
                String id = c.getString(c.getColumnIndex("_id"));
                c.close();
                try {
                    this.callbackContext.success(this.contactAccessor.getContactById(id));
                    return;
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "JSON fail.", e);
                }
            } else {
                this.callbackContext.error("Error occured while retrieving contact raw id");
                return;
            }
        }
        if (resultCode == 0) {
            this.callbackContext.sendPluginResult(new PluginResult(Status.NO_RESULT, 0));
            return;
        }
        this.callbackContext.sendPluginResult(new PluginResult(Status.ERROR, 0));
    }
}
