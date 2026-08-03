# ELMS Implementation Status

## 1. Project Overview

The Employee Leave Management System (ELMS) is a web application for managing employee leave requests.

### Main Roles

* Employee
* Manager
* HR

### Backend Technology

* Java 17
* Spring Boot
* Spring Data JPA / Hibernate
* PostgreSQL
* Maven
* REST APIs

---

# 2. Implementation Status

## Database

| Feature                       | Status      |
| ----------------------------- | ----------- |
| PostgreSQL database `elms_db` | ✅ Completed |
| Users table                   | ✅ Completed |
| Employees table               | ✅ Completed |
| Leave Types table             | ✅ Completed |
| Leave Balances table          | ✅ Completed |
| Leave Requests table          | ✅ Completed |
| Holidays table                | ✅ Completed |
| Notifications table           | ✅ Completed |

---

## User and Employee Management

| Feature                       | Status      |
| ----------------------------- | ----------- |
| User entity                   | ✅ Completed |
| Employee entity               | ✅ Completed |
| User-Employee relationship    | ✅ Completed |
| Employee-Manager relationship | ✅ Completed |
| Manager self-reference        | ✅ Completed |
| User repository               | ✅ Completed |
| Employee repository           | ✅ Completed |

---

## Leave Type Management

| Feature                                | Status      |
| -------------------------------------- | ----------- |
| Leave Type entity                      | ✅ Completed |
| Leave Type repository                  | ✅ Completed |
| Leave Type service                     | ✅ Completed |
| Leave Type controller                  | ✅ Completed |
| Initial leave types                    | ✅ Completed |
| Sick Leave auto-approval configuration | ✅ Completed |

Initial leave types:

* Casual Leave
* Sick Leave
* Earned Leave
* Maternity Leave
* Loss of Pay

---

## Leave Balance Management

| Feature                                       | Status      |
| --------------------------------------------- | ----------- |
| Leave Balance entity                          | ✅ Completed |
| Leave Balance repository                      | ✅ Completed |
| Leave Balance service                         | ✅ Completed |
| Leave Balance controller                      | ✅ Completed |
| Employee balance lookup                       | ✅ Completed |
| Leave type balance lookup                     | ✅ Completed |
| Duplicate balance prevention                  | ✅ Completed |
| Remaining balance calculation                 | ✅ Completed |
| Balance update after approval                 | ✅ Tested    |
| Balance update after Sick Leave auto-approval | ✅ Tested    |
| No balance update after rejection             | ✅ Tested    |
| No balance update after cancellation          | ✅ Tested    |

---

## Leave Request Management

| Feature                  | Status      |
| ------------------------ | ----------- |
| Leave Request entity     | ✅ Completed |
| Leave Request repository | ✅ Completed |
| Leave Request service    | ✅ Completed |
| Leave Request controller | ✅ Completed |
| Create leave request     | ✅ Tested    |
| Start date validation    | ✅ Completed |
| End date validation      | ✅ Completed |
| Past date validation     | ✅ Completed |
| Working-day calculation  | ✅ Completed |
| Weekend exclusion        | ✅ Completed |
| Holiday exclusion        | ✅ Completed |
| Leave balance validation | ✅ Completed |
| Overlap validation       | ✅ Completed |
| Pending leave workflow   | ✅ Tested    |
| Sick Leave auto-approval | ✅ Tested    |
| Leave cancellation       | ✅ Tested    |
| Manager approval         | ✅ Tested    |
| Manager rejection        | ✅ Tested    |
| Rejection reason storage | ✅ Tested    |

---

## Leave Request Status

Supported statuses:

```text
PENDING
APPROVED
REJECTED
CANCELLED
AUTO_APPROVED
```

---

## Leave Request Workflow

### Normal Leave

```text
Employee
    ↓
Create Leave Request
    ↓
Validate Request
    ↓
PENDING
    ↓
Manager Review
    ├── APPROVED
    │      ↓
    │  Update Balance
    │
    └── REJECTED
           ↓
       Store Reason
```

### Sick Leave

```text
Employee
    ↓
Create Sick Leave Request
    ↓
Validate Request
    ↓
AUTO_APPROVED
    ↓
Update Balance
    ↓
Notify Manager
```

---

# 3. Notification System

| Feature                               | Status      |
| ------------------------------------- | ----------- |
| Notification entity                   | ✅ Completed |
| Notification repository               | ✅ Completed |
| Notification service                  | ✅ Completed |
| Notification controller               | ✅ Completed |
| Leave submitted notification          | ✅ Tested    |
| Leave approved notification           | ✅ Tested    |
| Leave rejected notification           | ✅ Tested    |
| Leave cancelled notification          | ✅ Tested    |
| Sick Leave auto-approved notification | ✅ Tested    |
| Get all user notifications            | ✅ Tested    |
| Get unread notifications              | ✅ Tested    |
| Mark notification as read             | ✅ Tested    |
| Mark all notifications as read        | ✅ Tested    |

Supported notification types:

```text
LEAVE_SUBMITTED
LEAVE_APPROVED
LEAVE_REJECTED
LEAVE_CANCELLED
SICK_LEAVE_AUTO_APPROVED
```

---

# 4. API Verification

The following APIs have been tested using Postman.

## Leave Types

```text
GET /api/leave-types
```

Status: ✅ Tested

---

## Leave Balances

```text
GET /api/leave-balances/employee/{employeeId}

GET /api/leave-balances/employee/{employeeId}/leave-type/{leaveTypeId}

POST /api/leave-balances/employee/{employeeId}/leave-type/{leaveTypeId}
```

