# Employee Leave Management System (ELMS)

## Database Design

**Project:** Employee Leave Management System
**Specification Version:** 1.0
**Database:** PostgreSQL 18
**ORM:** Spring Data JPA / Hibernate
**Status:** Approved

---

# 1. Database Overview

The ELMS database stores information related to:

* User authentication
* Employee profiles
* Manager assignments
* Leave types
* Employee leave balances
* Leave requests
* Company holidays
* Notifications

Database name:

```text
elms_db
```

---

# 2. Entity Relationship Overview

```text
                         ┌─────────────────┐
                         │      users      │
                         ├─────────────────┤
                         │ id (PK)         │
                         │ email           │
                         │ password        │
                         │ role            │
                         │ enabled         │
                         └────────┬────────┘
                                  │
                                  │ 1 : 1
                                  ▼
                         ┌─────────────────┐
                         │    employees    │
                         ├─────────────────┤
                         │ id (PK)         │
                         │ employee_code   │
                         │ name            │
                         │ phone           │
                         │ department      │
                         │ designation     │
                         │ manager_id (FK) │◄──────┐
                         └───────┬─────────┘       │
                                 │                 │
                  ┌──────────────┼──────────────┐  │
                  │              │              │  │
                  │              │              │  │
                  ▼              ▼              ▼  │
          ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
          │leave_balances│ │leave_requests│ │notifications │
          └──────┬───────┘ └──────┬───────┘ └──────────────┘
                 │                │
                 │                │
                 ▼                ▼
          ┌──────────────┐ ┌──────────────┐
          │  leave_types │ │  leave_types │
          └──────────────┘ └──────────────┘


          ┌──────────────┐
          │   holidays   │
          └──────────────┘
```

---

# 3. Tables

The database contains the following tables:

```text
users
employees
leave_types
leave_balances
leave_requests
holidays
notifications
```

---

# 4. Users Table

## Table Name

```text
users
```

Stores authentication and authorization information.

Employee profile information is stored separately in the `employees` table.

## Columns

| Column     | Data Type    | Constraints      | Description           |
| ---------- | ------------ | ---------------- | --------------------- |
| id         | BIGSERIAL    | PRIMARY KEY      | Unique user ID        |
| email      | VARCHAR(255) | NOT NULL, UNIQUE | Login email           |
| password   | VARCHAR(255) | NOT NULL         | Hashed password       |
| role       | VARCHAR(20)  | NOT NULL         | User role             |
| enabled    | BOOLEAN      | NOT NULL         | Account status        |
| created_at | TIMESTAMP    | NOT NULL         | Account creation time |
| updated_at | TIMESTAMP    | NOT NULL         | Last update time      |

## Role Values

```text
EMPLOYEE
MANAGER
HR
```

## Rules

* Email must be unique.
* Password must be stored as a secure hash.
* Role must be one of the supported roles.
* Disabled users cannot log in.

---

# 5. Employees Table

## Table Name

```text
employees
```

Stores employee profile and organizational information.

## Columns

| Column        | Data Type    | Constraints          | Description        |
| ------------- | ------------ | -------------------- | ------------------ |
| id            | BIGSERIAL    | PRIMARY KEY          | Employee record ID |
| user_id       | BIGINT       | NOT NULL, UNIQUE, FK | Related user       |
| employee_code | VARCHAR(50)  | NOT NULL, UNIQUE     | Employee ID        |
| name          | VARCHAR(150) | NOT NULL             | Employee name      |
| phone         | VARCHAR(20)  | NULL                 | Phone number       |
| department    | VARCHAR(100) | NOT NULL             | Department         |
| designation   | VARCHAR(100) | NOT NULL             | Job designation    |
| manager_id    | BIGINT       | NULL, FK             | Assigned manager   |
| created_at    | TIMESTAMP    | NOT NULL             | Creation time      |
| updated_at    | TIMESTAMP    | NOT NULL             | Last update time   |

## Foreign Keys

```text
user_id → users.id
manager_id → employees.id
```

## Manager Relationship

The `manager_id` column references the same `employees` table.

Example:

```text
Employee A
    manager_id
       │
       ▼
Employee B
    Manager
```

This allows one employee to be assigned to another employee who has the `MANAGER` role.

## Rules

* An employee must have one user account.
* Employee code must be unique.
* Email is stored in `users`, not duplicated in `employees`.
* Manager assignment is optional.
* The assigned manager must have the `MANAGER` role.
* An employee cannot be assigned as their own manager.

---

# 6. Leave Types Table

## Table Name

```text
leave_types
```

Stores configurable leave types managed by HR.

## Columns

