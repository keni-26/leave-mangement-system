package com.elms.dto;

public class LeaveRejectionRequest {
    /**
     * @deprecated Reviewer identity is derived from the authenticated JWT user.
     * This field is accepted temporarily for frontend compatibility and is ignored.
     */
    private Long managerId;
    private String rejectionReason;

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
