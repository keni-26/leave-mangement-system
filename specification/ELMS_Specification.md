# Employee Leave Management System (ELMS)

## Software Specification — Version 1.0

**Project:** Employee Leave Management System (ELMS)
**Version:** 1.0
**Status:** Draft for Approval
**Development Approach:** Specification-Driven Development (SDD)

---

# 1. Project Overview

The Employee Leave Management System (ELMS) is a web-based application designed to manage employee leave requests and approval workflows.

The system allows:

* Employees to apply for leave and track leave status.
* Managers to review and manage employee leave requests.
* HR users to manage employees, leave types, holidays, and reports.

The application will provide role-based access and automated leave validation.

---

# 2. Project Goals

The main goals of the system are:

1. Digitize the employee leave application process.
2. Reduce manual leave management.
3. Allow employees to track leave balances.
4. Allow managers to approve or reject leave requests.
5. Automatically validate leave requests against defined rules.
6. Automatically update leave balances after approved leave.
7. Allow HR to manage employees, leave types, and holidays.
8. Provide notifications for important leave events.

---

# 3. Technology Stack

## Frontend

* React
* Vite
* JavaScript
* CSS / Bootstrap

## Backend

* Java 17
* Spring Boot
* Spring Web
* Spring Data JPA
* Hibernate
* Spring Security
* JWT Authentication
* Maven

## Database

* PostgreSQL 18

## Development Tools

* Visual Studio Code
* Git
* GitHub
* Postman

---

# 4. User Roles

The system contains three primary roles.

## 4.1 Employee

An Employee can:

* Login
* Logout
* Change password
* View profile
* Update basic profile information
* View available leave balance
* View used leave
* View remaining leave
* Apply for leave
* View leave history
* View leave status
* Cancel pending leave requests
* Receive notifications

---

## 4.2 Manager

A Manager can:

* Login
* Logout
* Change password
* View their assigned team
* View team employee information
* View pending leave requests
* Approve normal leave requests
* Reject normal leave requests
* Provide rejection reasons
* Receive notifications

A Manager cannot approve or reject Sick Leave requests.

Sick Leave is automatically approved according to the Sick Leave business rule.

---

## 4.3 HR

HR can:

* Login
* Logout
* Change password
* Add employees
* Edit employees
* Delete employees
* View employee list
* View employee information
* Assign managers
* Create leave types
* Edit leave types
* Delete leave types
* Add holidays
* Edit holidays
* Delete holidays
* View holiday calendar
* View leave reports
* Manage system settings

---

# 5. Authentication

The system shall provide:

* Login using email and password
* Logout
* Role-based access control
* Change password

Authentication will use:

* Spring Security
* JWT

Passwords must never be stored as plain text.

Passwords must be securely hashed before storage.

The backend must validate the user's role before allowing access to protected APIs.

---

# 6. Employee Management

Each employee shall have:

* Employee ID
* Name
* Email
* Phone Number
* Department
* Designation
* Assigned Manager

The system shall also maintain authentication information separately from employee profile information.

## 6.1 HR Operations

HR can:

* Add employee
* Edit employee
* Delete employee
* View employee list
* View employee information
* Assign or change manager

## 6.2 Employee Operations

An Employee can:

* View own profile
* Update permitted basic profile information

Employees cannot change:

* Employee ID
* Role
* Department
* Designation
* Assigned Manager

These fields are managed by HR.

## 6.3 Manager Operations

A Manager can view information of employees assigned to their team.

A Manager cannot modify employee information.

---

# 7. Leave Types

HR can:

* Create leave type
* Edit leave type
* Delete leave type
* Activate or deactivate leave type

Initial leave types:

1. Casual Leave
2. Sick Leave
3. Earned Leave
4. Maternity Leave
5. Loss of Pay

Each leave type shall have:

* Leave Type ID
* Name
* Description
* Allocated Days
* Approval Required
* Active/Inactive Status

---

# 8. Leave Balance

The system shall maintain leave balances for employees.

Each leave balance shall contain:

* Employee
* Leave Type
* Allocated Days
* Used Days
* Remaining Days

The basic calculation is:

```text
Remaining Days = Allocated Days - Used Days
```

The system shall automatically update the used and remaining balance after leave is approved.

For Sick Leave, the balance shall also be updated when the leave is automatically approved.

Rejected leave requests shall not affect leave balance.

Cancelled pending leave requests shall not affect leave balance.

---

# 9. Leave Application

An Employee can apply for leave by providing:

* Leave Type
* Start Date
* End Date
* Reason

The system shall calculate the number of applicable leave days.

Before creating the leave request, the system shall validate:

* Leave type exists
* Leave type is active
* Start date is valid
* End date is valid
* Start date is not after end date
* Leave does not violate leave balance rules
* Leave does not overlap with another active leave request
* Leave follows applicable leave policy rules

For normal leave requiring approval, a valid request shall initially have:

```text
PENDING
```

An Employee can cancel a leave request only while it is:

```text
PENDING
```

---

# 10. Leave Status

The system shall support the following leave statuses:

```text
PENDING
APPROVED
REJECTED
CANCELLED
AUTO_APPROVED
```

