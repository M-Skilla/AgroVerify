package com.group.agroverify.dtos;

public class BatchAssignmentResponse {
    private boolean success;
    private String message;
    private String assignmentId;

    public BatchAssignmentResponse() {}

    public BatchAssignmentResponse(boolean success, String message, String assignmentId) {
        this.success = success;
        this.message = message;
        this.assignmentId = assignmentId;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }
}
