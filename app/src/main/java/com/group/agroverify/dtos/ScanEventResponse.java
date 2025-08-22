package com.group.agroverify.dtos;

public class ScanEventResponse {

    private String id, serial, timestamp, ip, msisdn, user_agent;

    public ScanEventResponse() {
    }

    public ScanEventResponse(String id, String serial, String timestamp, String ip, String msisdn, String user_agent) {
        this.id = id;
        this.serial = serial;
        this.timestamp = timestamp;
        this.ip = ip;
        this.msisdn = msisdn;
        this.user_agent = user_agent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getUser_agent() {
        return user_agent;
    }

    public void setUser_agent(String user_agent) {
        this.user_agent = user_agent;
    }
}
