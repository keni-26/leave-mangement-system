package com.elms.repository;

import com.elms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByUserId(Long userId);

    boolean existsByEmployeeCode(String employeeCode);
}