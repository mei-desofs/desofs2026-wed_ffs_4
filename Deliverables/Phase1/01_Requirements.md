# Requirements

## Functional Requirements

### Authentication
- FR-1: Login (username/password). Users authenticate with credentials.
- FR-2: Token creation. After authentication, system issues valid JWT.
- FR-3: Token expiry & logout. Tokens expire after configuration hours; blacklist is kept in the database; users can log out manually.
- FR-4: Role assignment (admin only). Only admins can assign roles (Admin/Manager/Member).

### Projects
- FR-5: Create project (admin). Admins create new projects.
- FR-6: List projects (member). Users see only projects where they are members.
- FR-7: Update project (admin, manager). Admins/managers can edit project details.
- FR-8: Soft-delete project (admin). Deleted projects don't appear in queries but data is preserved.
- FR-9: Manage members (add/remove). Admins/Managers add/remove members from their projects.

### Tasks
- FR-10: Create task in project (Manager+). Managers create tasks within projects.
- FR-11: List tasks per project. Members view tasks from their project.
- FR-12: Update task (manager). Manager can edit it.
- FR-13: Change task status (TODO → IN_PROGRESS → DONE). Status transitions follow defined pipeline. Both the manager and the assigned member can update the status of a task.
- FR-14: Soft-delete task (manager). Deleted tasks don't appear but data is preserved.
- FR-15: Assign task to member. Manager assigns tasks to specific project members. A member can assign himself to a task if it is unassigned.

### Comments
- FR-16: Add comment to task. Members add comments to project tasks. Members can mention other members in comments using @username 
- FR-17: Edit own comment. User edits only their own comments.
- FR-18: Soft-delete comment. Deleted comments disappear but data is preserved.

### Attachments
- FR-19: Upload file to task (<25MB, whitelist types). Files up to 25MB of approved types only.
- FR-20: List attachments per task. Members view files attached to the task.
- FR-21: Download attachment (members only). Only project members can download files.
- FR-22: Delete attachment. File is removed from storage. Only the manager and the member who uploaded the file can delete attachments.

---

## Non-Functional Requirements

- NFR-1: API response <500ms (95th percentile). Adequate performance for responsive experience.
- NFR-2: Support 100+ concurrent users. System scalable for multiple simultaneous users.
- NFR-3: Data consistency (ACID). Transactions ensure data integrity.
- NFR-4: Code quality (DDD pattern). Domain-Driven Design architecture for maintainability.
- NFR-5: Audit trail for all operations. All relevant events are recorded.
- NFR-6: Database indexes on frequently queried columns. Optimization for frequent access queries.

---

## Security Requirements

- SR-1: Passwords hashed (bcrypt, salt ≥10). Protects credentials against rainbow tables and brute force.
- SR-2: RBAC at API layer (Admin/Manager/Member). Role-based access control.
- SR-3: Project isolation (members see only own projects). Users cannot see other projects.
- SR-4: Task isolation. Members can only edit their own task status. Managers can edit all tasks within their projects.
- SR-5: File type validation (whitelist + MIME type). Only approved file types accepted.
- SR-6: File size limit 25MB. Protection against excessive uploads.
- SR-7: Input validation (XSS, SQLi prevention). All inputs validated against attacks.
- SR-8: All auth failures logged. Failed attempts recorded for security analysis.
- SR-9: Soft-delete data not accessible in queries. Deleted data does not return in searches.
- SR-10: Sensitive data never logged (passwords, tokens). Credentials never appear in logs.
- SR-11: File access control via authentication/authorization. Files accessible only to authenticated and authorized users.
- SR-12: Parameterized queries for all DB access. Prevents SQL injection in all queries.
- SR-13: All API requests authenticated via Bearer JWT in Authorization header. No cookie-based authentication used. Prevents CSRF attacks.
- SR-14: There is an upload configured number of files per configured time period (e.g., 10 files per hour)
---
