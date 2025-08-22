package com.group.agroverify.dtos;

public class PublicKeyRequest {

    String kid;

    public PublicKeyRequest(String kid) {
        this.kid = kid;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }
}
