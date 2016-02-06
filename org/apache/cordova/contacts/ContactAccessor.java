package org.apache.cordova.contacts;

import android.util.Log;
import java.util.HashMap;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class ContactAccessor {
    protected final String LOG_TAG;
    protected CordovaInterface mApp;

    class WhereOptions {
        private String where;
        private String[] whereArgs;

        WhereOptions() {
        }

        public void setWhere(String where) {
            this.where = where;
        }

        public String getWhere() {
            return this.where;
        }

        public void setWhereArgs(String[] whereArgs) {
            this.whereArgs = whereArgs;
        }

        public String[] getWhereArgs() {
            return this.whereArgs;
        }
    }

    public abstract JSONObject getContactById(String str) throws JSONException;

    public abstract JSONObject getContactById(String str, JSONArray jSONArray) throws JSONException;

    public abstract boolean remove(String str);

    public abstract String save(JSONObject jSONObject);

    public abstract JSONArray search(JSONArray jSONArray, JSONObject jSONObject);

    public ContactAccessor() {
        this.LOG_TAG = "ContactsAccessor";
    }

    protected boolean isRequired(String key, HashMap<String, Boolean> map) {
        Boolean retVal = (Boolean) map.get(key);
        return retVal == null ? false : retVal.booleanValue();
    }

    protected HashMap<String, Boolean> buildPopulationSet(JSONObject options) {
        HashMap<String, Boolean> map = new HashMap();
        JSONArray desiredFields = null;
        if (options != null) {
            try {
                if (options.has("desiredFields")) {
                    desiredFields = options.getJSONArray("desiredFields");
                }
            } catch (JSONException e) {
                Log.e("ContactsAccessor", e.getMessage(), e);
            }
        }
        if (desiredFields == null || desiredFields.length() == 0) {
            map.put("displayName", Boolean.valueOf(true));
            map.put("name", Boolean.valueOf(true));
            map.put("nickname", Boolean.valueOf(true));
            map.put("phoneNumbers", Boolean.valueOf(true));
            map.put("emails", Boolean.valueOf(true));
            map.put("addresses", Boolean.valueOf(true));
            map.put("ims", Boolean.valueOf(true));
            map.put("organizations", Boolean.valueOf(true));
            map.put("birthday", Boolean.valueOf(true));
            map.put("note", Boolean.valueOf(true));
            map.put("urls", Boolean.valueOf(true));
            map.put("photos", Boolean.valueOf(true));
            map.put("categories", Boolean.valueOf(true));
            return map;
        }
        for (int i = 0; i < desiredFields.length(); i++) {
            String key = desiredFields.getString(i);
            if (key.startsWith("displayName")) {
                map.put("displayName", Boolean.valueOf(true));
            } else if (key.startsWith("name")) {
                map.put("displayName", Boolean.valueOf(true));
                map.put("name", Boolean.valueOf(true));
            } else if (key.startsWith("nickname")) {
                map.put("nickname", Boolean.valueOf(true));
            } else if (key.startsWith("phoneNumbers")) {
                map.put("phoneNumbers", Boolean.valueOf(true));
            } else if (key.startsWith("emails")) {
                map.put("emails", Boolean.valueOf(true));
            } else if (key.startsWith("addresses")) {
                map.put("addresses", Boolean.valueOf(true));
            } else if (key.startsWith("ims")) {
                map.put("ims", Boolean.valueOf(true));
            } else if (key.startsWith("organizations")) {
                map.put("organizations", Boolean.valueOf(true));
            } else if (key.startsWith("birthday")) {
                map.put("birthday", Boolean.valueOf(true));
            } else if (key.startsWith("note")) {
                map.put("note", Boolean.valueOf(true));
            } else if (key.startsWith("urls")) {
                map.put("urls", Boolean.valueOf(true));
            } else if (key.startsWith("photos")) {
                map.put("photos", Boolean.valueOf(true));
            } else if (key.startsWith("categories")) {
                map.put("categories", Boolean.valueOf(true));
            }
        }
        return map;
    }

    protected String getJsonString(JSONObject obj, String property) {
        if (obj == null) {
            return null;
        }
        try {
            String value = obj.getString(property);
            if (!value.equals("null")) {
                return value;
            }
            Log.d("ContactsAccessor", property + " is string called 'null'");
            return null;
        } catch (JSONException e) {
            Log.d("ContactsAccessor", "Could not get = " + e.getMessage());
            return null;
        }
    }
}
