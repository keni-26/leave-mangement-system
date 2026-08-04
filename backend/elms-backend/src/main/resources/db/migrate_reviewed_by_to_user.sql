-- Manual one-time migration for an existing ELMS database.
--
-- Source schema constraint name: fk_leave_request_reviewer.
-- Confirm the deployed database uses that name before executing. This file is
-- not executed automatically by Spring Boot.
--
-- PostgreSQL cannot update reviewed_by to users.id values while the old foreign
-- key still references employees.id, so the old constraint is dropped first.

BEGIN;

ALTER TABLE leave_requests
    DROP CONSTRAINT fk_leave_request_reviewer;

UPDATE leave_requests lr
SET reviewed_by = e.user_id
FROM employees e
WHERE lr.reviewed_by = e.id;

ALTER TABLE leave_requests
    ADD CONSTRAINT fk_leave_request_reviewer
    FOREIGN KEY (reviewed_by)
    REFERENCES users(id);

COMMIT;
