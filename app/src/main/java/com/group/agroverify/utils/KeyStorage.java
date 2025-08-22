package com.group.agroverify.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class KeyStorage {

    private static final String PREFS_NAME = "agro_key";

    private static final String PUBLIC_KEY_NAME = "public_key";

    public static void setPublicKey(Context context, String publicKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PUBLIC_KEY_NAME, publicKey).apply();
    }

    public static String getPublicKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PUBLIC_KEY_NAME, null);
    }
}
