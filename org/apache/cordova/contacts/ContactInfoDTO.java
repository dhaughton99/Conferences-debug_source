package org.apache.cordova.contacts;

import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class ContactInfoDTO {
    JSONArray addresses;
    String birthday;
    HashMap<String, Object> desiredFieldsWithVals;
    String displayName;
    JSONArray emails;
    JSONArray ims;
    JSONObject name;
    String nickname;
    String note;
    JSONArray organizations;
    JSONArray phones;
    JSONArray photos;
    JSONArray websites;

    public ContactInfoDTO() {
        this.displayName = "";
        this.name = new JSONObject();
        this.organizations = new JSONArray();
        this.addresses = new JSONArray();
        this.phones = new JSONArray();
        this.emails = new JSONArray();
        this.ims = new JSONArray();
        this.websites = new JSONArray();
        this.photos = new JSONArray();
        this.note = "";
        this.nickname = "";
        this.desiredFieldsWithVals = new HashMap();
    }
}
