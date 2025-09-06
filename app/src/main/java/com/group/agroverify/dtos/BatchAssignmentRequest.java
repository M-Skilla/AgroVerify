package com.group.agroverify.dtos;

public class BatchAssignmentRequest {
    private int distributorId;
    private String batchId;
    private String scanTimestamp;

    public BatchAssignmentRequest() {}

    public BatchAssignmentRequest(int distributorId, String batchId, String scanTimestamp) {
        this.distributorId = distributorId;
        this.batchId = batchId;
        this.scanTimestamp = scanTimestamp;
    }

    public int getDistributorId() { return distributorId; }
    public void setDistributorId(int distributorId) { this.distributorId = distributorId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getScanTimestamp() { return scanTimestamp; }
    public void setScanTimestamp(String scanTimestamp) { this.scanTimestamp = scanTimestamp; }
}
