package com.elms.repository;

import com.elms.entity.Employee;
import com.elms.entity.LeaveRequest;
import com.elms.entity.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeOrderByCreatedAtDesc(Employee employee);

    List<LeaveRequest> findByEmployeeManagerIdOrderByCreatedAtDesc(Long managerId);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee = :employee AND lr.status IN :statuses")
    List<LeaveRequest> findByEmployeeAndStatusIn(
            @Param("employee") Employee employee,
            @Param("statuses") List<LeaveRequestStatus> statuses
    );

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee = :employee AND lr.startDate <= :endDate AND lr.endDate >= :startDate AND lr.status IN :statuses")
    List<LeaveRequest> findOverlappingRequests(
            @Param("employee") Employee employee,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<LeaveRequestStatus> statuses
    );
}
