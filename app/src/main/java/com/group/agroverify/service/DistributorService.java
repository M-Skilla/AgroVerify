package com.group.agroverify.service;

import com.group.agroverify.dtos.BatchAssignmentRequest;
import com.group.agroverify.dtos.BatchAssignmentResponse;
import com.group.agroverify.dtos.DistributorLoginRequest;
import com.group.agroverify.dtos.DistributorLoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface DistributorService {

    @POST("distributor-login")
    Call<DistributorLoginResponse> login(@Body DistributorLoginRequest request);

    @POST("batch-assignment")
    Call<BatchAssignmentResponse> assignBatch(@Body BatchAssignmentRequest request);
}
