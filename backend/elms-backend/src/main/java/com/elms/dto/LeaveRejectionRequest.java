package com.elms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LeaveRejectionRequest {
    /**
     * @deprecated Reviewer identity is derived from the authenticated JWT user.
     * This field is accepted temporarily for frontend compatibility and is ignored.
     */
    private Long managerId;
    @NotBlank @Size(max = 2000) private String rejectionReason;

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
