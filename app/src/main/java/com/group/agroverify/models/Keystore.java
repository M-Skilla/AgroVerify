package com.group.agroverify.models;

import com.google.gson.annotations.SerializedName;

public class Keystore {

    private String kid;

    @SerializedName("public_key_pem")
    private String publicKey;

    public Keystore() {
    }

    public Keystore(String kid, String publicKey) {
        this.kid = kid;
        this.publicKey = publicKey;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
