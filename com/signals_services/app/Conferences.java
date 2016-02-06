package com.signals_services.app;

import android.os.Bundle;
import org.apache.cordova.Config;
import org.apache.cordova.CordovaActivity;

public class Conferences extends CordovaActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.init();
        super.loadUrl(Config.getStartUrl());
    }
}
