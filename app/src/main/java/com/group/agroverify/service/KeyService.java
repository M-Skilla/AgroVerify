package com.group.agroverify.service;

import com.group.agroverify.dtos.PublicKeyRequest;
import com.group.agroverify.dtos.ReportRequest;
import com.group.agroverify.dtos.ReportResponse;
import com.group.agroverify.dtos.ScanEventRequest;
import com.group.agroverify.dtos.ScanEventResponse;
import com.group.agroverify.models.Keystore;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

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

    @Multipart
    @POST("addReport")
    Call<ReportResponse> addReport(
            @Part("reporter_contact") RequestBody reporterContact,
            @Part("notes") RequestBody notes,
            @Part MultipartBody.Part photo
    );
}
