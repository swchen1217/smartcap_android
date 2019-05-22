package com.example.smartcap;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
//import android.R;

public class PrefsActivity extends PreferenceActivity {
    // Option names and default values

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting);
    }

    public static String getServer(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString("server", "");
    }

    public static String getDevice(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString("device", "");
    }

    public static String getUser(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString("user", "");
    }
}