| Column            | Data Type    | Constraints      | Description                          |
| ----------------- | ------------ | ---------------- | ------------------------------------ |
| id                | BIGSERIAL    | PRIMARY KEY      | Leave type ID                        |
| name              | VARCHAR(100) | NOT NULL, UNIQUE | Leave type name                      |
| description       | TEXT         | NULL             | Leave type description               |
| allocated_days    | DECIMAL(5,2) | NOT NULL         | Default allocated days               |
| approval_required | BOOLEAN      | NOT NULL         | Whether manager approval is required |
| active            | BOOLEAN      | NOT NULL         | Whether leave type is active         |
| created_at        | TIMESTAMP    | NOT NULL         | Creation time                        |
| updated_at        | TIMESTAMP    | NOT NULL         | Last update time                     |

## Initial Leave Types

```text
Casual Leave
Sick Leave
Earned Leave
Maternity Leave
Loss of Pay
```

## Sick Leave Rule

Sick Leave shall have:

```text
approval_required = FALSE
```

This means:

```text
Employee applies
      ↓
Validation
      ↓
AUTO_APPROVED
      ↓
Balance updated
      ↓
Manager notified
```

---

# 7. Leave Balances Table

## Table Name

```text
leave_balances
```

Stores the leave balance of each employee for each leave type.

## Columns

| Column         | Data Type    | Constraints  | Description      |
| -------------- | ------------ | ------------ | ---------------- |
| id             | BIGSERIAL    | PRIMARY KEY  | Balance ID       |
| employee_id    | BIGINT       | NOT NULL, FK | Employee         |
| leave_type_id  | BIGINT       | NOT NULL, FK | Leave type       |
| allocated_days | DECIMAL(5,2) | NOT NULL     | Allocated leave  |
| used_days      | DECIMAL(5,2) | NOT NULL     | Used leave       |
| created_at     | TIMESTAMP    | NOT NULL     | Creation time    |
| updated_at     | TIMESTAMP    | NOT NULL     | Last update time |

## Foreign Keys

```text
employee_id → employees.id
leave_type_id → leave_types.id
```

## Remaining Balance

Remaining days are calculated as:

```text
remaining_days = allocated_days - used_days
```

The MVP does not need to store `remaining_days` separately.

## Unique Constraint

An employee can have only one balance record for each leave type.

```text
UNIQUE(employee_id, leave_type_id)
```

## Rules

When a leave is approved:

```text
used_days = used_days + leave_days
```

When a leave is rejected:

```text
No balance change
```

When a pending leave is cancelled:

```text
No balance change
```

When Sick Leave is automatically approved:

```text
used_days = used_days + leave_days
```

---

# 8. Leave Requests Table

## Table Name

```text
leave_requests
```

Stores employee leave applications.

## Columns

| Column           | Data Type    | Constraints  | Description                |
| ---------------- | ------------ | ------------ | -------------------------- |
| id               | BIGSERIAL    | PRIMARY KEY  | Leave request ID           |
| employee_id      | BIGINT       | NOT NULL, FK | Employee applying          |
| leave_type_id    | BIGINT       | NOT NULL, FK | Selected leave type        |
| start_date       | DATE         | NOT NULL     | Leave start                |
| end_date         | DATE         | NOT NULL     | Leave end                  |
| leave_days       | DECIMAL(5,2) | NOT NULL     | Calculated applicable days |
| reason           | TEXT         | NOT NULL     | Leave reason               |
| status           | VARCHAR(20)  | NOT NULL     | Current leave status       |
| rejection_reason | TEXT         | NULL         | Reason for rejection       |
| reviewed_by      | BIGINT       | NULL, FK     | Authenticated user who reviewed |
| reviewed_at      | TIMESTAMP    | NULL         | Review time                |
| created_at       | TIMESTAMP    | NOT NULL     | Request creation time      |
| updated_at       | TIMESTAMP    | NOT NULL     | Last update time           |

## Foreign Keys

```text
employee_id → employees.id
leave_type_id → leave_types.id
reviewed_by → users.id
```

## Status Values

```text
PENDING
APPROVED
REJECTED
CANCELLED
AUTO_APPROVED
```

---

# 9. Leave Request Workflow

## Normal Leave

```text
Employee
    │
    ▼
Create Leave Request
    │
    ▼
Validate Request
    │
    ▼
PENDING
    │
    ▼
Manager Review
    │
    ├───────────────┐
    ▼               ▼
APPROVED         REJECTED
    │               │
    ▼               ▼
Update Balance   Store Reason
```

## Sick Leave

