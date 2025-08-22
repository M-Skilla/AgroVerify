package com.group.agroverify.dtos;

public class ScanEventRequest {

    private String serial;

    private String msisdn;

    public ScanEventRequest(String serial, String msisdn) {
        this.serial = serial;
        this.msisdn = msisdn;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }
}
