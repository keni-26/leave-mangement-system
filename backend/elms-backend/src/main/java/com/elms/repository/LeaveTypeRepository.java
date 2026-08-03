package com.elms.repository;

import com.elms.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    Optional<LeaveType> findByName(String name);

    List<LeaveType> findByActiveTrue();

    boolean existsByName(String name);
}