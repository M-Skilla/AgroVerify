package com.group.agroverify.service;

import com.group.agroverify.dtos.PublicKeyRequest;
import com.group.agroverify.dtos.ScanEventRequest;
import com.group.agroverify.dtos.ScanEventResponse;
import com.group.agroverify.models.Keystore;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface KeyService {
    @Headers({
            "Content-Type: application/json"
    })
    @POST("getActivePublicKey/")
    Call<Keystore> getActivePublicKey();

    @Headers({
            "Content-Type: application/json"
    })
    @POST("getPublicKeyByKid")
    Call<Keystore> getPublicKeyByKid(@Body PublicKeyRequest request);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("addScanEvent")
    Call<ScanEventResponse> addScanEvent(@Body ScanEventRequest request);
}
