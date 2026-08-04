package com.elms.dto;

public class LeaveApprovalRequest {
    /**
     * @deprecated Reviewer identity is derived from the authenticated JWT user.
     * This field is accepted temporarily for frontend compatibility and is ignored.
     */
    private Long managerId;

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }
}
