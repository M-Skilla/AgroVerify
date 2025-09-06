package com.group.agroverify.dtos;

import com.group.agroverify.models.Distributor;

public class DistributorLoginResponse {
    private boolean success;
    private String message;
    private Distributor distributor;

    public DistributorLoginResponse() {}

    public DistributorLoginResponse(boolean success, String message, Distributor distributor) {
        this.success = success;
        this.message = message;
        this.distributor = distributor;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Distributor getDistributor() { return distributor; }
    public void setDistributor(Distributor distributor) { this.distributor = distributor; }
}
