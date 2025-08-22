package com.group.agroverify.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "verification_history")
public class VerificationHistory {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "serial")
    private String serial;

    @ColumnInfo(name = "msisdn")
    private String msisdn;

    @ColumnInfo(name = "scan_timestamp")
    private long scanTimestamp;

    @ColumnInfo(name = "verification_result")
    private boolean verificationResult;

    @ColumnInfo(name = "response_id")
    private String responseId;

    @ColumnInfo(name = "response_timestamp")
    private String responseTimestamp;

    @ColumnInfo(name = "response_ip")
    private String responseIp;

    @ColumnInfo(name = "response_user_agent")
    private String responseUserAgent;

    @ColumnInfo(name = "error_message")
    private String errorMessage;

    // Default constructor
    public VerificationHistory() {
    }

    // Constructor for successful verification
    public VerificationHistory(String serial, String msisdn, long scanTimestamp,
                             boolean verificationResult, String responseId,
                             String responseTimestamp, String responseIp,
                             String responseUserAgent) {
        this.serial = serial;
        this.msisdn = msisdn;
        this.scanTimestamp = scanTimestamp;
        this.verificationResult = verificationResult;
        this.responseId = responseId;
        this.responseTimestamp = responseTimestamp;
        this.responseIp = responseIp;
        this.responseUserAgent = responseUserAgent;
    }

    // Constructor for failed verification
    public VerificationHistory(String serial, String msisdn, long scanTimestamp,
                             boolean verificationResult, String errorMessage) {
        this.serial = serial;
        this.msisdn = msisdn;
        this.scanTimestamp = scanTimestamp;
        this.verificationResult = verificationResult;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public long getScanTimestamp() {
        return scanTimestamp;
    }

    public void setScanTimestamp(long scanTimestamp) {
        this.scanTimestamp = scanTimestamp;
    }

    public boolean isVerificationResult() {
        return verificationResult;
    }

    public void setVerificationResult(boolean verificationResult) {
        this.verificationResult = verificationResult;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getResponseTimestamp() {
        return responseTimestamp;
    }

    public void setResponseTimestamp(String responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }

    public String getResponseIp() {
        return responseIp;
    }

    public void setResponseIp(String responseIp) {
        this.responseIp = responseIp;
    }

    public String getResponseUserAgent() {
        return responseUserAgent;
    }

    public void setResponseUserAgent(String responseUserAgent) {
        this.responseUserAgent = responseUserAgent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
