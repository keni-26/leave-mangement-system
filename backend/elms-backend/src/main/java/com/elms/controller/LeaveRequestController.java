package com.elms.controller;

import com.elms.dto.LeaveApprovalRequest;
import com.elms.dto.LeaveRejectionRequest;
import com.elms.entity.Employee;
import com.elms.entity.LeaveRequest;
import com.elms.entity.LeaveType;
import com.elms.service.LeaveRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<LeaveRequest> createLeaveRequest(@RequestBody LeaveRequestCreateRequest request) {
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

        LeaveRequest created = leaveRequestService.createLeaveRequest(leaveRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getLeaveRequestsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestsByEmployee(employeeId));
    }

    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<List<LeaveRequest>> getLeaveRequestsByManager(
            @PathVariable Long managerId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                leaveRequestService.getLeaveRequestsByManager(managerId, authentication.getName(), isHr(authentication))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequest> getLeaveRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestById(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequest> cancelLeaveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.cancelLeaveRequest(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<LeaveRequest> approveLeaveRequest(
            @PathVariable Long id,
            @RequestBody LeaveApprovalRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(leaveRequestService.approveLeaveRequest(id, request, authentication.getName(), isHr(authentication)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<LeaveRequest> rejectLeaveRequest(
            @PathVariable Long id,
            @RequestBody LeaveRejectionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(leaveRequestService.rejectLeaveRequest(id, request, authentication.getName(), isHr(authentication)));
    }

    private boolean isHr(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_HR".equals(authority.getAuthority()));
    }

    public static class LeaveRequestCreateRequest {
        private Long employeeId;
        private Long leaveTypeId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;

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
