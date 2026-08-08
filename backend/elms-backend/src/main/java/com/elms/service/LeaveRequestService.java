package com.elms.service;

import com.elms.dto.LeaveApprovalRequest;
import com.elms.dto.LeaveRejectionRequest;
import com.elms.dto.LeaveRequestResponse;
import com.elms.exception.ResourceNotFoundException;
import com.elms.entity.Employee;
import com.elms.entity.LeaveBalance;
import com.elms.entity.LeaveRequest;
import com.elms.entity.LeaveRequestStatus;
import com.elms.entity.LeaveType;
import com.elms.entity.NotificationType;
import com.elms.entity.Role;
import com.elms.entity.User;
import com.elms.repository.EmployeeRepository;
import com.elms.repository.HolidayRepository;
import com.elms.repository.LeaveBalanceRepository;
import com.elms.repository.LeaveRequestRepository;
import com.elms.repository.LeaveTypeRepository;
import com.elms.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getLeaveRequestsByEmployee(Long employeeId, String authenticatedEmail) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        Employee employee = getEmployee(employeeId);
        ensureCanViewEmployeeRequests(authenticatedUser, employee);
        return leaveRequestRepository.findByEmployeeOrderByCreatedAtDesc(employee).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponse getLeaveRequestById(Long id, String authenticatedEmail) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        LeaveRequest leaveRequest = findLeaveRequestById(id);
        ensureCanViewEmployeeRequests(authenticatedUser, leaveRequest.getEmployee());
        return toResponse(leaveRequest);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getLeaveRequestsByManager(Long managerId, String authenticatedEmail) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        if (authenticatedUser.getRole() == Role.HR) {
            return leaveRequestRepository.findByEmployeeManagerIdOrderByCreatedAtDesc(managerId).stream().map(this::toResponse).toList();
        }

        if (authenticatedUser.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Only managers and HR can view team leave requests");
        }

        Employee authenticatedManager = getEmployeeForAuthenticatedUser(authenticatedUser);
        if (!managerId.equals(authenticatedManager.getId())) {
            throw new AccessDeniedException("Managers can only view leave requests for their own team");
        }
        return leaveRequestRepository.findByEmployeeManagerIdOrderByCreatedAtDesc(managerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getAllLeaveRequests(String authenticatedEmail) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        if (authenticatedUser.getRole() != Role.HR) {
            throw new AccessDeniedException("Only HR can view all leave requests");
        }
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public LeaveRequestResponse createLeaveRequest(LeaveRequest leaveRequest, String authenticatedEmail) {
        validateRequest(leaveRequest);

        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        Employee employee = getEmployee(leaveRequest.getEmployee().getId());
        if (authenticatedUser.getRole() != Role.HR) {
            Employee authenticatedEmployee = getEmployeeForAuthenticatedUser(authenticatedUser);
            if (!authenticatedEmployee.getId().equals(employee.getId())) {
                throw new AccessDeniedException("Users can only create leave requests for themselves");
            }
        }

        LeaveType leaveType = leaveTypeRepository.findById(leaveRequest.getLeaveType().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + leaveRequest.getLeaveType().getId()));
        if (!Boolean.TRUE.equals(leaveType.getActive())) {
            throw new IllegalArgumentException("Leave type is inactive and cannot be used for new leave requests.");
        }

        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);

        BigDecimal leaveDays = calculateWorkingDays(leaveRequest.getStartDate(), leaveRequest.getEndDate());
        if (leaveDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("No working days available in the selected date range");
        }
        leaveRequest.setLeaveDays(leaveDays);

        validateBalance(employee, leaveType, leaveDays);
        validateNoOverlap(employee, leaveRequest.getStartDate(), leaveRequest.getEndDate());

        if (Boolean.TRUE.equals(leaveType.getApprovalRequired())) {
            if (employee.getManager() == null) {
            throw new IllegalArgumentException("Manager assignment is required for this leave type");
            }
            leaveRequest.setStatus(LeaveRequestStatus.PENDING);
        } else {
            leaveRequest.setStatus(LeaveRequestStatus.AUTO_APPROVED);
        }

        LocalDateTime now = LocalDateTime.now();
        leaveRequest.setCreatedAt(now);
        leaveRequest.setUpdatedAt(now);
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        if (savedRequest.getStatus() == LeaveRequestStatus.AUTO_APPROVED) {
            updateBalance(savedRequest);
            notifyManagerOfAutoApprovedLeave(savedRequest);
        } else {
            notifyManagerOfPendingLeave(savedRequest);
        }
        return toResponse(savedRequest);
    }

    @Transactional
    public LeaveRequestResponse cancelLeaveRequest(Long id, String authenticatedEmail) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        LeaveRequest leaveRequest = findLeaveRequestById(id);

        if (authenticatedUser.getRole() == Role.HR) {
            throw new AccessDeniedException("HR cannot cancel employee leave requests through this endpoint");
        }

        Employee authenticatedEmployee = getEmployeeForAuthenticatedUser(authenticatedUser);
        if (!authenticatedEmployee.getId().equals(leaveRequest.getEmployee().getId())) {
            throw new AccessDeniedException("Only the employee who owns this leave request can cancel it");
        }
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING leave requests can be cancelled");
        }

        leaveRequest.setStatus(LeaveRequestStatus.CANCELLED);
        leaveRequest.setUpdatedAt(LocalDateTime.now());
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        notifyManagerOfCancellation(savedRequest);
        return toResponse(savedRequest);
    }

    @Transactional
    public LeaveRequestResponse approveLeaveRequest(
            Long id,
            LeaveApprovalRequest request,
            String authenticatedEmail
    ) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        LeaveRequest leaveRequest = findLeaveRequestById(id);
        validateReviewEligibility(leaveRequest, authenticatedUser, true);
        validateBalance(leaveRequest.getEmployee(), leaveRequest.getLeaveType(), leaveRequest.getLeaveDays());

        leaveRequest.setStatus(LeaveRequestStatus.APPROVED);
        leaveRequest.setReviewedBy(authenticatedUser);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setUpdatedAt(LocalDateTime.now());

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        updateBalance(savedRequest);
        notifyApproval(savedRequest);
        return toResponse(savedRequest);
    }

    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(
            Long id,
            LeaveRejectionRequest request,
            String authenticatedEmail
    ) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        LeaveRequest leaveRequest = findLeaveRequestById(id);
        validateReviewEligibility(leaveRequest, authenticatedUser, false);

        if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        leaveRequest.setStatus(LeaveRequestStatus.REJECTED);
        leaveRequest.setReviewedBy(authenticatedUser);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setRejectionReason(request.getRejectionReason().trim());
        leaveRequest.setUpdatedAt(LocalDateTime.now());

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        notifyRejection(savedRequest);
        return toResponse(savedRequest);
    }

    private void ensureCanViewEmployeeRequests(User authenticatedUser, Employee targetEmployee) {
        if (authenticatedUser.getRole() == Role.HR) {
            return;
        }

        Employee authenticatedEmployee = getEmployeeForAuthenticatedUser(authenticatedUser);
        if (authenticatedUser.getRole() == Role.EMPLOYEE
                && authenticatedEmployee.getId().equals(targetEmployee.getId())) {
            return;
        }
        if (authenticatedUser.getRole() == Role.MANAGER
                && targetEmployee.getManager() != null
                && authenticatedEmployee.getId().equals(targetEmployee.getManager().getId())) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to view these leave requests");
    }

    private void validateReviewEligibility(LeaveRequest leaveRequest, User authenticatedUser, boolean isApproval) {
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING leave requests can be " + (isApproval ? "approved" : "rejected"));
        }
        if (!Boolean.TRUE.equals(leaveRequest.getLeaveType().getApprovalRequired())) {
            throw new IllegalStateException("Only approval-required leave requests can be manually approved or rejected");
        }
        if (authenticatedUser.getRole() == Role.HR) {
            return;
        }
        if (authenticatedUser.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Only managers and HR can review leave requests");
        }

        Employee authenticatedManager = getEmployeeForAuthenticatedUser(authenticatedUser);
        Employee requestEmployee = leaveRequest.getEmployee();
        if (requestEmployee.getManager() == null
                || !authenticatedManager.getId().equals(requestEmployee.getManager().getId())) {
            throw new AccessDeniedException("Managers can only review leave requests for their direct reports");
        }
    }

    private User getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    private Employee getEmployeeForAuthenticatedUser(User user) {
        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("Authenticated employee not found"));
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
    }

    private LeaveRequest findLeaveRequestById(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    private void validateRequest(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee() == null || leaveRequest.getEmployee().getId() == null) {
            throw new IllegalArgumentException("employeeId is required");
        }
        if (leaveRequest.getLeaveType() == null || leaveRequest.getLeaveType().getId() == null) {
            throw new IllegalArgumentException("leaveTypeId is required");
        }
        if (leaveRequest.getStartDate() == null || leaveRequest.getEndDate() == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (leaveRequest.getStartDate().isAfter(leaveRequest.getEndDate())) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }
        if (leaveRequest.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("startDate cannot be in the past");
        }
        if (leaveRequest.getReason() == null || leaveRequest.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("reason is required");
        }
    }

    private BigDecimal calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> holidayDates = holidayRepository.findByHolidayDateBetween(startDate, endDate).stream()
                .map(holiday -> holiday.getHolidayDate())
                .collect(Collectors.toSet());

        BigDecimal workingDays = BigDecimal.ZERO;
        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
            if (!isWeekend(current) && !holidayDates.contains(current)) {
                workingDays = workingDays.add(BigDecimal.ONE);
            }
        }
        return workingDays;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private void validateBalance(Employee employee, LeaveType leaveType, BigDecimal requestedDays) {
        // Auto-approved leave still consumes its configured allocation. Loss of Pay is the
        // only configured leave type that intentionally has no balance limit.
        if (isLossOfPay(leaveType)) {
            return;
        }

        LeaveBalance balance = getLeaveBalance(employee, leaveType);
        if (requestedDays.compareTo(balance.getRemainingDays()) > 0) {
            throw new IllegalStateException("Insufficient leave balance");
        }
    }

    private boolean isLossOfPay(LeaveType leaveType) {
        return "Loss of Pay".equalsIgnoreCase(leaveType.getName())
                && leaveType.getAllocatedDays() != null
                && leaveType.getAllocatedDays().compareTo(BigDecimal.ZERO) == 0;
    }

    private LeaveBalance getLeaveBalance(Employee employee, LeaveType leaveType) {
        return leaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found for employee and leave type"));
    }

    private void validateNoOverlap(Employee employee, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequestStatus> blockingStatuses = Arrays.asList(
                LeaveRequestStatus.PENDING,
                LeaveRequestStatus.APPROVED,
                LeaveRequestStatus.AUTO_APPROVED
        );
        if (!leaveRequestRepository.findOverlappingRequests(employee, startDate, endDate, blockingStatuses).isEmpty()) {
            throw new IllegalStateException("Leave request overlaps with an existing request");
        }
    }

    private void notifyManagerOfPendingLeave(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee().getManager() != null) {
            notificationService.createNotification(
                    leaveRequest.getEmployee().getManager().getUser(),
                    NotificationType.LEAVE_SUBMITTED,
                    leaveRequest.getEmployee().getName() + " submitted a leave request."
            );
        }
    }

    private void notifyManagerOfCancellation(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee().getManager() != null) {
            notificationService.createNotification(
                    leaveRequest.getEmployee().getManager().getUser(),
                    NotificationType.LEAVE_CANCELLED,
                    leaveRequest.getEmployee().getName() + " cancelled their leave request."
            );
        }
    }

    private void notifyApproval(LeaveRequest leaveRequest) {
        notificationService.createNotification(
                leaveRequest.getEmployee().getUser(),
                NotificationType.LEAVE_APPROVED,
                "Your leave request was approved."
        );
    }

    private void notifyRejection(LeaveRequest leaveRequest) {
        String message = "Your leave request was rejected. Reason: " + leaveRequest.getRejectionReason();
        notificationService.createNotification(
                leaveRequest.getEmployee().getUser(),
                NotificationType.LEAVE_REJECTED,
                message
        );
    }

    private void notifyManagerOfAutoApprovedLeave(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee().getManager() != null) {
            notificationService.createNotification(
                    leaveRequest.getEmployee().getManager().getUser(),
                    NotificationType.SICK_LEAVE_AUTO_APPROVED,
                    leaveRequest.getEmployee().getName() + "'s " + leaveRequest.getLeaveType().getName()
                            + " request was auto-approved."
            );
        }
    }

    private void updateBalance(LeaveRequest leaveRequest) {
        LeaveBalance balance = getLeaveBalance(leaveRequest.getEmployee(), leaveRequest.getLeaveType());
        balance.setUsedDays(balance.getUsedDays().add(leaveRequest.getLeaveDays()));
        balance.setUpdatedAt(LocalDateTime.now());
        leaveBalanceRepository.save(balance);
    }

    private LeaveRequestResponse toResponse(LeaveRequest request) {
        LeaveRequestResponse response = new LeaveRequestResponse();
        response.setId(request.getId());
        response.setEmployeeId(request.getEmployee().getId());
        response.setEmployeeName(request.getEmployee().getName());
        response.setEmployeeCode(request.getEmployee().getEmployeeCode());
        response.setEmployeeEmail(request.getEmployee().getUser().getEmail());
        response.setManagerName(request.getEmployee().getManager() == null ? null : request.getEmployee().getManager().getName());
        response.setLeaveTypeId(request.getLeaveType().getId());
        response.setLeaveTypeName(request.getLeaveType().getName());
        response.setStartDate(request.getStartDate());
        response.setEndDate(request.getEndDate());
        response.setLeaveDays(request.getLeaveDays());
        response.setReason(request.getReason());
        response.setStatus(request.getStatus());
        response.setRejectionReason(request.getRejectionReason());
        response.setCreatedAt(request.getCreatedAt());
        response.setUpdatedAt(request.getUpdatedAt());
        response.setReviewedAt(request.getReviewedAt());
        response.setReviewedBy(request.getReviewedBy() == null ? null : employeeRepository.findByUserId(request.getReviewedBy().getId())
                .map(Employee::getName)
                .orElse(request.getReviewedBy().getEmail()));
        return response;
    }
}
