package com.elms.service;

import com.elms.entity.Employee;
import com.elms.entity.LeaveBalance;
import com.elms.entity.LeaveType;
import com.elms.repository.EmployeeRepository;
import com.elms.repository.LeaveBalanceRepository;
import com.elms.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveBalanceService(
            LeaveBalanceRepository leaveBalanceRepository,
            EmployeeRepository employeeRepository,
            LeaveTypeRepository leaveTypeRepository
    ) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    public List<LeaveBalance> getBalancesByEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        return leaveBalanceRepository.findByEmployee(employee);
    }

    public LeaveBalance getBalanceByEmployeeAndLeaveType(Long employeeId, Long leaveTypeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new RuntimeException("Leave type not found with id: " + leaveTypeId));

        return leaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new RuntimeException(
                        "Leave balance not found for employee " + employeeId + " and leave type " + leaveTypeId));
    }

    public LeaveBalance createBalance(Long employeeId, Long leaveTypeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new RuntimeException("Leave type not found with id: " + leaveTypeId));

        if (leaveBalanceRepository.existsByEmployeeAndLeaveType(employee, leaveType)) {
            throw new RuntimeException(
                    "Leave balance already exists for employee " + employeeId + " and leave type " + leaveTypeId);
        }

        LeaveBalance balance = new LeaveBalance();
        balance.setEmployee(employee);
        balance.setLeaveType(leaveType);
        balance.setAllocatedDays(leaveType.getAllocatedDays());
        balance.setUsedDays(BigDecimal.ZERO);

        return leaveBalanceRepository.save(balance);
    }
}
