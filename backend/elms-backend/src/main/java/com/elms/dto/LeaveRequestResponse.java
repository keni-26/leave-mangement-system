package com.elms.dto;

import com.elms.entity.LeaveRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Safe API representation of a leave request; it deliberately contains no JPA relations. */
public class LeaveRequestResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String employeeEmail;
    private String managerName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal leaveDays;
    private String reason;
    private LeaveRequestStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; } public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; } public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmployeeCode() { return employeeCode; } public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getEmployeeEmail() { return employeeEmail; } public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }
    public String getManagerName() { return managerName; } public void setManagerName(String managerName) { this.managerName = managerName; }
    public Long getLeaveTypeId() { return leaveTypeId; } public void setLeaveTypeId(Long leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public String getLeaveTypeName() { return leaveTypeName; } public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getLeaveDays() { return leaveDays; } public void setLeaveDays(BigDecimal leaveDays) { this.leaveDays = leaveDays; }
    public String getReason() { return reason; } public void setReason(String reason) { this.reason = reason; }
    public LeaveRequestStatus getStatus() { return status; } public void setStatus(LeaveRequestStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; } public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewedBy() { return reviewedBy; } public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
}
