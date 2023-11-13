package com.example.nsyy.config;

import android.content.Context;
import android.content.SharedPreferences;

public class MySharedPreferences {

    private static final String PREFS_NAME = "NsyyConfig";
    private static SharedPreferences sharedPreferences;

    public static void init(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
}