## Status Meaning

### PENDING

Leave request is waiting for Manager approval.

### APPROVED

Manager has approved the leave.

### REJECTED

Manager has rejected the leave.

A rejection reason must be stored.

### CANCELLED

Employee cancelled a pending leave request.

### AUTO_APPROVED

System automatically approved the leave without requiring Manager approval.

This status is primarily used for Sick Leave.

---

# 11. Normal Leave Approval Workflow

For leave types requiring Manager approval:

```text
Employee
    |
    | Apply Leave
    v
System Validation
    |
    v
PENDING
    |
    v
Manager Review
    |
    +-------------------+
    |                   |
    v                   v
APPROVED             REJECTED
    |                   |
    v                   v
Update Balance      Store Reason
    |                   |
    +---------+---------+
              |
              v
         Notify Employee
```

The Manager can:

* View pending requests from assigned team members.
* Approve a request.
* Reject a request.
* Enter a rejection reason.

---

# 12. Sick Leave Special Rule

Sick Leave does not require Manager approval.

When an Employee submits Sick Leave:

```text
Employee
    |
    | Apply Sick Leave
    v
System Validation
    |
    v
AUTO_APPROVED
    |
    +----------------------+
    |                      |
    v                      v
Update Balance       Notify Manager
    |
    v
Notify Employee
```

The Manager is informed about the Sick Leave but cannot approve or reject it.

The system shall:

1. Validate the Sick Leave request.
2. Check applicable Sick Leave balance rules.
3. Automatically approve the request.
4. Set status to `AUTO_APPROVED`.
5. Update the employee's Sick Leave balance.
6. Create an in-app notification for the employee.
7. Create an in-app notification for the assigned Manager.
8. Allow HR to view the leave record.

### Sick Leave Approval Requirement

```text
Approval Required = FALSE
```

---

# 13. Leave Validation Rules

## 13.1 Leave Type Validation

The selected leave type must exist and be active.

---

## 13.2 Date Validation

The start date must not be after the end date.

```text
Start Date <= End Date
```

---

## 13.3 Past Date Rule

For the MVP, employees cannot apply for leave starting on a past date.

---

## 13.4 Leave Balance Rule

For leave types with balance restrictions:

```text
Requested Days <= Remaining Days
```

If sufficient balance is unavailable, the request shall be rejected.

---

## 13.5 Overlapping Leave Rule

An employee cannot have overlapping active leave requests.

Active leave requests include:

* PENDING
* APPROVED
* AUTO_APPROVED

Cancelled and rejected requests do not block new leave applications.

---

## 13.6 Weekend Rule

For the MVP:

* Saturday is not counted as a leave day.
* Sunday is not counted as a leave day.

---

## 13.7 Holiday Rule

Company holidays are not counted as leave days.

---

## 13.8 Manager Assignment Rule

For normal leave requiring approval:

* Employee must have an assigned Manager.
* If no Manager is assigned, the leave request cannot be submitted.

For Sick Leave:

* Manager approval is not required.
* If a Manager is assigned, the Manager is notified.
* If no Manager is assigned, the Sick Leave can still be automatically approved and HR can view it.

---

# 14. Leave Day Calculation

For the MVP, leave days shall be calculated based on:

* Monday to Friday
* Excluding Saturday
* Excluding Sunday
* Excluding company holidays

Example:

```text
Leave Request:

Monday → Friday

Wednesday = Company Holiday

Applicable Leave Days = 4
```

The system will initially support full-day leave only.

Half-day leave is not included in MVP Version 1.0.

---

# 15. Holiday Management

HR can:

* Add holiday
* Edit holiday
* Delete holiday
* View holiday calendar

Each holiday shall contain:

* Holiday ID
* Holiday Name
* Holiday Date
* Description (optional)

A holiday shall not be counted as a leave day.

---

# 16. Notifications

The system shall support notifications for:

### Leave Submitted

For normal leave:

```text
Employee → Manager
```

### Leave Approved

```text
Manager → Employee
```

### Leave Rejected

```text
Manager → Employee
```

The rejection reason shall be included.

### Leave Cancelled

```text
Employee → Manager
```

### Sick Leave

```text
Employee
    ↓
Auto Approval Notification

Manager
    ↓
Information Notification
```

## Notification Types

The specification supports:

* In-app notifications
* Email notifications

### MVP Implementation

For the 3-day MVP, only **in-app notifications** will be implemented.

Email notification support may be added in a future version.

Each notification shall contain:

* Notification ID
* Recipient
* Message
* Notification Type
* Read/Unread Status
* Created Date

---

# 17. Leave Policy

The system shall support configurable leave policies through Leave Type configuration.

For MVP, the following rules apply:

* Leave type can be active or inactive.
* Leave type can have allocated days.
* Leave type can require or not require Manager approval.
* Sick Leave does not require Manager approval.
* Normal leave requires Manager approval.
* Leave balance is checked before approval where applicable.

The detailed leave allocation values will be configured by HR.

---

# 18. Role-Based Access

