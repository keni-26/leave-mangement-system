package com.elms.controller;

import com.elms.dto.LeaveBalanceResponse;
import com.elms.service.LeaveBalanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/leave-balances")
@CrossOrigin(origins = "http://localhost:5173")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveBalanceResponse>> getBalancesByEmployee(@PathVariable Long employeeId, Authentication authentication) {
        return ResponseEntity.ok(leaveBalanceService.getBalancesByEmployee(employeeId, authentication));
    }

    @GetMapping("/employee/{employeeId}/leave-type/{leaveTypeId}")
    public ResponseEntity<LeaveBalanceResponse> getBalanceByEmployeeAndLeaveType(
            @PathVariable Long employeeId,
            @PathVariable Long leaveTypeId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(leaveBalanceService.getBalanceByEmployeeAndLeaveType(employeeId, leaveTypeId, authentication));
    }

    @PostMapping("/employee/{employeeId}/leave-type/{leaveTypeId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<LeaveBalanceResponse> createBalance(
            @PathVariable Long employeeId,
            @PathVariable Long leaveTypeId
    ) {
        LeaveBalanceResponse createdBalance = leaveBalanceService.createBalance(employeeId, leaveTypeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBalance);
    }
}
