package com.elms.controller;

import com.elms.entity.LeaveType;
import com.elms.service.LeaveTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/leave-types")
@CrossOrigin(origins = "http://localhost:5173")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    // GET /api/leave-types
    @GetMapping
    public ResponseEntity<List<LeaveType>> getAllActiveLeaveTypes() {

        return ResponseEntity.ok(
                leaveTypeService.getAllActiveLeaveTypes()
        );
    }

    // GET /api/leave-types/{id}
    @GetMapping("/{id}")
    public ResponseEntity<LeaveType> getLeaveTypeById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                leaveTypeService.getLeaveTypeById(id)
        );
    }

    // POST /api/leave-types
    @PostMapping
    public ResponseEntity<LeaveType> createLeaveType(
            @Valid @RequestBody LeaveType leaveType
    ) {

        LeaveType createdLeaveType =
                leaveTypeService.createLeaveType(leaveType);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdLeaveType);
    }

    // PUT /api/leave-types/{id}
    @PutMapping("/{id}")
    public ResponseEntity<LeaveType> updateLeaveType(
            @PathVariable Long id,
            @Valid @RequestBody LeaveType leaveType
    ) {

        return ResponseEntity.ok(
                leaveTypeService.updateLeaveType(id, leaveType)
        );
    }

    // PUT /api/leave-types/{id}/activate
    @PutMapping("/{id}/activate")
    public ResponseEntity<LeaveType> activateLeaveType(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                leaveTypeService.activateLeaveType(id)
        );
    }

    // DELETE /api/leave-types/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateLeaveType(
            @PathVariable Long id
    ) {

        leaveTypeService.deactivateLeaveType(id);

        return ResponseEntity.noContent().build();
    }
}
