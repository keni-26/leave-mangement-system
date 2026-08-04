package com.elms.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Safe API representation of a leave balance; it deliberately contains no JPA relations. */
public class LeaveBalanceResponse {
    private Long id;
    private Long employeeId;
    private Long leaveTypeId;
    private String leaveTypeName;
    private BigDecimal allocatedDays;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;
    private LocalDateTime updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; } public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Long getLeaveTypeId() { return leaveTypeId; } public void setLeaveTypeId(Long leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public String getLeaveTypeName() { return leaveTypeName; } public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    public BigDecimal getAllocatedDays() { return allocatedDays; } public void setAllocatedDays(BigDecimal allocatedDays) { this.allocatedDays = allocatedDays; }
    public BigDecimal getUsedDays() { return usedDays; } public void setUsedDays(BigDecimal usedDays) { this.usedDays = usedDays; }
    public BigDecimal getRemainingDays() { return remainingDays; } public void setRemainingDays(BigDecimal remainingDays) { this.remainingDays = remainingDays; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
