package com.elms.service;

import com.elms.dto.EmployeeCreateRequest;
import com.elms.dto.EmployeeResponse;
import com.elms.dto.EmployeeSummaryResponse;
import com.elms.dto.EmployeeUpdateRequest;
import com.elms.entity.Employee;
import com.elms.entity.Role;
import com.elms.entity.User;
import com.elms.exception.ResourceNotFoundException;
import com.elms.repository.EmployeeRepository;
import com.elms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        validateCreateRequest(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new IllegalArgumentException("Employee code already exists");
        }

        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            if (manager.getUser() == null || manager.getUser().getRole() != Role.MANAGER) {
                throw new IllegalArgumentException("Selected manager is not a MANAGER");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.save(user);

        Employee employee = new Employee();
        employee.setUser(user);
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setName(request.getName());
        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment());
        employee.setDesignation(request.getDesignation());
        employee.setManager(manager);
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        employee = employeeRepository.save(employee);

        return toResponse(employee);
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (request.getEmployeeCode() != null && !request.getEmployeeCode().equals(employee.getEmployeeCode())) {
            if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
                throw new IllegalArgumentException("Employee code already exists");
            }
            employee.setEmployeeCode(request.getEmployeeCode());
        }

        if (request.getName() != null) {
            employee.setName(request.getName());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getDepartment() != null) {
            employee.setDepartment(request.getDepartment());
        }
        if (request.getDesignation() != null) {
            employee.setDesignation(request.getDesignation());
        }

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            if (manager.getUser() == null || manager.getUser().getRole() != Role.MANAGER) {
                throw new IllegalArgumentException("Selected manager is not a MANAGER");
            }
            if (employee.getId().equals(manager.getId())) {
                throw new IllegalArgumentException("Employee cannot be their own manager");
            }
            employee.setManager(manager);
        }

        employee.setUpdatedAt(LocalDateTime.now());
        employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse enableEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        User user = employee.getUser();
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        employee.setUpdatedAt(user.getUpdatedAt());
        employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse disableEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        User user = employee.getUser();
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        employee.setUpdatedAt(user.getUpdatedAt());
        employeeRepository.save(employee);
        return toResponse(employee);
    }

    private void validateCreateRequest(EmployeeCreateRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (request.getRole() != Role.EMPLOYEE && request.getRole() != Role.MANAGER) {
            throw new IllegalArgumentException("Invalid role for employee creation");
        }
        if (request.getEmployeeCode() == null || request.getEmployeeCode().isBlank()) {
            throw new IllegalArgumentException("Employee code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getDepartment() == null || request.getDepartment().isBlank()) {
            throw new IllegalArgumentException("Department is required");
        }
        if (request.getDesignation() == null || request.getDesignation().isBlank()) {
            throw new IllegalArgumentException("Designation is required");
        }
    }

    private EmployeeResponse toResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setEmail(employee.getUser() != null ? employee.getUser().getEmail() : null);
        response.setRole(employee.getUser() != null ? employee.getUser().getRole() : null);
        response.setEnabled(employee.getUser() != null ? employee.getUser().getEnabled() : null);
        response.setEmployeeCode(employee.getEmployeeCode());
        response.setName(employee.getName());
        response.setPhone(employee.getPhone());
        response.setDepartment(employee.getDepartment());
        response.setDesignation(employee.getDesignation());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());

        if (employee.getManager() != null) {
            EmployeeSummaryResponse managerResponse = new EmployeeSummaryResponse();
            managerResponse.setId(employee.getManager().getId());
            managerResponse.setEmployeeCode(employee.getManager().getEmployeeCode());
            managerResponse.setName(employee.getManager().getName());
            managerResponse.setDepartment(employee.getManager().getDepartment());
            managerResponse.setDesignation(employee.getManager().getDesignation());
            response.setManager(managerResponse);
        }

        return response;
    }
}
