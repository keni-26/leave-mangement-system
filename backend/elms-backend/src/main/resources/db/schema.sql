
-- ============================================================
-- ELMS DATABASE SCHEMA
-- Employee Leave Management System
--
-- Source of Truth:
-- Database_Design.md
--
-- Database:
-- PostgreSQL
-- ============================================================


-- ============================================================
-- 1. USERS TABLE
-- ============================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_user_role
        CHECK (role IN ('EMPLOYEE', 'MANAGER', 'HR'))
);


-- ============================================================
-- 2. EMPLOYEES TABLE
-- ============================================================

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    department VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    manager_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_employee_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_employee_manager
        FOREIGN KEY (manager_id)
        REFERENCES employees(id),

    CONSTRAINT chk_employee_not_own_manager
        CHECK (manager_id IS NULL OR manager_id <> id)
);


-- ============================================================
-- 3. LEAVE TYPES TABLE
-- ============================================================

CREATE TABLE leave_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    allocated_days DECIMAL(5,2) NOT NULL,
    approval_required BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);


-- ============================================================
-- 4. LEAVE BALANCES TABLE
-- ============================================================

CREATE TABLE leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    allocated_days DECIMAL(5,2) NOT NULL,
    used_days DECIMAL(5,2) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_leave_balance_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id),

    CONSTRAINT fk_leave_balance_leave_type
        FOREIGN KEY (leave_type_id)
        REFERENCES leave_types(id),

    CONSTRAINT uq_employee_leave_type
        UNIQUE (employee_id, leave_type_id)
);


-- ============================================================
-- 5. LEAVE REQUESTS TABLE
-- ============================================================

CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    leave_days DECIMAL(5,2) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    rejection_reason TEXT,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_leave_request_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id),

    CONSTRAINT fk_leave_request_leave_type
        FOREIGN KEY (leave_type_id)
        REFERENCES leave_types(id),

    CONSTRAINT fk_leave_request_reviewer
        FOREIGN KEY (reviewed_by)
        REFERENCES employees(id),

    CONSTRAINT chk_leave_request_status
        CHECK (
            status IN (
                'PENDING',
                'APPROVED',
                'REJECTED',
                'CANCELLED',
                'AUTO_APPROVED'
            )
        ),

    CONSTRAINT chk_leave_request_dates
        CHECK (start_date <= end_date),

    CONSTRAINT chk_leave_request_leave_days
        CHECK (leave_days > 0)
);


-- ============================================================
-- 6. HOLIDAYS TABLE
-- ============================================================

CREATE TABLE holidays (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    holiday_date DATE NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);


-- ============================================================
-- 7. NOTIFICATIONS TABLE
-- ============================================================

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT chk_notification_type
        CHECK (
            type IN (
                'LEAVE_SUBMITTED',
                'LEAVE_APPROVED',
                'LEAVE_REJECTED',
                'LEAVE_CANCELLED',
                'SICK_LEAVE_AUTO_APPROVED'
            )
        )
);


-- ============================================================
-- 8. INITIAL LEAVE TYPES
-- ============================================================
--
-- Sick Leave:
-- approval_required = FALSE
--
-- This means Sick Leave is automatically approved.
--
-- The actual AUTO_APPROVED workflow is handled by the
-- Spring Boot service layer, not by the database.
-- ============================================================

INSERT INTO leave_types
    (name, description, allocated_days, approval_required, active, created_at, updated_at)
VALUES
    (
        'Casual Leave',
        'Leave for personal or casual purposes',
        12.00,
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Sick Leave',
        'Leave taken due to illness or health reasons',
        12.00,
        FALSE,
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Earned Leave',
        'Leave earned by employees based on company policy',
        15.00,
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Maternity Leave',
        'Leave provided for maternity purposes',
        180.00,
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Loss of Pay',
        'Leave taken when no paid leave balance is available',
        0.00,
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
