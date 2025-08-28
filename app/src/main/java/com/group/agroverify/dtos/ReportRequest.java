package com.group.agroverify.dtos;

public class ReportRequest {
    private String serial;
    private String signed_token;
    private String reporter_contact;
    private String notes;

    public ReportRequest() {}

    public ReportRequest(String serial, String signed_token, String reporter_contact, String notes) {
        this.serial = serial;
        this.signed_token = signed_token;
        this.reporter_contact = reporter_contact;
        this.notes = notes;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getSigned_token() {
        return signed_token;
    }

    public void setSigned_token(String signed_token) {
        this.signed_token = signed_token;
    }

    public String getReporter_contact() {
        return reporter_contact;
    }

    public void setReporter_contact(String reporter_contact) {
        this.reporter_contact = reporter_contact;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
