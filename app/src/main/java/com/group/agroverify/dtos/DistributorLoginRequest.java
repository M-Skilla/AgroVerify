package com.group.agroverify.dtos;

public class DistributorLoginRequest {
    private String nin;
    private String password;

    public DistributorLoginRequest() {}

    public DistributorLoginRequest(String nin, String password) {
        this.nin = nin;
        this.password = password;
    }

    public String getNin() { return nin; }
    public void setNin(String nin) { this.nin = nin; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