```text
Employee
    │
    ▼
Create Leave Request
    │
    ▼
Validate Request
    │
    ▼
AUTO_APPROVED
    │
    ├───────────────┐
    ▼               ▼
Update Balance   Notify Manager
```

---

# 10. Leave Request Rules

## Date Validation

```text
start_date <= end_date
```

## Past Date

The start date cannot be in the past.

## Balance Validation

For leave types with balance restrictions:

```text
leave_days <= remaining_days
```

## Overlap Validation

An employee cannot have overlapping leave requests with:

```text
PENDING
APPROVED
AUTO_APPROVED
```

Requests with:

```text
REJECTED
CANCELLED
```

do not block new requests.

## Manager Requirement

For normal leave:

```text
Employee must have a manager
```

For Sick Leave:

```text
Manager approval is not required
```

---

# 11. Holidays Table

## Table Name

```text
holidays
```

Stores company holidays.

## Columns

| Column       | Data Type    | Constraints      | Description          |
| ------------ | ------------ | ---------------- | -------------------- |
| id           | BIGSERIAL    | PRIMARY KEY      | Holiday ID           |
| name         | VARCHAR(150) | NOT NULL         | Holiday name         |
| holiday_date | DATE         | NOT NULL, UNIQUE | Holiday date         |
| description  | TEXT         | NULL             | Optional description |
| created_at   | TIMESTAMP    | NOT NULL         | Creation time        |
| updated_at   | TIMESTAMP    | NOT NULL         | Last update time     |

## Rules

* Holiday dates must be unique.
* Holidays are excluded from leave day calculation.
* HR can create, update, and delete holidays.

---

# 12. Notifications Table

## Table Name

```text
notifications
```

Stores in-app notifications.

## Columns

| Column     | Data Type   | Constraints  | Description            |
| ---------- | ----------- | ------------ | ---------------------- |
| id         | BIGSERIAL   | PRIMARY KEY  | Notification ID        |
| user_id    | BIGINT      | NOT NULL, FK | Notification recipient |
| message    | TEXT        | NOT NULL     | Notification message   |
| type       | VARCHAR(50) | NOT NULL     | Notification type      |
| is_read    | BOOLEAN     | NOT NULL     | Read status            |
| created_at | TIMESTAMP   | NOT NULL     | Creation time          |

## Foreign Key

```text
user_id → users.id
```

## Notification Types

```text
LEAVE_SUBMITTED
LEAVE_APPROVED
LEAVE_REJECTED
LEAVE_CANCELLED
SICK_LEAVE_AUTO_APPROVED
```

---

# 13. Entity Relationships

## User → Employee

```text
users 1 ─────── 1 employees
```

Each user has one employee profile.

Each employee has one user account.

---

## Employee → Manager

```text
employees 1 ─────── N employees
```

One manager can manage multiple employees.

Each employee can have one assigned manager.

This is implemented using:

```text
employees.manager_id → employees.id
```

---

## Employee → Leave Balance

```text
employees 1 ─────── N leave_balances
```

An employee can have multiple leave balances.

One balance exists for each leave type.

---

## Leave Type → Leave Balance

```text
leave_types 1 ─────── N leave_balances
```

One leave type can be assigned to many employees.

---

## Employee → Leave Request

```text
employees 1 ─────── N leave_requests
```

An employee can submit multiple leave requests.

---

## Leave Type → Leave Request

```text
leave_types 1 ─────── N leave_requests
```

One leave type can be used in many leave requests.

---

## Employee → Notifications

Notifications are linked to the `users` table.

```text
users 1 ─────── N notifications
```

A user can receive multiple notifications.

---

# 14. Complete Relationship Diagram

```text
                         ┌───────────────┐
                         │     users     │
                         └───────┬───────┘
                                 │
                                1:1
                                 │
                                 ▼
                         ┌───────────────┐
                  ┌──────│   employees   │◄─────────┐
                  │      └───────┬───────┘          │
                  │              │                  │
                  │              │                  │
                  │              │                  │
                  │              ▼                  │
                  │      ┌────────────────┐         │
                  │      │ leave_requests │         │
                  │      └───────┬────────┘         │
                  │              │                  │
                  │              │                  │
                  │              ▼                  │
                  │      ┌────────────────┐         │
                  │      │  leave_types   │         │
                  │      └───────┬────────┘         │
                  │              │                  │
                  │              ▼                  │
                  │      ┌────────────────┐         │
                  └─────►│ leave_balances │         │
                         └────────────────┘         │
                                                   │
                         Manager relationship       │
                         manager_id ────────────────┘


                         ┌────────────────┐
                         │    holidays    │
                         └────────────────┘


                         ┌────────────────┐
                         │ notifications  │
                         └───────┬────────┘
                                 │
                                 ▼
                              users
```

