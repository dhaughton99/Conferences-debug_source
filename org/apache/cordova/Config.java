package org.apache.cordova;

import android.app.Activity;
import android.content.res.XmlResourceParser;
import android.util.Log;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParserException;

public class Config {
    public static final String TAG = "Config";
    private static Config self;
    private String startUrl;
    private Whitelist whitelist;

    static {
        self = null;
    }

    public static void init(Activity action) {
        self = new Config(action);
    }

    public static void init() {
        if (self == null) {
            self = new Config();
        }
    }

    private Config() {
        this.whitelist = new Whitelist();
    }

    private Config(Activity action) {
        this.whitelist = new Whitelist();
        if (action == null) {
            LOG.m6i("CordovaLog", "There is no activity. Is this on the lock screen?");
            return;
        }
        int id = action.getResources().getIdentifier("config", "xml", action.getClass().getPackage().getName());
        if (id == 0) {
            id = action.getResources().getIdentifier("config", "xml", action.getPackageName());
            if (id == 0) {
                LOG.m6i("CordovaLog", "config.xml missing. Ignoring...");
                return;
            }
        }
        this.whitelist.addWhiteListEntry("file:///*", false);
        this.whitelist.addWhiteListEntry("content:///*", false);
        this.whitelist.addWhiteListEntry("data:*", false);
        XmlResourceParser xml = action.getResources().getXml(id);
        int eventType = -1;
        while (eventType != 1) {
            if (eventType == 2) {
                String strNode = xml.getName();
                if (strNode.equals("access")) {
                    String origin = xml.getAttributeValue(null, "origin");
                    String subdomains = xml.getAttributeValue(null, "subdomains");
                    if (origin != null) {
                        boolean z;
                        Whitelist whitelist = this.whitelist;
                        if (subdomains != null) {
                            if (subdomains.compareToIgnoreCase("true") == 0) {
                                z = true;
                                whitelist.addWhiteListEntry(origin, z);
                            }
                        }
                        z = false;
                        whitelist.addWhiteListEntry(origin, z);
                    }
                } else {
                    if (strNode.equals("log")) {
                        String level = xml.getAttributeValue(null, "level");
                        Log.d(TAG, "The <log> tags is deprecated. Use <preference name=\"loglevel\" value=\"" + level + "\"/> instead.");
                        if (level != null) {
                            LOG.setLogLevel(level);
                        }
                    } else {
                        if (strNode.equals("preference")) {
                            String name = xml.getAttributeValue(null, "name").toLowerCase(Locale.getDefault());
                            if (name.equalsIgnoreCase("LogLevel")) {
                                LOG.setLogLevel(xml.getAttributeValue(null, "value"));
                            } else {
                                String value;
                                if (name.equalsIgnoreCase("SplashScreen")) {
                                    value = xml.getAttributeValue(null, "value");
                                    if (value == null) {
                                        value = "splash";
                                    }
                                    int resource = action.getResources().getIdentifier(value, "drawable", action.getClass().getPackage().getName());
                                    action.getIntent().putExtra(name, resource);
                                } else {
                                    int value2;
                                    if (name.equalsIgnoreCase("BackgroundColor")) {
                                        value2 = xml.getAttributeIntValue(null, "value", -16777216);
                                        action.getIntent().putExtra(name, value2);
                                    } else {
                                        if (name.equalsIgnoreCase("LoadUrlTimeoutValue")) {
                                            value2 = xml.getAttributeIntValue(null, "value", 20000);
                                            action.getIntent().putExtra(name, value2);
                                        } else {
                                            if (name.equalsIgnoreCase("SplashScreenDelay")) {
                                                value2 = xml.getAttributeIntValue(null, "value", 3000);
                                                action.getIntent().putExtra(name, value2);
                                            } else {
                                                boolean value3;
                                                if (name.equalsIgnoreCase("KeepRunning")) {
                                                    value3 = xml.getAttributeValue(null, "value").equals("true");
                                                    action.getIntent().putExtra(name, value3);
                                                } else {
                                                    if (name.equalsIgnoreCase("InAppBrowserStorageEnabled")) {
                                                        value3 = xml.getAttributeValue(null, "value").equals("true");
                                                        action.getIntent().putExtra(name, value3);
                                                    } else {
                                                        if (name.equalsIgnoreCase("DisallowOverscroll")) {
                                                            value3 = xml.getAttributeValue(null, "value").equals("true");
                                                            action.getIntent().putExtra(name, value3);
                                                        } else {
                                                            value = xml.getAttributeValue(null, "value");
                                                            action.getIntent().putExtra(name, value);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (strNode.equals("content")) {
                                String src = xml.getAttributeValue(null, "src");
                                LOG.m8i("CordovaLog", "Found start page location: %s", src);
                                if (src != null) {
                                    if (Pattern.compile("^[a-z-]+://").matcher(src).find()) {
                                        this.startUrl = src;
                                    } else {
                                        if (src.charAt(0) == '/') {
                                            src = src.substring(1);
                                        }
                                        this.startUrl = "file:///android_asset/www/" + src;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            try {
                eventType = xml.next();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public static void addWhiteListEntry(String origin, boolean subdomains) {
        if (self != null) {
            self.whitelist.addWhiteListEntry(origin, subdomains);
        }
    }

    public static boolean isUrlWhiteListed(String url) {
        if (self == null) {
            return false;
        }
        return self.whitelist.isUrlWhiteListed(url);
    }

    public static String getStartUrl() {
        if (self == null || self.startUrl == null) {
            return "file:///android_asset/www/index.html";
        }
        return self.startUrl;
    }
}
