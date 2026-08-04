package com.elms.service;

import com.elms.entity.Employee;
import com.elms.dto.LeaveBalanceResponse;
import com.elms.entity.LeaveBalance;
import com.elms.entity.LeaveType;
import com.elms.exception.ResourceNotFoundException;
import com.elms.repository.EmployeeRepository;
import com.elms.repository.LeaveBalanceRepository;
import com.elms.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

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

    public List<LeaveBalanceResponse> getBalancesByEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        return leaveBalanceRepository.findByEmployee(employee).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getBalancesByEmployee(Long employeeId, Authentication authentication) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        ensureCanViewEmployeeBalances(employee, authentication);
        return leaveBalanceRepository.findByEmployee(employee).stream().map(this::toResponse).toList();
    }

    public LeaveBalanceResponse getBalanceByEmployeeAndLeaveType(Long employeeId, Long leaveTypeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + leaveTypeId));

        return toResponse(leaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave balance not found for employee " + employeeId + " and leave type " + leaveTypeId)));
    }

    @Transactional(readOnly = true)
    public LeaveBalanceResponse getBalanceByEmployeeAndLeaveType(Long employeeId, Long leaveTypeId, Authentication authentication) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        ensureCanViewEmployeeBalances(employee, authentication);
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + leaveTypeId));
        return toResponse(leaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave balance not found for employee " + employeeId + " and leave type " + leaveTypeId)));
    }

    @Transactional
    public LeaveBalanceResponse createBalance(Long employeeId, Long leaveTypeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + leaveTypeId));

        if (leaveBalanceRepository.existsByEmployeeAndLeaveType(employee, leaveType)) {
            throw new IllegalStateException(
                    "Leave balance already exists for employee " + employeeId + " and leave type " + leaveTypeId);
        }

        return toResponse(createBalance(employee, leaveType));
    }

    /** Creates missing balances only; existing balance values are never overwritten. */
    @Transactional
    public void initializeMissingBalances() {
        List<Employee> employees = employeeRepository.findAll();
        List<LeaveType> activeLeaveTypes = leaveTypeRepository.findByActiveTrue();
        for (Employee employee : employees) {
            createMissingBalancesForEmployee(employee, activeLeaveTypes);
        }
    }

    @Transactional
    public void createMissingBalancesForEmployee(Employee employee) {
        createMissingBalancesForEmployee(employee, leaveTypeRepository.findByActiveTrue());
    }

    @Transactional
    public void createMissingBalancesForLeaveType(LeaveType leaveType) {
        for (Employee employee : employeeRepository.findAll()) {
            if (!leaveBalanceRepository.existsByEmployeeAndLeaveType(employee, leaveType)) {
                createBalance(employee, leaveType);
            }
        }
    }

    private void createMissingBalancesForEmployee(Employee employee, List<LeaveType> activeLeaveTypes) {
        for (LeaveType leaveType : activeLeaveTypes) {
            if (!leaveBalanceRepository.existsByEmployeeAndLeaveType(employee, leaveType)) {
                createBalance(employee, leaveType);
            }
        }
    }

    private LeaveBalance createBalance(Employee employee, LeaveType leaveType) {
        LeaveBalance balance = new LeaveBalance();
        balance.setEmployee(employee);
        balance.setLeaveType(leaveType);
        balance.setAllocatedDays(leaveType.getAllocatedDays());
        balance.setUsedDays(BigDecimal.ZERO);
        return leaveBalanceRepository.save(balance);
    }

    private void ensureCanViewEmployeeBalances(Employee targetEmployee, Authentication authentication) {
        boolean isHr = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_HR".equals(authority.getAuthority()));
        if (isHr) {
            return;
        }

        Employee requestingEmployee = employeeRepository.findByUserEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("No employee record is associated with this account"));
        boolean isOwnBalance = requestingEmployee.getId().equals(targetEmployee.getId());
        if (!isOwnBalance) {
            throw new AccessDeniedException("You are not authorized to view these leave balances");
        }
    }

    private LeaveBalanceResponse toResponse(LeaveBalance balance) {
        LeaveBalanceResponse response = new LeaveBalanceResponse();
        response.setId(balance.getId());
        response.setEmployeeId(balance.getEmployee().getId());
        response.setLeaveTypeId(balance.getLeaveType().getId());
        response.setLeaveTypeName(balance.getLeaveType().getName());
        response.setAllocatedDays(balance.getAllocatedDays());
        response.setUsedDays(balance.getUsedDays());
        response.setRemainingDays(balance.getRemainingDays());
        response.setUpdatedAt(balance.getUpdatedAt());
        return response;
    }
}