| Feature                  | Employee | Manager | HR   |
| ------------------------ | -------- | ------- | ---- |
| Login                    | Yes      | Yes     | Yes  |
| Logout                   | Yes      | Yes     | Yes  |
| Change Password          | Yes      | Yes     | Yes  |
| View Own Profile         | Yes      | Yes     | Yes  |
| Update Own Basic Profile | Yes      | Yes     | Yes  |
| Apply Leave              | Yes      | Yes*    | Yes* |
| View Own Leave           | Yes      | Yes     | Yes  |
| Cancel Pending Leave     | Yes      | Yes*    | Yes* |
| View Team                | No       | Yes     | Yes  |
| Approve Leave            | No       | Yes     | No   |
| Reject Leave             | No       | Yes     | No   |
| Manage Employees         | No       | No      | Yes  |
| Assign Manager           | No       | No      | Yes  |
| Manage Leave Types       | No       | No      | Yes  |
| Manage Holidays          | No       | No      | Yes  |
| View Reports             | No       | No      | Yes  |
| View Notifications       | Yes      | Yes     | Yes  |

`*` Manager and HR leave application is not a primary MVP workflow and may be restricted to Employee accounts in the first version.

---

# 19. Database Entities

The initial database shall contain the following major entities:

```text
User
Employee
LeaveType
LeaveBalance
LeaveRequest
Holiday
Notification
```

The detailed database structure will be defined in:

```text
Database_Design.md
```

The database shall use PostgreSQL.

Spring Data JPA and Hibernate shall be used for database access.

---

# 20. API Architecture

The backend shall expose REST APIs.

Initial API groups:

```text
/api/auth
/api/employees
/api/leave-types
/api/leave-balances
/api/leaves
/api/holidays
/api/notifications
/api/reports
```

Detailed API endpoints, request bodies, response bodies, and HTTP status codes will be defined in:

```text
API_Specification.md
```

---

# 21. Error Handling

The backend shall return appropriate HTTP status codes.

Examples:

```text
200 OK
201 CREATED
400 BAD REQUEST
401 UNAUTHORIZED
403 FORBIDDEN
404 NOT FOUND
409 CONFLICT
500 INTERNAL SERVER ERROR
```

Validation errors shall provide meaningful messages.

Example:

```text
"Insufficient leave balance"
"Leave dates overlap with an existing request"
"Manager is not assigned"
"Leave type is inactive"
"Start date cannot be after end date"
```

---

# 22. Security Requirements

The application shall:

* Use JWT-based authentication.
* Hash passwords.
* Protect authenticated endpoints.
* Restrict APIs based on user roles.
* Prevent Employees from accessing other Employees' private information.
* Prevent Managers from modifying employee records.
* Prevent Employees from approving or rejecting leave.
* Prevent Managers from approving or rejecting Sick Leave.
* Prevent unauthorized users from accessing HR functionality.

---

# 23. Non-Functional Requirements

## Performance

The application should respond to normal API requests within a reasonable time under normal MVP usage.

## Security

Authentication and authorization must be enforced on protected APIs.

## Maintainability

The backend should follow a layered architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

## Usability

The UI should be simple and easy to understand for Employees, Managers, and HR.

## Compatibility

The application should run on modern web browsers.

---

# 24. MVP Scope

The 3-day MVP shall prioritize:

### Priority 1 — Core

* Authentication
* Role-based access
* Employee management
* Leave types
* Leave balance
* Leave application
* Leave validation
* Normal leave approval
* Normal leave rejection
* Sick Leave auto-approval
* Holiday management

### Priority 2 — Supporting

* In-app notifications
* Employee profile
* Leave history
* Basic dashboard

### Priority 3 — If Time Allows

* Reports
* Email notifications
* Advanced dashboard
* Advanced system settings

---

# 25. Future Enhancements

The following are outside the initial MVP:

* Half-day leave
* Multiple-level approval
* Email notifications
* Mobile application
* Advanced analytics
* Attendance integration
* Payroll integration
* Calendar integration
* File/document attachment
* Medical certificate upload
* Audit logs
* Advanced HR reports

---

# 26. Specification Change Policy

This document is the primary source of truth for ELMS Version 1.0.

Any requirement change must follow:

```text
Requirement Change
        ↓
Update Specification
        ↓
Review Database Impact
        ↓
Review API Impact
        ↓
Update Backend
        ↓
Update Frontend
        ↓
Test
```

Code must not introduce business rules that are not defined in the specification unless the specification is updated.

---

# 27. Development Principle

ELMS will be developed using Specification-Driven Development.

The development flow is:

```text
Specification
      ↓
Business Rules
      ↓
Database Design
      ↓
API Specification
      ↓
Backend Implementation
      ↓
Frontend Implementation
      ↓
Testing
      ↓
Deployment
```

GitHub Copilot may be used as a coding assistant for implementation and boilerplate generation.

However, all generated code must follow the approved ELMS specification.

---

# 28. Version History

| Version | Status | Description                         |
| ------- | ------ | ----------------------------------- |
| 1.0     | Draft  | Initial complete ELMS specification |

---

# 29. Approval

**Specification Status:** Pending User Approval

Once approved, this document becomes the baseline source of truth for ELMS Version 1.0.
