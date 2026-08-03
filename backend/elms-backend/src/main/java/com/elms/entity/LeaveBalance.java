package com.elms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "leave_balances",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_employee_leave_type",
            columnNames = {"employee_id", "leave_type_id"}
        )
    }
)
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "allocated_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal allocatedDays;

    @Column(name = "used_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal usedDays;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor required by JPA
    public LeaveBalance() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public BigDecimal getAllocatedDays() {
        return allocatedDays;
    }

    public void setAllocatedDays(BigDecimal allocatedDays) {
        this.allocatedDays = allocatedDays;
    }

    public BigDecimal getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(BigDecimal usedDays) {
        this.usedDays = usedDays;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Calculate remaining leave
    public BigDecimal getRemainingDays() {

        if (allocatedDays == null || usedDays == null) {
            return BigDecimal.ZERO;
        }

        return allocatedDays.subtract(usedDays);
    }
}