---

# 15. Database Constraints

The following constraints must be implemented.

## Users

```text
email UNIQUE
email NOT NULL
password NOT NULL
role NOT NULL
```

## Employees

```text
user_id UNIQUE
employee_code UNIQUE
user_id NOT NULL
employee_code NOT NULL
manager_id REFERENCES employees(id)
```

## Leave Types

```text
name UNIQUE
name NOT NULL
allocated_days NOT NULL
approval_required NOT NULL
active NOT NULL
```

## Leave Balances

```text
UNIQUE(employee_id, leave_type_id)
```

## Leave Requests

```text
employee_id NOT NULL
leave_type_id NOT NULL
start_date NOT NULL
end_date NOT NULL
leave_days NOT NULL
status NOT NULL
reason NOT NULL
```

## Holidays

```text
holiday_date UNIQUE
holiday_date NOT NULL
```

---

# 16. Database Indexes

The following indexes should be considered for performance.

```text
users.email

employees.employee_code

employees.manager_id

leave_balances.employee_id

leave_balances.leave_type_id

leave_requests.employee_id

leave_requests.status

leave_requests.start_date

leave_requests.end_date

notifications.user_id

notifications.is_read
```

PostgreSQL indexes may be created explicitly where needed.

---

# 17. Audit Timestamps

The following tables shall contain:

```text
created_at
updated_at
```

Tables:

```text
users
employees
leave_types
leave_balances
leave_requests
holidays
```

The `notifications` table requires only:

```text
created_at
```

---

# 18. Data Integrity Rules

The application must ensure:

1. An employee cannot be their own manager.
2. A manager assigned to an employee must have the `MANAGER` role.
3. A user email must be unique.
4. An employee code must be unique.
5. A leave type name must be unique.
6. A holiday date must be unique.
7. An employee can have only one balance per leave type.
8. Rejected leave cannot reduce leave balance.
9. Cancelled pending leave cannot reduce leave balance.
10. Approved leave must update leave balance.
11. Auto-approved Sick Leave must update leave balance.
12. Overlapping active leave requests must not be allowed.
13. Normal leave requires a manager.
14. Sick Leave does not require manager approval.
15. Sick Leave must use `AUTO_APPROVED` status.
16. Manager approval and rejection must not be allowed for Sick Leave.

---

# 19. Transaction Requirements

The following operations must be executed as database transactions.

## Normal Leave Approval

```text
BEGIN TRANSACTION
    │
    ├── Validate leave request
    │
    ├── Update leave request status
    │
    ├── Update leave balance
    │
    └── Create notification
    │
COMMIT
```

If any operation fails:

```text
ROLLBACK
```

---

## Sick Leave Auto Approval

```text
BEGIN TRANSACTION
    │
    ├── Validate leave request
    │
    ├── Set status = AUTO_APPROVED
    │
    ├── Update leave balance
    │
    ├── Create employee notification
    │
    └── Create manager notification
    │
COMMIT
```

If any operation fails:

```text
ROLLBACK
```

---

# 20. Database Creation

The development database shall be:

```text
elms_db
```

PostgreSQL configuration:

```text
Database: elms_db
Host: localhost
Port: 5432
```

The database username and password shall be configured locally and must not be committed to Git.

---

# 21. Environment Configuration

Database credentials should be provided using environment variables or local configuration.

Example:

```properties
DB_URL=jdbc:postgresql://localhost:5432/elms_db
DB_USERNAME=postgres
DB_PASSWORD=<local-password>
```

Actual passwords must never be committed to GitHub.

---

# 22. Database Initialization Strategy

For MVP development:

```text
Spring Boot
    ↓
Spring Data JPA
    ↓
Hibernate
    ↓
PostgreSQL
```

During early development, Hibernate may automatically create/update tables using:

```properties
spring.jpa.hibernate.ddl-auto=update
```

For production deployment, a database migration tool such as Flyway should be introduced.

---

# 23. Database Design Summary

The ELMS database consists of:

```text
users
    │
    │ 1:1
    ▼
employees
    │
    ├──────────────► employees (manager)
    │
    ├──────────────► leave_balances ──────► leave_types
    │
    └──────────────► leave_requests ───────► leave_types

users
    │
    └──────────────► notifications

holidays
```

The database design is derived from:

```text
ELMS Specification v1.0
```

Any future database changes must first be evaluated against the approved specification.

---

# 24. Version History

| Version | Status   | Description                                                  |
| ------- | -------- | ------------------------------------------------------------ |
| 1.0     | Approved | Initial database design derived from ELMS Specification v1.0 |
