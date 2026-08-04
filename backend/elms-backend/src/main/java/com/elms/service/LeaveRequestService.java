package com.elms.service;

import com.elms.dto.LeaveApprovalRequest;
import com.elms.dto.LeaveRejectionRequest;
import com.elms.entity.*;
import com.elms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final HolidayRepository holidayRepository;
    private final NotificationService notificationService;

    public LeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            LeaveTypeRepository leaveTypeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            HolidayRepository holidayRepository,
            NotificationService notificationService
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.holidayRepository = holidayRepository;
        this.notificationService = notificationService;
    }

    public List<LeaveRequest> getLeaveRequestsByEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        return leaveRequestRepository.findByEmployeeOrderByCreatedAtDesc(employee);
    }

    public LeaveRequest getLeaveRequestById(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found with id: " + id));
    }

    public List<LeaveRequest> getLeaveRequestsByManager(Long managerId, String authenticatedEmail, boolean isHr) {
        if (!isHr) {
            Employee authenticatedManager = getEmployeeForAuthenticatedUser(authenticatedEmail);
            if (!managerId.equals(authenticatedManager.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Managers can only view leave requests for their own team"
                );
            }
        }

        return leaveRequestRepository.findByEmployeeManagerIdOrderByCreatedAtDesc(managerId);
    }

    @Transactional
    public LeaveRequest createLeaveRequest(LeaveRequest leaveRequest) {
        validateRequest(leaveRequest);

        Employee employee = employeeRepository.findById(leaveRequest.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + leaveRequest.getEmployee().getId()));
        LeaveType leaveType = leaveTypeRepository.findById(leaveRequest.getLeaveType().getId())
                .orElseThrow(() -> new RuntimeException("Leave type not found with id: " + leaveRequest.getLeaveType().getId()));

        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);

        BigDecimal leaveDays = calculateWorkingDays(leaveRequest.getStartDate(), leaveRequest.getEndDate());
        if (leaveDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("No working days available in the selected date range");
        }
        leaveRequest.setLeaveDays(leaveDays);

        validateBalance(employee, leaveType, leaveDays);
        validateNoOverlap(employee, leaveRequest.getStartDate(), leaveRequest.getEndDate());

        if (Boolean.TRUE.equals(leaveType.getApprovalRequired())) {
            leaveRequest.setStatus(LeaveRequestStatus.PENDING);
            if (employee.getManager() == null) {
                throw new RuntimeException("Manager assignment is required for this leave type");
            }
        } else {
            leaveRequest.setStatus(LeaveRequestStatus.AUTO_APPROVED);
        }

        leaveRequest.setCreatedAt(LocalDateTime.now());
        leaveRequest.setUpdatedAt(LocalDateTime.now());

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        if (savedRequest.getStatus() == LeaveRequestStatus.AUTO_APPROVED) {
            updateBalanceForAutoApproved(savedRequest);
            notifySickLeaveAutoApproved(savedRequest);
        } else if (savedRequest.getStatus() == LeaveRequestStatus.PENDING) {
            notifyManagerOfPendingLeave(savedRequest);
        }

        return savedRequest;
    }

    @Transactional
    public LeaveRequest cancelLeaveRequest(Long id) {
        LeaveRequest leaveRequest = getLeaveRequestById(id);

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new RuntimeException("Only PENDING leave requests can be cancelled");
        }

        leaveRequest.setStatus(LeaveRequestStatus.CANCELLED);
        leaveRequest.setUpdatedAt(LocalDateTime.now());
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        notifyCancellation(savedRequest);
        return savedRequest;
    }

    @Transactional
    public LeaveRequest approveLeaveRequest(
            Long id,
            LeaveApprovalRequest request,
            String authenticatedEmail,
            boolean isHr
    ) {
        LeaveRequest leaveRequest = getLeaveRequestById(id);

        validateManagerReviewEligibility(leaveRequest, request.getManagerId(), true);
        validateAuthenticatedReviewer(authenticatedEmail, isHr, request.getManagerId());

        Employee manager = employeeRepository.findById(request.getManagerId())
                .orElseThrow(() -> new RuntimeException("Manager not found with id: " + request.getManagerId()));

        leaveRequest.setStatus(LeaveRequestStatus.APPROVED);
        leaveRequest.setReviewedBy(manager);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setUpdatedAt(LocalDateTime.now());

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        updateBalanceForApproved(savedRequest);
        notifyApproval(savedRequest);
        return savedRequest;
    }

    @Transactional
    public LeaveRequest rejectLeaveRequest(
            Long id,
            LeaveRejectionRequest request,
            String authenticatedEmail,
            boolean isHr
    ) {
        LeaveRequest leaveRequest = getLeaveRequestById(id);

        validateManagerReviewEligibility(leaveRequest, request.getManagerId(), false);
        validateAuthenticatedReviewer(authenticatedEmail, isHr, request.getManagerId());

        if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
            throw new RuntimeException("Rejection reason is required");
        }

        Employee manager = employeeRepository.findById(request.getManagerId())
                .orElseThrow(() -> new RuntimeException("Manager not found with id: " + request.getManagerId()));

        leaveRequest.setStatus(LeaveRequestStatus.REJECTED);
        leaveRequest.setReviewedBy(manager);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setRejectionReason(request.getRejectionReason());
        leaveRequest.setUpdatedAt(LocalDateTime.now());

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        notifyRejection(savedRequest);
        return savedRequest;
    }

    private void validateManagerReviewEligibility(LeaveRequest leaveRequest, Long managerId, boolean isApproval) {
        if (managerId == null) {
            throw new RuntimeException("managerId is required");
        }

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new RuntimeException("Only PENDING leave requests can be " + (isApproval ? "approved" : "rejected"));
        }

        if (leaveRequest.getLeaveType() != null && !Boolean.TRUE.equals(leaveRequest.getLeaveType().getApprovalRequired())) {
            throw new RuntimeException("Sick Leave requests cannot be approved or rejected by a manager");
        }

        Employee employee = leaveRequest.getEmployee();
        if (employee == null || employee.getManager() == null) {
            throw new RuntimeException("The employee does not have an assigned manager");
        }

        if (!managerId.equals(employee.getManager().getId())) {
            throw new RuntimeException("The provided manager is not assigned to this employee");
        }
    }

    private void validateAuthenticatedReviewer(String authenticatedEmail, boolean isHr, Long managerId) {
        if (isHr) {
            return;
        }

        Employee authenticatedManager = getEmployeeForAuthenticatedUser(authenticatedEmail);
        if (!managerId.equals(authenticatedManager.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Managers can only review leave requests for their own team"
            );
        }
    }

    private Employee getEmployeeForAuthenticatedUser(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Authenticated user not found"));

        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Authenticated user is not linked to an employee record"
                ));
    }

    private void validateRequest(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee() == null || leaveRequest.getEmployee().getId() == null) {
            throw new RuntimeException("employeeId is required");
        }
        if (leaveRequest.getLeaveType() == null || leaveRequest.getLeaveType().getId() == null) {
            throw new RuntimeException("leaveTypeId is required");
        }
        if (leaveRequest.getStartDate() == null || leaveRequest.getEndDate() == null) {
            throw new RuntimeException("startDate and endDate are required");
        }
        if (leaveRequest.getStartDate().isAfter(leaveRequest.getEndDate())) {
            throw new RuntimeException("startDate must not be after endDate");
        }
        if (leaveRequest.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("startDate cannot be in the past");
        }
        if (leaveRequest.getReason() == null || leaveRequest.getReason().trim().isEmpty()) {
            throw new RuntimeException("reason is required");
        }
    }

    private BigDecimal calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        BigDecimal workingDays = BigDecimal.ZERO;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (!isWeekend(current) && !isHoliday(current)) {
                workingDays = workingDays.add(BigDecimal.ONE);
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private boolean isHoliday(LocalDate date) {
        return holidayRepository.findByHolidayDateBetween(date, date).size() > 0;
    }

    private void validateBalance(Employee employee, LeaveType leaveType, BigDecimal requestedDays) {
        if (!Boolean.TRUE.equals(leaveType.getApprovalRequired())) {
            return;
        }

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new RuntimeException("Leave balance not found for employee and leave type"));

        BigDecimal remainingDays = balance.getRemainingDays();
        if (requestedDays.compareTo(remainingDays) > 0) {
            throw new RuntimeException("Insufficient leave balance");
        }
    }

    private void validateNoOverlap(Employee employee, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequestStatus> blockingStatuses = Arrays.asList(
                LeaveRequestStatus.PENDING,
                LeaveRequestStatus.APPROVED,
                LeaveRequestStatus.AUTO_APPROVED
        );

        List<LeaveRequest> overlappingRequests = leaveRequestRepository.findOverlappingRequests(employee, startDate, endDate, blockingStatuses);
        if (!overlappingRequests.isEmpty()) {
            throw new RuntimeException("Leave request overlaps with an existing request");
        }
    }

    private void notifyManagerOfPendingLeave(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee() == null || leaveRequest.getEmployee().getManager() == null) {
            return;
        }
        notificationService.createNotification(
                leaveRequest.getEmployee().getManager().getUser(),
                NotificationType.LEAVE_SUBMITTED,
                leaveRequest.getEmployee().getName() + " submitted a leave request."
        );
    }

    private void notifyApproval(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee() == null || leaveRequest.getEmployee().getUser() == null) {
            return;
        }
        notificationService.createNotification(
                leaveRequest.getEmployee().getUser(),
                NotificationType.LEAVE_APPROVED,
                "Your leave request was approved."
        );
    }

    private void notifyRejection(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee() == null || leaveRequest.getEmployee().getUser() == null) {
            return;
        }
        String message = "Your leave request was rejected.";
        if (leaveRequest.getRejectionReason() != null && !leaveRequest.getRejectionReason().trim().isEmpty()) {
            message = "Your leave request was rejected. Reason: " + leaveRequest.getRejectionReason();
        }
        notificationService.createNotification(
                leaveRequest.getEmployee().getUser(),
                NotificationType.LEAVE_REJECTED,
                message
        );
    }

    private void notifyCancellation(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee() == null || leaveRequest.getEmployee().getUser() == null) {
            return;
        }
        notificationService.createNotification(
                leaveRequest.getEmployee().getUser(),
                NotificationType.LEAVE_CANCELLED,
                "Your leave request was cancelled."
        );
    }

    private void notifySickLeaveAutoApproved(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee() == null || leaveRequest.getEmployee().getManager() == null) {
            return;
        }
        notificationService.createNotification(
                leaveRequest.getEmployee().getManager().getUser(),
                NotificationType.SICK_LEAVE_AUTO_APPROVED,
                leaveRequest.getEmployee().getName() + "'s sick leave was auto-approved."
        );
    }

    private void updateBalanceForAutoApproved(LeaveRequest leaveRequest) {
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeAndLeaveType(leaveRequest.getEmployee(), leaveRequest.getLeaveType())
                .orElseThrow(() -> new RuntimeException("Leave balance not found for employee and leave type"));

        balance.setUsedDays(balance.getUsedDays().add(leaveRequest.getLeaveDays()));
        balance.setUpdatedAt(LocalDateTime.now());
        leaveBalanceRepository.save(balance);
    }

    private void updateBalanceForApproved(LeaveRequest leaveRequest) {
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeAndLeaveType(leaveRequest.getEmployee(), leaveRequest.getLeaveType())
                .orElseThrow(() -> new RuntimeException("Leave balance not found for employee and leave type"));

        balance.setUsedDays(balance.getUsedDays().add(leaveRequest.getLeaveDays()));
        balance.setUpdatedAt(LocalDateTime.now());
        leaveBalanceRepository.save(balance);
    }
}
