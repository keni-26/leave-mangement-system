package com.elms.repository;

import com.elms.entity.Employee;
import com.elms.entity.LeaveBalance;
import com.elms.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository
        extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByEmployee(Employee employee);

    Optional<LeaveBalance> findByEmployeeAndLeaveType(
            Employee employee,
            LeaveType leaveType
    );

    boolean existsByEmployeeAndLeaveType(
            Employee employee,
            LeaveType leaveType
    );
}