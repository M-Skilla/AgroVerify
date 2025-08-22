package com.group.agroverify.utils;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://fjkhoubrsvxgqueeijwn.supabase.co/functions/v1/";

    private static Retrofit retrofit;

    public static Retrofit getRetrofit() {
        if (retrofit == null) { 
            String baseUrl = BASE_URL;
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
