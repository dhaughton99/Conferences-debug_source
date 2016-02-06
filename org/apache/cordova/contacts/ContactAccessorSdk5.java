package org.apache.cordova.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import com.squareup.okhttp.internal.spdy.SpdyStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import org.apache.cordova.networkinformation.NetworkManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactAccessorSdk5 extends ContactAccessor {
    private static final String EMAIL_REGEXP = ".+@.+\\.+.+";
    private static final long MAX_PHOTO_SIZE = 1048576;
    private static final Map<String, String> dbMap;

    static {
        dbMap = new HashMap();
        dbMap.put("id", "contact_id");
        dbMap.put("displayName", "display_name");
        dbMap.put("name", "data1");
        dbMap.put("name.formatted", "data1");
        dbMap.put("name.familyName", "data3");
        dbMap.put("name.givenName", "data2");
        dbMap.put("name.middleName", "data5");
        dbMap.put("name.honorificPrefix", "data4");
        dbMap.put("name.honorificSuffix", "data6");
        dbMap.put("nickname", "data1");
        dbMap.put("phoneNumbers", "data1");
        dbMap.put("phoneNumbers.value", "data1");
        dbMap.put("emails", "data1");
        dbMap.put("emails.value", "data1");
        dbMap.put("addresses", "data1");
        dbMap.put("addresses.formatted", "data1");
        dbMap.put("addresses.streetAddress", "data4");
        dbMap.put("addresses.locality", "data7");
        dbMap.put("addresses.region", "data8");
        dbMap.put("addresses.postalCode", "data9");
        dbMap.put("addresses.country", "data10");
        dbMap.put("ims", "data1");
        dbMap.put("ims.value", "data1");
        dbMap.put("organizations", "data1");
        dbMap.put("organizations.name", "data1");
        dbMap.put("organizations.department", "data5");
        dbMap.put("organizations.title", "data4");
        dbMap.put("birthday", "vnd.android.cursor.item/contact_event");
        dbMap.put("note", "data1");
        dbMap.put("photos.value", "vnd.android.cursor.item/photo");
        dbMap.put("urls", "data1");
        dbMap.put("urls.value", "data1");
    }

    public ContactAccessorSdk5(CordovaInterface context) {
        this.mApp = context;
    }

    public JSONArray search(JSONArray fields, JSONObject options) {
        String searchTerm = "";
        int limit = Integer.MAX_VALUE;
        if (options != null) {
            searchTerm = options.optString("filter");
            if (searchTerm.length() == 0) {
                searchTerm = "%";
            } else {
                searchTerm = "%" + searchTerm + "%";
            }
            try {
                if (!options.getBoolean("multiple")) {
                    limit = 1;
                }
            } catch (JSONException e) {
            }
        } else {
            searchTerm = "%";
        }
        HashMap<String, Boolean> populate = buildPopulationSet(options);
        WhereOptions whereOptions = buildWhereClause(fields, searchTerm);
        Cursor idCursor = this.mApp.getActivity().getContentResolver().query(Data.CONTENT_URI, new String[]{"contact_id"}, whereOptions.getWhere(), whereOptions.getWhereArgs(), "contact_id ASC");
        Set<String> contactIds = new HashSet();
        int idColumn = -1;
        while (idCursor.moveToNext()) {
            if (idColumn < 0) {
                idColumn = idCursor.getColumnIndex("contact_id");
            }
            contactIds.add(idCursor.getString(idColumn));
        }
        idCursor.close();
        WhereOptions idOptions = buildIdClause(contactIds, searchTerm);
        HashSet<String> columnsToFetch = new HashSet();
        columnsToFetch.add("contact_id");
        columnsToFetch.add("raw_contact_id");
        columnsToFetch.add("mimetype");
        if (isRequired("displayName", populate)) {
            columnsToFetch.add("data1");
        }
        if (isRequired("name", populate)) {
            columnsToFetch.add("data3");
            columnsToFetch.add("data2");
            columnsToFetch.add("data5");
            columnsToFetch.add("data4");
            columnsToFetch.add("data6");
        }
        if (isRequired("phoneNumbers", populate)) {
            columnsToFetch.add("_id");
            columnsToFetch.add("data1");
            columnsToFetch.add("data2");
        }
        if (isRequired("emails", populate)) {
            columnsToFetch.add("_id");
            columnsToFetch.add("data1");
            columnsToFetch.add("data2");
        }
        if (isRequired("addresses", populate)) {
            columnsToFetch.add("_id");
            columnsToFetch.add("data2");
            columnsToFetch.add("data1");
            columnsToFetch.add("data4");
            columnsToFetch.add("data7");
            columnsToFetch.add("data8");
            columnsToFetch.add("data9");
            columnsToFetch.add("data10");
        }
        if (isRequired("organizations", populate)) {
            columnsToFetch.add("_id");
            columnsToFetch.add("data2");
            columnsToFetch.add("data5");
            columnsToFetch.add("data1");
            columnsToFetch.add("data4");
        }
        if (isRequired("ims", populate)) {
            columnsToFetch.add("_id");
            columnsToFetch.add("data1");
            columnsToFetch.add("data2");
        }
        if (isRequired("note", populate)) {
            columnsToFetch.add("data1");
        }
        if (isRequired("nickname", populate)) {
            columnsToFetch.add("data1");
        }
        if (isRequired("urls", populate)) {
            columnsToFetch.add("_id");
            columnsToFetch.add("data1");
            columnsToFetch.add("data2");
        }
        if (isRequired("birthday", populate)) {
            columnsToFetch.add("data1");
            columnsToFetch.add("data2");
        }
        if (isRequired("photos", populate)) {
            columnsToFetch.add("_id");
        }
        return populateContactArray(limit, populate, this.mApp.getActivity().getContentResolver().query(Data.CONTENT_URI, (String[]) columnsToFetch.toArray(new String[0]), idOptions.getWhere(), idOptions.getWhereArgs(), "contact_id ASC"));
    }

    public JSONObject getContactById(String id) throws JSONException {
        return getContactById(id, null);
    }

    public JSONObject getContactById(String id, JSONArray desiredFields) throws JSONException {
        JSONArray contacts = populateContactArray(1, buildPopulationSet(new JSONObject().put("desiredFields", desiredFields)), this.mApp.getActivity().getContentResolver().query(Data.CONTENT_URI, null, "raw_contact_id = ? ", new String[]{id}, "raw_contact_id ASC"));
        if (contacts.length() == 1) {
            return contacts.getJSONObject(0);
        }
        return null;
    }

    private JSONArray populateContactArray(int limit, HashMap<String, Boolean> populate, Cursor c) {
        Throwable e;
        JSONArray addresses;
        JSONArray phones;
        String contactId = "";
        String rawId = "";
        String oldContactId = "";
        boolean newContact = true;
        String mimetype = "";
        JSONArray contacts = new JSONArray();
        JSONObject contact = new JSONObject();
        JSONArray organizations = new JSONArray();
        JSONArray addresses2 = new JSONArray();
        JSONArray phones2 = new JSONArray();
        JSONArray emails = new JSONArray();
        JSONArray ims = new JSONArray();
        JSONArray websites = new JSONArray();
        JSONArray photos = new JSONArray();
        int colContactId = c.getColumnIndex("contact_id");
        int colRawContactId = c.getColumnIndex("raw_contact_id");
        int colMimetype = c.getColumnIndex("mimetype");
        int colDisplayName = c.getColumnIndex("data1");
        int colNote = c.getColumnIndex("data1");
        int colNickname = c.getColumnIndex("data1");
        int colBirthday = c.getColumnIndex("data1");
        int colEventType = c.getColumnIndex("data2");
        if (c.getCount() > 0) {
            while (c.moveToNext() && contacts.length() <= limit - 1) {
                try {
                    contactId = c.getString(colContactId);
                    rawId = c.getString(colRawContactId);
                    if (c.getPosition() == 0) {
                        oldContactId = contactId;
                    }
                    if (!oldContactId.equals(contactId)) {
                        JSONArray organizations2;
                        JSONArray emails2;
                        contacts.put(populateContact(contact, organizations, addresses2, phones2, emails, ims, websites, photos));
                        JSONObject contact2 = new JSONObject();
                        try {
                            organizations2 = new JSONArray();
                        } catch (JSONException e2) {
                            e = e2;
                            contact = contact2;
                            Log.e("ContactsAccessor", e.getMessage(), e);
                            oldContactId = contactId;
                        }
                        try {
                            addresses = new JSONArray();
                            try {
                                phones = new JSONArray();
                                try {
                                    emails2 = new JSONArray();
                                } catch (JSONException e3) {
                                    e = e3;
                                    phones2 = phones;
                                    addresses2 = addresses;
                                    organizations = organizations2;
                                    contact = contact2;
                                    Log.e("ContactsAccessor", e.getMessage(), e);
                                    oldContactId = contactId;
                                }
                            } catch (JSONException e4) {
                                e = e4;
                                addresses2 = addresses;
                                organizations = organizations2;
                                contact = contact2;
                                Log.e("ContactsAccessor", e.getMessage(), e);
                                oldContactId = contactId;
                            }
                        } catch (JSONException e5) {
                            e = e5;
                            organizations = organizations2;
                            contact = contact2;
                            Log.e("ContactsAccessor", e.getMessage(), e);
                            oldContactId = contactId;
                        }
                        try {
                            JSONArray ims2 = new JSONArray();
                            try {
                                JSONArray websites2 = new JSONArray();
                                try {
                                    newContact = true;
                                    photos = new JSONArray();
                                    websites = websites2;
                                    ims = ims2;
                                    emails = emails2;
                                    phones2 = phones;
                                    addresses2 = addresses;
                                    organizations = organizations2;
                                    contact = contact2;
                                } catch (JSONException e6) {
                                    e = e6;
                                    websites = websites2;
                                    ims = ims2;
                                    emails = emails2;
                                    phones2 = phones;
                                    addresses2 = addresses;
                                    organizations = organizations2;
                                    contact = contact2;
                                    Log.e("ContactsAccessor", e.getMessage(), e);
                                    oldContactId = contactId;
                                }
                            } catch (JSONException e7) {
                                e = e7;
                                ims = ims2;
                                emails = emails2;
                                phones2 = phones;
                                addresses2 = addresses;
                                organizations = organizations2;
                                contact = contact2;
                                Log.e("ContactsAccessor", e.getMessage(), e);
                                oldContactId = contactId;
                            }
                        } catch (JSONException e8) {
                            e = e8;
                            emails = emails2;
                            phones2 = phones;
                            addresses2 = addresses;
                            organizations = organizations2;
                            contact = contact2;
                            Log.e("ContactsAccessor", e.getMessage(), e);
                            oldContactId = contactId;
                        }
                    }
                    if (newContact) {
                        newContact = false;
                        contact.put("id", contactId);
                        contact.put("rawId", rawId);
                    }
                    mimetype = c.getString(colMimetype);
                    if (mimetype.equals("vnd.android.cursor.item/name") && isRequired("name", populate)) {
                        contact.put("displayName", c.getString(colDisplayName));
                    }
                    if (mimetype.equals("vnd.android.cursor.item/name") && isRequired("name", populate)) {
                        contact.put("name", nameQuery(c));
                        oldContactId = contactId;
                    } else {
                        if (mimetype.equals("vnd.android.cursor.item/phone_v2") && isRequired("phoneNumbers", populate)) {
                            phones2.put(phoneQuery(c));
                            oldContactId = contactId;
                        } else {
                            if (mimetype.equals("vnd.android.cursor.item/email_v2") && isRequired("emails", populate)) {
                                emails.put(emailQuery(c));
                                oldContactId = contactId;
                            } else {
                                if (mimetype.equals("vnd.android.cursor.item/postal-address_v2") && isRequired("addresses", populate)) {
                                    addresses2.put(addressQuery(c));
                                    oldContactId = contactId;
                                } else {
                                    if (mimetype.equals("vnd.android.cursor.item/organization") && isRequired("organizations", populate)) {
                                        organizations.put(organizationQuery(c));
                                        oldContactId = contactId;
                                    } else {
                                        if (mimetype.equals("vnd.android.cursor.item/im") && isRequired("ims", populate)) {
                                            ims.put(imQuery(c));
                                            oldContactId = contactId;
                                        } else {
                                            if (mimetype.equals("vnd.android.cursor.item/note") && isRequired("note", populate)) {
                                                contact.put("note", c.getString(colNote));
                                                oldContactId = contactId;
                                            } else {
                                                if (mimetype.equals("vnd.android.cursor.item/nickname") && isRequired("nickname", populate)) {
                                                    contact.put("nickname", c.getString(colNickname));
                                                    oldContactId = contactId;
                                                } else {
                                                    if (mimetype.equals("vnd.android.cursor.item/website") && isRequired("urls", populate)) {
                                                        websites.put(websiteQuery(c));
                                                        oldContactId = contactId;
                                                    } else {
                                                        if (!mimetype.equals("vnd.android.cursor.item/contact_event")) {
                                                            if (mimetype.equals("vnd.android.cursor.item/photo") && isRequired("photos", populate)) {
                                                                JSONObject photo = photoQuery(c, contactId);
                                                                if (photo != null) {
                                                                    photos.put(photo);
                                                                }
                                                            }
                                                        } else if (isRequired("birthday", populate) && 3 == c.getInt(colEventType)) {
                                                            contact.put("birthday", c.getString(colBirthday));
                                                        }
                                                        oldContactId = contactId;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (JSONException e9) {
                    e = e9;
                    Log.e("ContactsAccessor", e.getMessage(), e);
                    oldContactId = contactId;
                }
            }
            if (contacts.length() < limit) {
                contacts.put(populateContact(contact, organizations, addresses2, phones2, emails, ims, websites, photos));
            }
        }
        c.close();
        return contacts;
    }

    private WhereOptions buildIdClause(Set<String> contactIds, String searchTerm) {
        WhereOptions options = new WhereOptions();
        if (searchTerm.equals("%")) {
            options.setWhere("(contact_id LIKE ? )");
            options.setWhereArgs(new String[]{searchTerm});
        } else {
            Iterator<String> it = contactIds.iterator();
            StringBuffer buffer = new StringBuffer("(");
            while (it.hasNext()) {
                buffer.append("'" + ((String) it.next()) + "'");
                if (it.hasNext()) {
                    buffer.append(",");
                }
            }
            buffer.append(")");
            options.setWhere("contact_id IN " + buffer.toString());
            options.setWhereArgs(null);
        }
        return options;
    }

    private JSONObject populateContact(JSONObject contact, JSONArray organizations, JSONArray addresses, JSONArray phones, JSONArray emails, JSONArray ims, JSONArray websites, JSONArray photos) {
        try {
            if (organizations.length() > 0) {
                contact.put("organizations", organizations);
            }
            if (addresses.length() > 0) {
                contact.put("addresses", addresses);
            }
            if (phones.length() > 0) {
                contact.put("phoneNumbers", phones);
            }
            if (emails.length() > 0) {
                contact.put("emails", emails);
            }
            if (ims.length() > 0) {
                contact.put("ims", ims);
            }
            if (websites.length() > 0) {
                contact.put("urls", websites);
            }
            if (photos.length() > 0) {
                contact.put("photos", photos);
            }
        } catch (JSONException e) {
            Log.e("ContactsAccessor", e.getMessage(), e);
        }
        return contact;
    }

    private WhereOptions buildWhereClause(JSONArray fields, String searchTerm) {
        ArrayList<String> where = new ArrayList();
        ArrayList<String> whereArgs = new ArrayList();
        WhereOptions options = new WhereOptions();
        if (isWildCardSearch(fields)) {
            if ("%".equals(searchTerm)) {
                options.setWhere("(display_name LIKE ? )");
                options.setWhereArgs(new String[]{searchTerm});
                return options;
            }
            where.add("(" + ((String) dbMap.get("displayName")) + " LIKE ? )");
            whereArgs.add(searchTerm);
            where.add("(" + ((String) dbMap.get("name")) + " LIKE ? AND " + "mimetype" + " = ? )");
            whereArgs.add(searchTerm);
            whereArgs.add("vnd.android.cursor.item/name");
            where.add("(" + ((String) dbMap.get("nickname")) + " LIKE ? AND " + "mimetype" + " = ? )");
            whereArgs.add(searchTerm);
            whereArgs.add("vnd.android.cursor.item/nickname");
            where.add("(" + ((String) dbMap.get("phoneNumbers")) + " LIKE ? AND " + "mimetype" + " = ? )");
            whereArgs.add(searchTerm);
            whereArgs.add("vnd.android.cursor.item/phone_v2");
            where.add("(" + ((String) dbMap.get("emails")) + " LIKE ? AND " + "mimetype" + " = ? )");
            whereArgs.add(searchTerm);
            whereArgs.add("vnd.android.cursor.item/email_v2");
            where.add("(" + ((String) dbMap.get("addresses")) + " LIKE ? AND " + "mimetype" + " = ? )");
            whereArgs.add(searchTerm);
            whereArgs.add("vnd.android.cursor.item/postal-address_v2");
            where.add("(" + ((String) dbMap.get("ims")) + " LIKE ? AND " + "mimetype" + " = ? )");
            whereArgs.add(searchTerm);
            whereArgs.add("vnd.android.cursor.item/im");
            where.add("(" + ((String) dbMap.get("organizations")) + " LIKE ? AND " + "mimetype" + " = ? )");
            whereArgs.add(searchTerm);
            whereArgs.add("vnd.android.cursor.item/organization");
            where.add("(" + ((String) dbMap.get("note")) + " LIKE ? AND " + "mimetype" + " = ? )");
            whereArgs.add(searchTerm);
            whereArgs.add("vnd.android.cursor.item/note");
            where.add("(" + ((String) dbMap.get("urls")) + " LIKE ? AND " + "mimetype" + " = ? )");
            whereArgs.add(searchTerm);
            whereArgs.add("vnd.android.cursor.item/website");
        }
        if ("%".equals(searchTerm)) {
            options.setWhere("(display_name LIKE ? )");
            options.setWhereArgs(new String[]{searchTerm});
        } else {
            int i = 0;
            while (i < fields.length()) {
                try {
                    String key = fields.getString(i);
                    if (key.equals("id")) {
                        where.add("(" + ((String) dbMap.get(key)) + " = ? )");
                        whereArgs.add(searchTerm.substring(1, searchTerm.length() - 1));
                    } else if (key.startsWith("displayName")) {
                        where.add("(" + ((String) dbMap.get(key)) + " LIKE ? )");
                        whereArgs.add(searchTerm);
                    } else if (key.startsWith("name")) {
                        where.add("(" + ((String) dbMap.get(key)) + " LIKE ? AND " + "mimetype" + " = ? )");
                        whereArgs.add(searchTerm);
                        whereArgs.add("vnd.android.cursor.item/name");
                    } else if (key.startsWith("nickname")) {
                        where.add("(" + ((String) dbMap.get(key)) + " LIKE ? AND " + "mimetype" + " = ? )");
                        whereArgs.add(searchTerm);
                        whereArgs.add("vnd.android.cursor.item/nickname");
                    } else if (key.startsWith("phoneNumbers")) {
                        where.add("(" + ((String) dbMap.get(key)) + " LIKE ? AND " + "mimetype" + " = ? )");
                        whereArgs.add(searchTerm);
                        whereArgs.add("vnd.android.cursor.item/phone_v2");
                    } else if (key.startsWith("emails")) {
                        where.add("(" + ((String) dbMap.get(key)) + " LIKE ? AND " + "mimetype" + " = ? )");
                        whereArgs.add(searchTerm);
                        whereArgs.add("vnd.android.cursor.item/email_v2");
                    } else if (key.startsWith("addresses")) {
                        where.add("(" + ((String) dbMap.get(key)) + " LIKE ? AND " + "mimetype" + " = ? )");
                        whereArgs.add(searchTerm);
                        whereArgs.add("vnd.android.cursor.item/postal-address_v2");
                    } else if (key.startsWith("ims")) {
                        where.add("(" + ((String) dbMap.get(key)) + " LIKE ? AND " + "mimetype" + " = ? )");
                        whereArgs.add(searchTerm);
                        whereArgs.add("vnd.android.cursor.item/im");
                    } else if (key.startsWith("organizations")) {
                        where.add("(" + ((String) dbMap.get(key)) + " LIKE ? AND " + "mimetype" + " = ? )");
                        whereArgs.add(searchTerm);
                        whereArgs.add("vnd.android.cursor.item/organization");
                    } else if (key.startsWith("note")) {
                        where.add("(" + ((String) dbMap.get(key)) + " LIKE ? AND " + "mimetype" + " = ? )");
                        whereArgs.add(searchTerm);
                        whereArgs.add("vnd.android.cursor.item/note");
                    } else if (key.startsWith("urls")) {
                        where.add("(" + ((String) dbMap.get(key)) + " LIKE ? AND " + "mimetype" + " = ? )");
                        whereArgs.add(searchTerm);
                        whereArgs.add("vnd.android.cursor.item/website");
                    }
                    i++;
                } catch (JSONException e) {
                    Log.e("ContactsAccessor", e.getMessage(), e);
                }
            }
            StringBuffer selection = new StringBuffer();
            for (i = 0; i < where.size(); i++) {
                selection.append((String) where.get(i));
                if (i != where.size() - 1) {
                    selection.append(" OR ");
                }
            }
            options.setWhere(selection.toString());
            String[] selectionArgs = new String[whereArgs.size()];
            for (i = 0; i < whereArgs.size(); i++) {
                selectionArgs[i] = (String) whereArgs.get(i);
            }
            options.setWhereArgs(selectionArgs);
        }
        return options;
    }

    private boolean isWildCardSearch(JSONArray fields) {
        if (fields.length() == 1) {
            try {
                if ("*".equals(fields.getString(0))) {
                    return true;
                }
            } catch (JSONException e) {
                return false;
            }
        }
        return false;
    }

    private JSONObject organizationQuery(Cursor cursor) {
        JSONObject organization = new JSONObject();
        try {
            organization.put("id", cursor.getString(cursor.getColumnIndex("_id")));
            organization.put("pref", false);
            organization.put("type", getOrgType(cursor.getInt(cursor.getColumnIndex("data2"))));
            organization.put("department", cursor.getString(cursor.getColumnIndex("data5")));
            organization.put("name", cursor.getString(cursor.getColumnIndex("data1")));
            organization.put("title", cursor.getString(cursor.getColumnIndex("data4")));
        } catch (JSONException e) {
            Log.e("ContactsAccessor", e.getMessage(), e);
        }
        return organization;
    }

    private JSONObject addressQuery(Cursor cursor) {
        JSONObject address = new JSONObject();
        try {
            address.put("id", cursor.getString(cursor.getColumnIndex("_id")));
            address.put("pref", false);
            address.put("type", getAddressType(cursor.getInt(cursor.getColumnIndex("data2"))));
            address.put("formatted", cursor.getString(cursor.getColumnIndex("data1")));
            address.put("streetAddress", cursor.getString(cursor.getColumnIndex("data4")));
            address.put("locality", cursor.getString(cursor.getColumnIndex("data7")));
            address.put("region", cursor.getString(cursor.getColumnIndex("data8")));
            address.put("postalCode", cursor.getString(cursor.getColumnIndex("data9")));
            address.put("country", cursor.getString(cursor.getColumnIndex("data10")));
        } catch (JSONException e) {
            Log.e("ContactsAccessor", e.getMessage(), e);
        }
        return address;
    }

    private JSONObject nameQuery(Cursor cursor) {
        JSONObject contactName = new JSONObject();
        try {
            String familyName = cursor.getString(cursor.getColumnIndex("data3"));
            String givenName = cursor.getString(cursor.getColumnIndex("data2"));
            String middleName = cursor.getString(cursor.getColumnIndex("data5"));
            String honorificPrefix = cursor.getString(cursor.getColumnIndex("data4"));
            String honorificSuffix = cursor.getString(cursor.getColumnIndex("data6"));
            StringBuffer formatted = new StringBuffer("");
            if (honorificPrefix != null) {
                formatted.append(honorificPrefix + " ");
            }
            if (givenName != null) {
                formatted.append(givenName + " ");
            }
            if (middleName != null) {
                formatted.append(middleName + " ");
            }
            if (familyName != null) {
                formatted.append(familyName);
            }
            if (honorificSuffix != null) {
                formatted.append(" " + honorificSuffix);
            }
            contactName.put("familyName", familyName);
            contactName.put("givenName", givenName);
            contactName.put("middleName", middleName);
            contactName.put("honorificPrefix", honorificPrefix);
            contactName.put("honorificSuffix", honorificSuffix);
            contactName.put("formatted", formatted);
        } catch (JSONException e) {
            Log.e("ContactsAccessor", e.getMessage(), e);
        }
        return contactName;
    }

    private JSONObject phoneQuery(Cursor cursor) {
        JSONObject phoneNumber = new JSONObject();
        try {
            phoneNumber.put("id", cursor.getString(cursor.getColumnIndex("_id")));
            phoneNumber.put("pref", false);
            phoneNumber.put("value", cursor.getString(cursor.getColumnIndex("data1")));
            phoneNumber.put("type", getPhoneType(cursor.getInt(cursor.getColumnIndex("data2"))));
        } catch (JSONException e) {
            Log.e("ContactsAccessor", e.getMessage(), e);
        } catch (Exception excp) {
            Log.e("ContactsAccessor", excp.getMessage(), excp);
        }
        return phoneNumber;
    }

    private JSONObject emailQuery(Cursor cursor) {
        JSONObject email = new JSONObject();
        try {
            email.put("id", cursor.getString(cursor.getColumnIndex("_id")));
            email.put("pref", false);
            email.put("value", cursor.getString(cursor.getColumnIndex("data1")));
            email.put("type", getContactType(cursor.getInt(cursor.getColumnIndex("data2"))));
        } catch (JSONException e) {
            Log.e("ContactsAccessor", e.getMessage(), e);
        }
        return email;
    }

    private JSONObject imQuery(Cursor cursor) {
        JSONObject im = new JSONObject();
        try {
            im.put("id", cursor.getString(cursor.getColumnIndex("_id")));
            im.put("pref", false);
            im.put("value", cursor.getString(cursor.getColumnIndex("data1")));
            im.put("type", getImType(cursor.getString(cursor.getColumnIndex("data5"))));
        } catch (JSONException e) {
            Log.e("ContactsAccessor", e.getMessage(), e);
        }
        return im;
    }

    private JSONObject websiteQuery(Cursor cursor) {
        JSONObject website = new JSONObject();
        try {
            website.put("id", cursor.getString(cursor.getColumnIndex("_id")));
            website.put("pref", false);
            website.put("value", cursor.getString(cursor.getColumnIndex("data1")));
            website.put("type", getContactType(cursor.getInt(cursor.getColumnIndex("data2"))));
        } catch (JSONException e) {
            Log.e("ContactsAccessor", e.getMessage(), e);
        }
        return website;
    }

    private JSONObject photoQuery(Cursor cursor, String contactId) {
        JSONObject photo = new JSONObject();
        try {
            photo.put("id", cursor.getString(cursor.getColumnIndex("_id")));
            photo.put("pref", false);
            photo.put("type", "url");
            Uri photoUri = Uri.withAppendedPath(ContentUris.withAppendedId(Contacts.CONTENT_URI, Long.valueOf(contactId).longValue()), "photo");
            photo.put("value", photoUri.toString());
            Cursor photoCursor = this.mApp.getActivity().getContentResolver().query(photoUri, new String[]{"data15"}, null, null, null);
            if (photoCursor == null) {
                return null;
            }
            if (photoCursor.moveToFirst()) {
                return photo;
            }
            photoCursor.close();
            return null;
        } catch (JSONException e) {
            Log.e("ContactsAccessor", e.getMessage(), e);
            return photo;
        }
    }

    public String save(JSONObject contact) {
        Account[] accounts = AccountManager.get(this.mApp.getActivity()).getAccounts();
        String accountName = null;
        String accountType = null;
        if (accounts.length == 1) {
            accountName = accounts[0].name;
            accountType = accounts[0].type;
        } else if (accounts.length > 1) {
            for (Account a : accounts) {
                if (a.type.contains("eas") && a.name.matches(EMAIL_REGEXP)) {
                    accountName = a.name;
                    accountType = a.type;
                    break;
                }
            }
            if (accountName == null) {
                for (Account a2 : accounts) {
                    if (a2.type.contains("com.google") && a2.name.matches(EMAIL_REGEXP)) {
                        accountName = a2.name;
                        accountType = a2.type;
                        break;
                    }
                }
            }
            if (accountName == null) {
                for (Account a22 : accounts) {
                    if (a22.name.matches(EMAIL_REGEXP)) {
                        accountName = a22.name;
                        accountType = a22.type;
                        break;
                    }
                }
            }
        }
        String id = getJsonString(contact, "id");
        if (id == null) {
            return createNewContact(contact, accountType, accountName);
        }
        return modifyContact(id, contact, accountType, accountName);
    }

    private String modifyContact(String id, JSONObject contact, String accountType, String accountName) {
        int i;
        int rawId = Integer.valueOf(getJsonString(contact, "rawId")).intValue();
        ArrayList<ContentProviderOperation> ops = new ArrayList();
        ops.add(ContentProviderOperation.newUpdate(RawContacts.CONTENT_URI).withValue("account_type", accountType).withValue("account_name", accountName).build());
        try {
            String displayName = getJsonString(contact, "displayName");
            JSONObject name = contact.getJSONObject("name");
            if (!(displayName == null && name == null)) {
                Builder builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("contact_id=? AND mimetype=?", new String[]{id, "vnd.android.cursor.item/name"});
                if (displayName != null) {
                    builder.withValue("data1", displayName);
                }
                String familyName = getJsonString(name, "familyName");
                if (familyName != null) {
                    builder.withValue("data3", familyName);
                }
                String middleName = getJsonString(name, "middleName");
                if (middleName != null) {
                    builder.withValue("data5", middleName);
                }
                String givenName = getJsonString(name, "givenName");
                if (givenName != null) {
                    builder.withValue("data2", givenName);
                }
                String honorificPrefix = getJsonString(name, "honorificPrefix");
                if (honorificPrefix != null) {
                    builder.withValue("data4", honorificPrefix);
                }
                String honorificSuffix = getJsonString(name, "honorificSuffix");
                if (honorificSuffix != null) {
                    builder.withValue("data6", honorificSuffix);
                }
                ops.add(builder.build());
            }
        } catch (JSONException e) {
            Log.d("ContactsAccessor", "Could not get name");
        }
        try {
            JSONArray phones = contact.getJSONArray("phoneNumbers");
            if (phones != null) {
                if (phones.length() == 0) {
                    r46 = new String[2];
                    r46[0] = "" + rawId;
                    r46[1] = "vnd.android.cursor.item/phone_v2";
                    ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection("raw_contact_id=? AND mimetype=?", r46).build());
                } else {
                    for (i = 0; i < phones.length(); i++) {
                        JSONObject phone = (JSONObject) phones.get(i);
                        if (getJsonString(phone, "id") == null) {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("raw_contact_id", Integer.valueOf(rawId));
                            contentValues.put("mimetype", "vnd.android.cursor.item/phone_v2");
                            contentValues.put("data1", getJsonString(phone, "value"));
                            contentValues.put("data2", Integer.valueOf(getPhoneType(getJsonString(phone, "type"))));
                            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValues(contentValues).build());
                        } else {
                            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("_id=? AND mimetype=?", new String[]{getJsonString(phone, "id"), "vnd.android.cursor.item/phone_v2"}).withValue("data1", getJsonString(phone, "value")).withValue("data2", Integer.valueOf(getPhoneType(getJsonString(phone, "type")))).build());
                        }
                    }
                }
            }
        } catch (JSONException e2) {
            Log.d("ContactsAccessor", "Could not get phone numbers");
        }
        JSONArray emails = contact.getJSONArray("emails");
        if (emails != null) {
            if (emails.length() == 0) {
                r46 = new String[2];
                r46[0] = "" + rawId;
                r46[1] = "vnd.android.cursor.item/email_v2";
                ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection("raw_contact_id=? AND mimetype=?", r46).build());
            } else {
                for (i = 0; i < emails.length(); i++) {
                    JSONObject email = (JSONObject) emails.get(i);
                    if (getJsonString(email, "id") == null) {
                        contentValues = new ContentValues();
                        contentValues.put("raw_contact_id", Integer.valueOf(rawId));
                        contentValues.put("mimetype", "vnd.android.cursor.item/email_v2");
                        contentValues.put("data1", getJsonString(email, "value"));
                        contentValues.put("data2", Integer.valueOf(getContactType(getJsonString(email, "type"))));
                        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValues(contentValues).build());
                    } else {
                        if (getJsonString(email, "value").isEmpty()) {
                            try {
                                ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection("_id=? AND mimetype=?", new String[]{emailId, "vnd.android.cursor.item/email_v2"}).build());
                            } catch (JSONException e3) {
                                Log.d("ContactsAccessor", "Could not get emails");
                            }
                        } else {
                            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("_id=? AND mimetype=?", new String[]{emailId, "vnd.android.cursor.item/email_v2"}).withValue("data1", getJsonString(email, "value")).withValue("data2", Integer.valueOf(getContactType(getJsonString(email, "type")))).build());
                        }
                    }
                }
            }
        }
        try {
            JSONArray addresses = contact.getJSONArray("addresses");
            if (addresses != null) {
                if (addresses.length() == 0) {
                    r46 = new String[2];
                    r46[0] = "" + rawId;
                    r46[1] = "vnd.android.cursor.item/postal-address_v2";
                    ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection("raw_contact_id=? AND mimetype=?", r46).build());
                } else {
                    for (i = 0; i < addresses.length(); i++) {
                        JSONObject address = (JSONObject) addresses.get(i);
                        if (getJsonString(address, "id") == null) {
                            contentValues = new ContentValues();
                            contentValues.put("raw_contact_id", Integer.valueOf(rawId));
                            contentValues.put("mimetype", "vnd.android.cursor.item/postal-address_v2");
                            contentValues.put("data2", Integer.valueOf(getAddressType(getJsonString(address, "type"))));
                            contentValues.put("data1", getJsonString(address, "formatted"));
                            contentValues.put("data4", getJsonString(address, "streetAddress"));
                            contentValues.put("data7", getJsonString(address, "locality"));
                            contentValues.put("data8", getJsonString(address, "region"));
                            contentValues.put("data9", getJsonString(address, "postalCode"));
                            contentValues.put("data10", getJsonString(address, "country"));
                            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValues(contentValues).build());
                        } else {
                            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("_id=? AND mimetype=?", new String[]{getJsonString(address, "id"), "vnd.android.cursor.item/postal-address_v2"}).withValue("data2", Integer.valueOf(getAddressType(getJsonString(address, "type")))).withValue("data1", getJsonString(address, "formatted")).withValue("data4", getJsonString(address, "streetAddress")).withValue("data7", getJsonString(address, "locality")).withValue("data8", getJsonString(address, "region")).withValue("data9", getJsonString(address, "postalCode")).withValue("data10", getJsonString(address, "country")).build());
                        }
                    }
                }
            }
        } catch (JSONException e4) {
            Log.d("ContactsAccessor", "Could not get addresses");
        }
        try {
            JSONArray organizations = contact.getJSONArray("organizations");
            if (organizations != null) {
                if (organizations.length() == 0) {
                    r46 = new String[2];
                    r46[0] = "" + rawId;
                    r46[1] = "vnd.android.cursor.item/organization";
                    ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection("raw_contact_id=? AND mimetype=?", r46).build());
                } else {
                    for (i = 0; i < organizations.length(); i++) {
                        JSONObject org = (JSONObject) organizations.get(i);
                        if (getJsonString(org, "id") == null) {
                            contentValues = new ContentValues();
                            contentValues.put("raw_contact_id", Integer.valueOf(rawId));
                            contentValues.put("mimetype", "vnd.android.cursor.item/organization");
                            contentValues.put("data2", Integer.valueOf(getOrgType(getJsonString(org, "type"))));
                            contentValues.put("data5", getJsonString(org, "department"));
                            contentValues.put("data1", getJsonString(org, "name"));
                            contentValues.put("data4", getJsonString(org, "title"));
                            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValues(contentValues).build());
                        } else {
                            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("_id=? AND mimetype=?", new String[]{getJsonString(org, "id"), "vnd.android.cursor.item/organization"}).withValue("data2", Integer.valueOf(getOrgType(getJsonString(org, "type")))).withValue("data5", getJsonString(org, "department")).withValue("data1", getJsonString(org, "name")).withValue("data4", getJsonString(org, "title")).build());
                        }
                    }
                }
            }
        } catch (JSONException e5) {
            Log.d("ContactsAccessor", "Could not get organizations");
        }
        try {
            JSONArray ims = contact.getJSONArray("ims");
            if (ims != null) {
                if (ims.length() == 0) {
                    r46 = new String[2];
                    r46[0] = "" + rawId;
                    r46[1] = "vnd.android.cursor.item/im";
                    ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection("raw_contact_id=? AND mimetype=?", r46).build());
                } else {
                    for (i = 0; i < ims.length(); i++) {
                        JSONObject im = (JSONObject) ims.get(i);
                        if (getJsonString(im, "id") == null) {
                            contentValues = new ContentValues();
                            contentValues.put("raw_contact_id", Integer.valueOf(rawId));
                            contentValues.put("mimetype", "vnd.android.cursor.item/im");
                            contentValues.put("data1", getJsonString(im, "value"));
                            contentValues.put("data2", Integer.valueOf(getImType(getJsonString(im, "type"))));
                            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValues(contentValues).build());
                        } else {
                            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("_id=? AND mimetype=?", new String[]{getJsonString(im, "id"), "vnd.android.cursor.item/im"}).withValue("data1", getJsonString(im, "value")).withValue("data2", Integer.valueOf(getContactType(getJsonString(im, "type")))).build());
                        }
                    }
                }
            }
        } catch (JSONException e6) {
            Log.d("ContactsAccessor", "Could not get emails");
        }
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("contact_id=? AND mimetype=?", new String[]{id, "vnd.android.cursor.item/note"}).withValue("data1", getJsonString(contact, "note")).build());
        String nickname = getJsonString(contact, "nickname");
        if (nickname != null) {
            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("contact_id=? AND mimetype=?", new String[]{id, "vnd.android.cursor.item/nickname"}).withValue("data1", nickname).build());
        }
        try {
            JSONArray websites = contact.getJSONArray("urls");
            if (websites != null) {
                if (websites.length() == 0) {
                    Log.d("ContactsAccessor", "This means we should be deleting all the phone numbers.");
                    r46 = new String[2];
                    r46[0] = "" + rawId;
                    r46[1] = "vnd.android.cursor.item/website";
                    ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection("raw_contact_id=? AND mimetype=?", r46).build());
                } else {
                    for (i = 0; i < websites.length(); i++) {
                        JSONObject website = (JSONObject) websites.get(i);
                        if (getJsonString(website, "id") == null) {
                            contentValues = new ContentValues();
                            contentValues.put("raw_contact_id", Integer.valueOf(rawId));
                            contentValues.put("mimetype", "vnd.android.cursor.item/website");
                            contentValues.put("data1", getJsonString(website, "value"));
                            contentValues.put("data2", Integer.valueOf(getContactType(getJsonString(website, "type"))));
                            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValues(contentValues).build());
                        } else {
                            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("_id=? AND mimetype=?", new String[]{getJsonString(website, "id"), "vnd.android.cursor.item/website"}).withValue("data1", getJsonString(website, "value")).withValue("data2", Integer.valueOf(getContactType(getJsonString(website, "type")))).build());
                        }
                    }
                }
            }
        } catch (JSONException e7) {
            Log.d("ContactsAccessor", "Could not get websites");
        }
        String birthday = getJsonString(contact, "birthday");
        if (birthday != null) {
            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("contact_id=? AND mimetype=? AND data2=?", new String[]{id, "vnd.android.cursor.item/contact_event", new String("3")}).withValue("data2", Integer.valueOf(3)).withValue("data1", birthday).build());
        }
        try {
            JSONArray photos = contact.getJSONArray("photos");
            if (photos != null) {
                if (photos.length() == 0) {
                    r46 = new String[2];
                    r46[0] = "" + rawId;
                    r46[1] = "vnd.android.cursor.item/photo";
                    ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection("raw_contact_id=? AND mimetype=?", r46).build());
                } else {
                    for (i = 0; i < photos.length(); i++) {
                        JSONObject photo = (JSONObject) photos.get(i);
                        String photoId = getJsonString(photo, "id");
                        byte[] bytes = getPhotoBytes(getJsonString(photo, "value"));
                        if (photoId == null) {
                            contentValues = new ContentValues();
                            contentValues.put("raw_contact_id", Integer.valueOf(rawId));
                            contentValues.put("mimetype", "vnd.android.cursor.item/photo");
                            contentValues.put("is_super_primary", Integer.valueOf(1));
                            contentValues.put("data15", bytes);
                            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValues(contentValues).build());
                        } else {
                            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection("_id=? AND mimetype=?", new String[]{photoId, "vnd.android.cursor.item/photo"}).withValue("is_super_primary", Integer.valueOf(1)).withValue("data15", bytes).build());
                        }
                    }
                }
            }
        } catch (JSONException e8) {
            Log.d("ContactsAccessor", "Could not get photos");
        }
        boolean retVal = true;
        try {
            this.mApp.getActivity().getContentResolver().applyBatch("com.android.contacts", ops);
        } catch (RemoteException e9) {
            Log.e("ContactsAccessor", e9.getMessage(), e9);
            Log.e("ContactsAccessor", Log.getStackTraceString(e9), e9);
            retVal = false;
        } catch (OperationApplicationException e10) {
            Log.e("ContactsAccessor", e10.getMessage(), e10);
            Log.e("ContactsAccessor", Log.getStackTraceString(e10), e10);
            retVal = false;
        }
        return retVal ? id : null;
    }

    private void insertWebsite(ArrayList<ContentProviderOperation> ops, JSONObject website) {
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("mimetype", "vnd.android.cursor.item/website").withValue("data1", getJsonString(website, "value")).withValue("data2", Integer.valueOf(getContactType(getJsonString(website, "type")))).build());
    }

    private void insertIm(ArrayList<ContentProviderOperation> ops, JSONObject im) {
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("mimetype", "vnd.android.cursor.item/im").withValue("data1", getJsonString(im, "value")).withValue("data2", Integer.valueOf(getImType(getJsonString(im, "type")))).build());
    }

    private void insertOrganization(ArrayList<ContentProviderOperation> ops, JSONObject org) {
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("mimetype", "vnd.android.cursor.item/organization").withValue("data2", Integer.valueOf(getOrgType(getJsonString(org, "type")))).withValue("data5", getJsonString(org, "department")).withValue("data1", getJsonString(org, "name")).withValue("data4", getJsonString(org, "title")).build());
    }

    private void insertAddress(ArrayList<ContentProviderOperation> ops, JSONObject address) {
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("mimetype", "vnd.android.cursor.item/postal-address_v2").withValue("data2", Integer.valueOf(getAddressType(getJsonString(address, "type")))).withValue("data1", getJsonString(address, "formatted")).withValue("data4", getJsonString(address, "streetAddress")).withValue("data7", getJsonString(address, "locality")).withValue("data8", getJsonString(address, "region")).withValue("data9", getJsonString(address, "postalCode")).withValue("data10", getJsonString(address, "country")).build());
    }

    private void insertEmail(ArrayList<ContentProviderOperation> ops, JSONObject email) {
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("mimetype", "vnd.android.cursor.item/email_v2").withValue("data1", getJsonString(email, "value")).withValue("data2", Integer.valueOf(getContactType(getJsonString(email, "type")))).build());
    }

    private void insertPhone(ArrayList<ContentProviderOperation> ops, JSONObject phone) {
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("mimetype", "vnd.android.cursor.item/phone_v2").withValue("data1", getJsonString(phone, "value")).withValue("data2", Integer.valueOf(getPhoneType(getJsonString(phone, "type")))).build());
    }

    private void insertPhoto(ArrayList<ContentProviderOperation> ops, JSONObject photo) {
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("is_super_primary", Integer.valueOf(1)).withValue("mimetype", "vnd.android.cursor.item/photo").withValue("data15", getPhotoBytes(getJsonString(photo, "value"))).build());
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private byte[] getPhotoBytes(java.lang.String r11) {
        /*
        r10 = this;
        r0 = new java.io.ByteArrayOutputStream;
        r0.<init>();
        r1 = 0;
        r6 = 0;
        r5 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;
        r2 = new byte[r5];	 Catch:{ FileNotFoundException -> 0x0032, IOException -> 0x003d }
        r4 = r10.getPathFromUri(r11);	 Catch:{ FileNotFoundException -> 0x0032, IOException -> 0x003d }
    L_0x0010:
        r5 = 0;
        r8 = r2.length;	 Catch:{ FileNotFoundException -> 0x0032, IOException -> 0x003d }
        r1 = r4.read(r2, r5, r8);	 Catch:{ FileNotFoundException -> 0x0032, IOException -> 0x003d }
        r5 = -1;
        if (r1 == r5) goto L_0x0027;
    L_0x0019:
        r8 = 1048576; // 0x100000 float:1.469368E-39 double:5.180654E-318;
        r5 = (r6 > r8 ? 1 : (r6 == r8 ? 0 : -1));
        if (r5 > 0) goto L_0x0027;
    L_0x0020:
        r5 = 0;
        r0.write(r2, r5, r1);	 Catch:{ FileNotFoundException -> 0x0032, IOException -> 0x003d }
        r8 = (long) r1;	 Catch:{ FileNotFoundException -> 0x0032, IOException -> 0x003d }
        r6 = r6 + r8;
        goto L_0x0010;
    L_0x0027:
        r4.close();	 Catch:{ FileNotFoundException -> 0x0032, IOException -> 0x003d }
        r0.flush();	 Catch:{ FileNotFoundException -> 0x0032, IOException -> 0x003d }
    L_0x002d:
        r5 = r0.toByteArray();
        return r5;
    L_0x0032:
        r3 = move-exception;
        r5 = "ContactsAccessor";
        r8 = r3.getMessage();
        android.util.Log.e(r5, r8, r3);
        goto L_0x002d;
    L_0x003d:
        r3 = move-exception;
        r5 = "ContactsAccessor";
        r8 = r3.getMessage();
        android.util.Log.e(r5, r8, r3);
        goto L_0x002d;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.apache.cordova.contacts.ContactAccessorSdk5.getPhotoBytes(java.lang.String):byte[]");
    }

    private InputStream getPathFromUri(String path) throws IOException {
        if (path.startsWith("content:")) {
            return this.mApp.getActivity().getContentResolver().openInputStream(Uri.parse(path));
        } else if (path.startsWith("http:") || path.startsWith("https:") || path.startsWith("file:")) {
            return new URL(path).openStream();
        } else {
            return new FileInputStream(path);
        }
    }

    private String createNewContact(JSONObject contact, String accountType, String accountName) {
        int i;
        ArrayList<ContentProviderOperation> ops = new ArrayList();
        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValue("account_type", accountType).withValue("account_name", accountName).build());
        try {
            JSONObject name = contact.optJSONObject("name");
            String displayName = contact.getString("displayName");
            if (!(displayName == null && name == null)) {
                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("mimetype", "vnd.android.cursor.item/name").withValue("data1", displayName).withValue("data3", getJsonString(name, "familyName")).withValue("data5", getJsonString(name, "middleName")).withValue("data2", getJsonString(name, "givenName")).withValue("data4", getJsonString(name, "honorificPrefix")).withValue("data6", getJsonString(name, "honorificSuffix")).build());
            }
        } catch (JSONException e) {
            Log.d("ContactsAccessor", "Could not get name object");
        }
        try {
            JSONArray phones = contact.getJSONArray("phoneNumbers");
            if (phones != null) {
                for (i = 0; i < phones.length(); i++) {
                    insertPhone(ops, (JSONObject) phones.get(i));
                }
            }
        } catch (JSONException e2) {
            Log.d("ContactsAccessor", "Could not get phone numbers");
        }
        try {
            JSONArray emails = contact.getJSONArray("emails");
            if (emails != null) {
                for (i = 0; i < emails.length(); i++) {
                    insertEmail(ops, (JSONObject) emails.get(i));
                }
            }
        } catch (JSONException e3) {
            Log.d("ContactsAccessor", "Could not get emails");
        }
        try {
            JSONArray addresses = contact.getJSONArray("addresses");
            if (addresses != null) {
                for (i = 0; i < addresses.length(); i++) {
                    insertAddress(ops, (JSONObject) addresses.get(i));
                }
            }
        } catch (JSONException e4) {
            Log.d("ContactsAccessor", "Could not get addresses");
        }
        try {
            JSONArray organizations = contact.getJSONArray("organizations");
            if (organizations != null) {
                for (i = 0; i < organizations.length(); i++) {
                    insertOrganization(ops, (JSONObject) organizations.get(i));
                }
            }
        } catch (JSONException e5) {
            Log.d("ContactsAccessor", "Could not get organizations");
        }
        try {
            JSONArray ims = contact.getJSONArray("ims");
            if (ims != null) {
                for (i = 0; i < ims.length(); i++) {
                    insertIm(ops, (JSONObject) ims.get(i));
                }
            }
        } catch (JSONException e6) {
            Log.d("ContactsAccessor", "Could not get emails");
        }
        String note = getJsonString(contact, "note");
        if (note != null) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("mimetype", "vnd.android.cursor.item/note").withValue("data1", note).build());
        }
        String nickname = getJsonString(contact, "nickname");
        if (nickname != null) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("mimetype", "vnd.android.cursor.item/nickname").withValue("data1", nickname).build());
        }
        try {
            JSONArray websites = contact.getJSONArray("urls");
            if (websites != null) {
                for (i = 0; i < websites.length(); i++) {
                    insertWebsite(ops, (JSONObject) websites.get(i));
                }
            }
        } catch (JSONException e7) {
            Log.d("ContactsAccessor", "Could not get websites");
        }
        String birthday = getJsonString(contact, "birthday");
        if (birthday != null) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValue("mimetype", "vnd.android.cursor.item/contact_event").withValue("data2", Integer.valueOf(3)).withValue("data1", birthday).build());
        }
        try {
            JSONArray photos = contact.getJSONArray("photos");
            if (photos != null) {
                for (i = 0; i < photos.length(); i++) {
                    insertPhoto(ops, (JSONObject) photos.get(i));
                }
            }
        } catch (JSONException e8) {
            Log.d("ContactsAccessor", "Could not get photos");
        }
        String newId = null;
        try {
            ContentProviderResult[] cpResults = this.mApp.getActivity().getContentResolver().applyBatch("com.android.contacts", ops);
            if (cpResults.length >= 0) {
                newId = cpResults[0].uri.getLastPathSegment();
            }
        } catch (RemoteException e9) {
            Log.e("ContactsAccessor", e9.getMessage(), e9);
        } catch (OperationApplicationException e10) {
            Log.e("ContactsAccessor", e10.getMessage(), e10);
        }
        return newId;
    }

    public boolean remove(String id) {
        int result = 0;
        Cursor cursor = this.mApp.getActivity().getContentResolver().query(Contacts.CONTENT_URI, null, "_id = ?", new String[]{id}, null);
        if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            result = this.mApp.getActivity().getContentResolver().delete(Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, cursor.getString(cursor.getColumnIndex("lookup"))), null, null);
        } else {
            Log.d("ContactsAccessor", "Could not find contact with ID");
        }
        if (result > 0) {
            return true;
        }
        return false;
    }

    private int getPhoneType(String string) {
        if (string == null) {
            return 7;
        }
        if ("home".equals(string.toLowerCase())) {
            return 1;
        }
        if (NetworkManager.MOBILE.equals(string.toLowerCase())) {
            return 2;
        }
        if ("work".equals(string.toLowerCase())) {
            return 3;
        }
        if ("work fax".equals(string.toLowerCase())) {
            return 4;
        }
        if ("home fax".equals(string.toLowerCase())) {
            return 5;
        }
        if ("fax".equals(string.toLowerCase())) {
            return 4;
        }
        if ("pager".equals(string.toLowerCase())) {
            return 6;
        }
        if ("other".equals(string.toLowerCase())) {
            return 7;
        }
        if ("car".equals(string.toLowerCase())) {
            return 9;
        }
        if ("company main".equals(string.toLowerCase())) {
            return 10;
        }
        if ("isdn".equals(string.toLowerCase())) {
            return 11;
        }
        if ("main".equals(string.toLowerCase())) {
            return 12;
        }
        if ("other fax".equals(string.toLowerCase())) {
            return 13;
        }
        if ("radio".equals(string.toLowerCase())) {
            return 14;
        }
        if ("telex".equals(string.toLowerCase())) {
            return 15;
        }
        if ("work mobile".equals(string.toLowerCase())) {
            return 17;
        }
        if ("work pager".equals(string.toLowerCase())) {
            return 18;
        }
        if ("assistant".equals(string.toLowerCase())) {
            return 19;
        }
        if ("mms".equals(string.toLowerCase())) {
            return 20;
        }
        if ("callback".equals(string.toLowerCase())) {
            return 8;
        }
        if ("tty ttd".equals(string.toLowerCase())) {
            return 16;
        }
        if ("custom".equals(string.toLowerCase())) {
            return 0;
        }
        return 7;
    }

    private String getPhoneType(int type) {
        switch (type) {
            case CordovaResourceApi.URI_TYPE_FILE /*0*/:
                return "custom";
            case ContactManager.INVALID_ARGUMENT_ERROR /*1*/:
                return "home";
            case ContactManager.TIMEOUT_ERROR /*2*/:
                return NetworkManager.MOBILE;
            case ContactManager.PENDING_OPERATION_ERROR /*3*/:
                return "work";
            case ContactManager.IO_ERROR /*4*/:
                return "work fax";
            case ContactManager.NOT_SUPPORTED_ERROR /*5*/:
                return "home fax";
            case PluginResult.MESSAGE_TYPE_ARRAYBUFFER /*6*/:
                return "pager";
            case SpdyStream.RST_STREAM_IN_USE /*8*/:
                return "callback";
            case SpdyStream.RST_STREAM_ALREADY_CLOSED /*9*/:
                return "car";
            case SpdyStream.RST_INVALID_CREDENTIALS /*10*/:
                return "company main";
            case SpdyStream.RST_FRAME_TOO_LARGE /*11*/:
                return "isdn";
            case 13:
                return "other fax";
            case 14:
                return "radio";
            case 15:
                return "telex";
            case 16:
                return "tty tdd";
            case 17:
                return "work mobile";
            case 18:
                return "work pager";
            case 19:
                return "assistant";
            case ContactManager.PERMISSION_DENIED_ERROR /*20*/:
                return "mms";
            default:
                return "other";
        }
    }

    private int getContactType(String string) {
        if (string == null) {
            return 3;
        }
        if ("home".equals(string.toLowerCase())) {
            return 1;
        }
        if ("work".equals(string.toLowerCase())) {
            return 2;
        }
        if ("other".equals(string.toLowerCase())) {
            return 3;
        }
        if (NetworkManager.MOBILE.equals(string.toLowerCase())) {
            return 4;
        }
        if ("custom".equals(string.toLowerCase())) {
            return 0;
        }
        return 3;
    }

    private String getContactType(int type) {
        switch (type) {
            case CordovaResourceApi.URI_TYPE_FILE /*0*/:
                return "custom";
            case ContactManager.INVALID_ARGUMENT_ERROR /*1*/:
                return "home";
            case ContactManager.TIMEOUT_ERROR /*2*/:
                return "work";
            case ContactManager.IO_ERROR /*4*/:
                return NetworkManager.MOBILE;
            default:
                return "other";
        }
    }

    private int getOrgType(String string) {
        if (string == null) {
            return 2;
        }
        if ("work".equals(string.toLowerCase())) {
            return 1;
        }
        if ("other".equals(string.toLowerCase())) {
            return 2;
        }
        if ("custom".equals(string.toLowerCase())) {
            return 0;
        }
        return 2;
    }

    private String getOrgType(int type) {
        switch (type) {
            case CordovaResourceApi.URI_TYPE_FILE /*0*/:
                return "custom";
            case ContactManager.INVALID_ARGUMENT_ERROR /*1*/:
                return "work";
            default:
                return "other";
        }
    }

    private int getAddressType(String string) {
        if (string == null) {
            return 3;
        }
        if ("work".equals(string.toLowerCase())) {
            return 2;
        }
        if ("other".equals(string.toLowerCase())) {
            return 3;
        }
        if ("home".equals(string.toLowerCase())) {
            return 1;
        }
        return 3;
    }

    private String getAddressType(int type) {
        switch (type) {
            case ContactManager.INVALID_ARGUMENT_ERROR /*1*/:
                return "home";
            case ContactManager.TIMEOUT_ERROR /*2*/:
                return "work";
            default:
                return "other";
        }
    }

    private int getImType(String string) {
        if (string == null) {
            return -1;
        }
        if ("aim".equals(string.toLowerCase())) {
            return 0;
        }
        if ("google talk".equals(string.toLowerCase())) {
            return 5;
        }
        if ("icq".equals(string.toLowerCase())) {
            return 6;
        }
        if ("jabber".equals(string.toLowerCase())) {
            return 7;
        }
        if ("msn".equals(string.toLowerCase())) {
            return 1;
        }
        if ("netmeeting".equals(string.toLowerCase())) {
            return 8;
        }
        if ("qq".equals(string.toLowerCase())) {
            return 4;
        }
        if ("skype".equals(string.toLowerCase())) {
            return 3;
        }
        if ("yahoo".equals(string.toLowerCase())) {
            return 2;
        }
        return -1;
    }

    private String getImType(int type) {
        switch (type) {
            case CordovaResourceApi.URI_TYPE_FILE /*0*/:
                return "AIM";
            case ContactManager.INVALID_ARGUMENT_ERROR /*1*/:
                return "MSN";
            case ContactManager.TIMEOUT_ERROR /*2*/:
                return "Yahoo";
            case ContactManager.PENDING_OPERATION_ERROR /*3*/:
                return "Skype";
            case ContactManager.IO_ERROR /*4*/:
                return "QQ";
            case ContactManager.NOT_SUPPORTED_ERROR /*5*/:
                return "Google Talk";
            case PluginResult.MESSAGE_TYPE_ARRAYBUFFER /*6*/:
                return "ICQ";
            case PluginResult.MESSAGE_TYPE_BINARYSTRING /*7*/:
                return "Jabber";
            case SpdyStream.RST_STREAM_IN_USE /*8*/:
                return "NetMeeting";
            default:
                return "custom";
        }
    }
}