Status: ✅ Tested

---

## Leave Requests

```text
POST /api/leave-requests

GET /api/leave-requests/employee/{employeeId}

GET /api/leave-requests/{id}

PUT /api/leave-requests/{id}/cancel

PUT /api/leave-requests/{id}/approve

PUT /api/leave-requests/{id}/reject
```

Status: ✅ Tested

---

## Notifications

```text
GET /api/notifications/user/{userId}

GET /api/notifications/user/{userId}/unread

PUT /api/notifications/{id}/read

PUT /api/notifications/user/{userId}/read-all
```

Status: ✅ Tested

---

# 5. Security Status

| Feature                  | Status    |
| ------------------------ | --------- |
| Password hashing         | ⏳ Pending |
| User login               | ⏳ Pending |
| JWT authentication       | ⏳ Pending |
| Role-based authorization | ⏳ Pending |
| Secure employee APIs     | ⏳ Pending |
| Secure manager APIs      | ⏳ Pending |
| Secure HR APIs           | ⏳ Pending |

---

# 6. HR Module Status

| Feature                        | Status    |
| ------------------------------ | --------- |
| Employee CRUD                  | ⏳ Pending |
| Create employee                | ⏳ Pending |
| Update employee                | ⏳ Pending |
| Disable employee               | ⏳ Pending |
| Leave type CRUD                | ⏳ Pending |
| Create leave type              | ⏳ Pending |
| Update leave type              | ⏳ Pending |
| Activate/deactivate leave type | ⏳ Pending |
| Holiday CRUD                   | ⏳ Pending |
| Add holiday                    | ⏳ Pending |
| Update holiday                 | ⏳ Pending |
| Delete holiday                 | ⏳ Pending |

---

# 7. Manager Module Status

| Feature                    | Status      |
| -------------------------- | ----------- |
| View team leave requests   | ⏳ Pending   |
| View pending team requests | ⏳ Pending   |
| Approve leave request      | ✅ Completed |
| Reject leave request       | ✅ Completed |
| View team leave history    | ⏳ Pending   |

---

# 8. Employee Module Status

| Feature                    | Status              |
| -------------------------- | ------------------- |
| Login                      | ⏳ Pending           |
| View profile               | ⏳ Pending           |
| View leave balance         | ✅ Backend completed |
| Apply for leave            | ✅ Backend completed |
| View own leave requests    | ✅ Backend completed |
| Cancel pending leave       | ✅ Backend completed |
| View notifications         | ✅ Backend completed |
| Mark notifications as read | ✅ Backend completed |

---

# 9. Frontend Status

| Feature                 | Status    |
| ----------------------- | --------- |
| React application setup | ⏳ Pending |
| Login page              | ⏳ Pending |
| Employee dashboard      | ⏳ Pending |
| Manager dashboard       | ⏳ Pending |
| HR dashboard            | ⏳ Pending |
| Leave application form  | ⏳ Pending |
| Leave history           | ⏳ Pending |
| Leave balance display   | ⏳ Pending |
| Manager approval UI     | ⏳ Pending |
| Manager rejection UI    | ⏳ Pending |
| HR management UI        | ⏳ Pending |
| Notification UI         | ⏳ Pending |

---

# 10. Testing Status

| Test Area                | Status   |
| ------------------------ | -------- |
| Application startup      | ✅ Passed |
| Database connection      | ✅ Passed |
| Maven compilation        | ✅ Passed |
| Leave type API           | ✅ Passed |
| Leave balance API        | ✅ Passed |
| Normal leave creation    | ✅ Passed |
| Manager approval         | ✅ Passed |
| Manager rejection        | ✅ Passed |
| Leave cancellation       | ✅ Passed |
| Sick Leave auto-approval | ✅ Passed |
| Balance update           | ✅ Passed |
| Notification creation    | ✅ Passed |
| Notification retrieval   | ✅ Passed |
| Notification read status | ✅ Passed |

---

# 11. Known Limitations

The current backend is still in development.

The following areas are not yet implemented:

1. Authentication
2. JWT security
3. Password hashing
4. Role-based authorization
5. Complete HR management APIs
6. Manager team-specific APIs
7. React frontend
8. Frontend-backend integration
9. Automated test suite
10. Production deployment

---

# 12. Current Development Phase

```text
Database Design
      ↓
Backend Core
      ↓
Leave Workflow
      ↓
Notification System
      ↓
CURRENT POSITION
      ↓
Authentication & Authorization
      ↓
HR APIs
      ↓
Manager APIs
      ↓
Frontend
      ↓
Integration Testing
      ↓
Deployment
```

---

# 13. Next Development Task

The next major development task is:

## Implement Authentication and Authorization

Required features:

1. Password hashing using BCrypt.
2. User login API.
3. JWT token generation.
4. JWT authentication filter.
5. Spring Security configuration.
6. Role-based authorization.
7. Employee access restrictions.
8. Manager access restrictions.
9. HR access restrictions.
10. Protect existing APIs.

The implementation must preserve all existing leave workflow and notification functionality.

Before modifying existing code, inspect the current entities, repositories, services, controllers, and `pom.xml`.

Do not change the existing database schema unless explicitly required.

After implementation:

1. Compile the project.
2. Start the Spring Boot application.
3. Test login using Postman.
4. Verify JWT token generation.
5. Verify invalid login rejection.
6. Verify role-based endpoint access.
7. Verify existing leave APIs still work.
8. Verify existing notification APIs still work.
