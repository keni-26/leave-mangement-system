package com.elms.controller;

import com.elms.dto.LeaveApprovalRequest;
import com.elms.dto.LeaveRejectionRequest;
import com.elms.dto.LeaveRequestResponse;
import com.elms.entity.Employee;
import com.elms.entity.LeaveRequest;
import com.elms.entity.LeaveType;
import com.elms.service.LeaveRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@CrossOrigin(origins = "http://localhost:5173")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(
            @Valid @RequestBody LeaveRequestCreateRequest request,
            Authentication authentication
    ) {
        LeaveRequest leaveRequest = new LeaveRequest();
        Employee employee = new Employee();
        employee.setId(request.getEmployeeId());
        leaveRequest.setEmployee(employee);

        LeaveType leaveType = new LeaveType();
        leaveType.setId(request.getLeaveTypeId());
        leaveRequest.setLeaveType(leaveType);

        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason());

        LeaveRequestResponse created = leaveRequestService.createLeaveRequest(leaveRequest, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequestResponse>> getLeaveRequestsByEmployee(
            @PathVariable Long employeeId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestsByEmployee(employeeId, authentication.getName()));
    }

    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<List<LeaveRequestResponse>> getLeaveRequestsByManager(
            @PathVariable Long managerId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                leaveRequestService.getLeaveRequestsByManager(managerId, authentication.getName())
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<LeaveRequestResponse>> getAllLeaveRequests(Authentication authentication) {
        return ResponseEntity.ok(leaveRequestService.getAllLeaveRequests(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestResponse> getLeaveRequestById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestById(id, authentication.getName()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequestResponse> cancelLeaveRequest(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(leaveRequestService.cancelLeaveRequest(id, authentication.getName()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<LeaveRequestResponse> approveLeaveRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveApprovalRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(leaveRequestService.approveLeaveRequest(id, request, authentication.getName()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<LeaveRequestResponse> rejectLeaveRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRejectionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(leaveRequestService.rejectLeaveRequest(id, request, authentication.getName()));
    }

    public static class LeaveRequestCreateRequest {
        @NotNull @Positive private Long employeeId;
        @NotNull @Positive private Long leaveTypeId;
        @NotNull @FutureOrPresent private LocalDate startDate;
        @NotNull private LocalDate endDate;
        @NotBlank @Size(max = 2000) private String reason;

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public Long getLeaveTypeId() {
            return leaveTypeId;
        }

        public void setLeaveTypeId(Long leaveTypeId) {
            this.leaveTypeId = leaveTypeId;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
