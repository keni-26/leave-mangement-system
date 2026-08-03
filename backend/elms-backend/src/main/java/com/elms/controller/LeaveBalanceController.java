package com.elms.controller;

import com.elms.entity.LeaveBalance;
import com.elms.service.LeaveBalanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<LeaveBalance>> getBalancesByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveBalanceService.getBalancesByEmployee(employeeId));
    }

    @GetMapping("/employee/{employeeId}/leave-type/{leaveTypeId}")
    public ResponseEntity<LeaveBalance> getBalanceByEmployeeAndLeaveType(
            @PathVariable Long employeeId,
            @PathVariable Long leaveTypeId
    ) {
        return ResponseEntity.ok(leaveBalanceService.getBalanceByEmployeeAndLeaveType(employeeId, leaveTypeId));
    }

    @PostMapping("/employee/{employeeId}/leave-type/{leaveTypeId}")
    public ResponseEntity<LeaveBalance> createBalance(
            @PathVariable Long employeeId,
            @PathVariable Long leaveTypeId
    ) {
        LeaveBalance createdBalance = leaveBalanceService.createBalance(employeeId, leaveTypeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBalance);
    }
}
