package com.elms.service;

import com.elms.entity.LeaveType;
import com.elms.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    // Get all active leave types
    public List<LeaveType> getAllActiveLeaveTypes() {
        return leaveTypeRepository.findByActiveTrue();
    }

    // Get leave type by ID
    public LeaveType getLeaveTypeById(Long id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Leave type not found with id: " + id));
    }

    // Create leave type
    public LeaveType createLeaveType(LeaveType leaveType) {

        if (leaveTypeRepository.existsByName(leaveType.getName())) {
            throw new RuntimeException(
                    "Leave type already exists: " + leaveType.getName()
            );
        }

        LocalDateTime now = LocalDateTime.now();

        leaveType.setCreatedAt(now);
        leaveType.setUpdatedAt(now);

        return leaveTypeRepository.save(leaveType);
    }

    // Update leave type
    public LeaveType updateLeaveType(Long id, LeaveType updatedLeaveType) {

        LeaveType existingLeaveType = getLeaveTypeById(id);

        existingLeaveType.setName(updatedLeaveType.getName());
        existingLeaveType.setDescription(updatedLeaveType.getDescription());
        existingLeaveType.setAllocatedDays(updatedLeaveType.getAllocatedDays());
        existingLeaveType.setApprovalRequired(updatedLeaveType.getApprovalRequired());
        existingLeaveType.setActive(updatedLeaveType.getActive());

        existingLeaveType.setUpdatedAt(LocalDateTime.now());

        return leaveTypeRepository.save(existingLeaveType);
    }

    // Deactivate leave type
    public void deactivateLeaveType(Long id) {

        LeaveType leaveType = getLeaveTypeById(id);

        leaveType.setActive(false);
        leaveType.setUpdatedAt(LocalDateTime.now());

        leaveTypeRepository.save(leaveType);
    }

    // Activate leave type
    public LeaveType activateLeaveType(Long id) {

        LeaveType leaveType = getLeaveTypeById(id);

        leaveType.setActive(true);
        leaveType.setUpdatedAt(LocalDateTime.now());

        return leaveTypeRepository.save(leaveType);
    }
}