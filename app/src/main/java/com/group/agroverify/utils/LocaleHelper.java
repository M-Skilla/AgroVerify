package com.group.agroverify.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleHelper {

    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";
    private static final String SWAHILI = "sw";
    private static final String ENGLISH = "en";

    public static Context onAttach(Context context) {
        String lang = getPersistedData(context, SWAHILI); // Default to Swahili
        return setLocale(context, lang);
    }

    public static Context onAttach(Context context, String defaultLanguage) {
        String lang = getPersistedData(context, defaultLanguage);
        return setLocale(context, lang);
    }

    public static String getLanguage(Context context) {
        return getPersistedData(context, SWAHILI);
    }

    public static Context setLocale(Context context, String language) {
        persist(context, language);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        configuration.setLocale(locale);

        return context.createConfigurationContext(configuration);
    }

    public static Context switchLanguage(Context context) {
        String currentLang = getLanguage(context);
        String newLang = currentLang.equals(SWAHILI) ? ENGLISH : SWAHILI;
        return setLocale(context, newLang);
    }

    public static boolean isSwahili(Context context) {
        return getLanguage(context).equals(SWAHILI);
    }

    private static String getPersistedData(Context context, String defaultLanguage) {
        SharedPreferences preferences = context.getSharedPreferences("locale", Context.MODE_PRIVATE);
        return preferences.getString(SELECTED_LANGUAGE, defaultLanguage);
    }

    private static void persist(Context context, String language) {
        SharedPreferences preferences = context.getSharedPreferences("locale", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SELECTED_LANGUAGE, language);
        editor.apply();
    }
}
