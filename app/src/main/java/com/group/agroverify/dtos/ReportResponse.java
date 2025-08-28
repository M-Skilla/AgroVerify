package com.group.agroverify.dtos;

public class ReportResponse {
    private boolean success;
    private String message;
    private String reportId;

    public ReportResponse() {}

    public ReportResponse(boolean success, String message, String reportId) {
        this.success = success;
        this.message = message;
        this.reportId = reportId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
}
