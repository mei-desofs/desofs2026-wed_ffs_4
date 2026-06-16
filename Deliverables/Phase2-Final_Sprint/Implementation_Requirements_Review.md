# Teuxdeux — Implementation Requirements Review

> **Project:** Teuxdeux — Task Management App  
> **Team ID:** wed_ffs_4  
> **Reviewed source:** [`src/`](../../src/)  
> **Baseline:** [`Deliverables/Phase1`](../Phase1/) analysis, architecture, security design, API specification, and security testing plan

---

## 1. Purpose

This report documents how well the current implementation follows the requirements and security design produced during Phase 1.

The review compares the implemented Spring Boot backend against:

- [01_Requirements.md](../Phase1/01_Requirements.md)
- [03_Architecture.md](../Phase1/03_Architecture.md)
- [04_SecurityDesign.md](../Phase1/04_SecurityDesign.md)
- [05_API_Specifications.md](../Phase1/05_API_Specifications.md)
- [06_SecurityTesting.md](../Phase1/06_SecurityTesting.md)

Overall, the implementation is a solid backend realization of the Phase 1 design. Most core functional requirements are present, the main security controls were implemented, and the project includes unit, integration, and end-to-end tests. Some differences remain, mostly around naming, exact response format, stricter or simplified authorization choices, and a few planned security details that were only partially implemented.

---

## 2. Overall Assessment

| Area                        | Status               | Summary                                                                                                                                          |
| --------------------------- | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| Functional scope            | Mostly implemented   | Authentication, users, projects, members, tasks, comments, attachments, and audit support are present.                                           |
| Architecture                | Implemented          | The backend follows the planned controller/service/repository layering.                                                                          |
| Security controls           | Mostly implemented   | JWT authentication, bcrypt password hashing, RBAC, project isolation, soft-delete filtering, file validation, and audit logging are implemented. |
| API contract                | Partially aligned    | Endpoint coverage is good, but response bodies do not consistently use the Phase 1 `success/data/error` envelope.                                |
| Testing                     | Strongly represented | The codebase contains service tests, controller tests, integration tests, E2E tests, and security-relevant negative cases.                       |
| Non-functional requirements | Partially evidenced  | ACID transactions and indexes exist in important areas, but performance/concurrency targets are not measured in the source.                      |

The implementation should be considered substantially aligned with Phase 1 for an academic delivery. The remaining gaps are mostly refinement items rather than failures of the main design.

---

## 3. Architecture Implementation

The implemented structure closely follows the layered design from Phase 1:

- Controllers expose REST endpoints:
  - [`AuthController`](../../src/main/java/com/desofs/auth/controller/AuthController.java)
  - [`ProjectController`](../../src/main/java/com/desofs/project/controller/ProjectController.java)
  - [`ProjectMemberController`](../../src/main/java/com/desofs/project/controller/ProjectMemberController.java)
  - [`TaskController`](../../src/main/java/com/desofs/task/controller/TaskController.java)
  - [`CommentController`](../../src/main/java/com/desofs/comment/controller/CommentController.java)
  - [`AttachmentController`](../../src/main/java/com/desofs/attachment/controller/AttachmentController.java)
- Services contain business and security decisions:
  - [`AuthService`](../../src/main/java/com/desofs/auth/service/AuthService.java)
  - [`ProjectService`](../../src/main/java/com/desofs/project/service/ProjectService.java)
  - [`TaskService`](../../src/main/java/com/desofs/task/service/TaskService.java)
  - [`CommentService`](../../src/main/java/com/desofs/comment/service/CommentService.java)
  - [`AttachmentService`](../../src/main/java/com/desofs/attachment/service/AttachmentService.java)
  - [`AuditService`](../../src/main/java/com/desofs/audit/service/AuditService.java)
- Repositories use Spring Data JPA, which gives parameterized database access by default:
  - project, task, comment, attachment, user, refresh token, token blacklist, and audit repositories.

This is consistent with the Phase 1 controller/service/repository architecture. The implementation also added useful operational pieces that were not fully detailed in Phase 1, such as refresh tokens, token blacklisting, global exception handling, and session termination endpoints.

---

## 4. Functional Requirements Coverage

### Authentication and Users

| Requirement                      | Status      | Evidence                                                                              |
| -------------------------------- | ----------- | ------------------------------------------------------------------------------------- |
| FR-1 Login                       | Implemented | `POST /auth/login` verifies credentials in `AuthService.login`.                       |
| FR-2 Token creation              | Implemented | JWT access tokens are generated by `JwtUtil.generateToken`.                           |
| FR-3 Token expiry and logout     | Implemented | JWT expiry is configured, logout blacklists access tokens and revokes refresh tokens. |
| FR-4 Role assignment by admin    | Implemented | `PUT /api/admin/users/{id}/role` is restricted to `ADMIN` in `SecurityConfig`.        |
| User lookup/update from API spec | Implemented | `GET/PUT /api/users/{id}` are present in `UserProfileController`.                     |
| Refresh token endpoint           | Implemented | `POST /auth/refresh` is present, improving on the base Phase 1 design.                |

Notes:

- Phase 1 uses the role name `MEMBER`, while the code uses `USER` for the normal member role. Functionally this role fills the member position, but the naming differs from the documents.
- The implemented token lifetime is 15 minutes in `application.yml`, which is stricter than the 24-hour example in Phase 1.

### Projects and Members

| Requirement                    | Status                         | Evidence                                                                                             |
| ------------------------------ | ------------------------------ | ---------------------------------------------------------------------------------------------------- |
| FR-5 Create project            | Implemented with stricter rule | Code allows only `ADMIN`, matching `01_Requirements.md`; the API spec had a broader `MANAGER+` note. |
| FR-6 List projects for members | Implemented                    | Non-admin users receive projects where they are members.                                             |
| FR-7 Update project            | Implemented                    | Admins and project-member managers can update projects.                                              |
| FR-8 Soft-delete project       | Implemented                    | Projects use a `deleted` flag and normal lookups exclude deleted projects.                           |
| FR-9 Manage members            | Implemented                    | Add, list, and remove member endpoints are present.                                                  |

Notes:

- The project model is simplified compared with the full Phase 1 database schema. It uses `deleted` rather than `deleted_at/deleted_by`.
- Project membership uses a direct many-to-many relation. The Phase 1 schema expected a richer `project_members` entity with fields such as `role_in_project` and `joined_at`. The current approach is simpler but sufficient for the implemented authorization checks.

### Tasks

| Requirement                  | Status      | Evidence                                                                   |
| ---------------------------- | ----------- | -------------------------------------------------------------------------- |
| FR-10 Create task in project | Implemented | `TaskService.createTask` validates project access and optional assignee.   |
| FR-11 List tasks per project | Implemented | Lists non-deleted project tasks and supports status filtering.             |
| FR-12 Update task            | Implemented | Task title, description, priority, and assignee can be updated.            |
| FR-13 Change task status     | Implemented | Status transition rules are enforced through `TaskStatus.canTransitionTo`. |
| FR-14 Soft-delete task       | Implemented | Tasks use a `deleted` flag and repository methods filter deleted tasks.    |
| FR-15 Assign task to member  | Implemented | Managers/admins can assign; members can self-assign unassigned tasks.      |

Notes:

- Task access includes project-membership checks, which supports the Phase 1 isolation requirements.
- Some task controller methods only catch `IllegalArgumentException`; service-level `AccessDeniedException` is handled globally, so the behavior is still protected, but response handling is not fully uniform inside the controller.

### Comments

| Requirement               | Status                         | Evidence                                                                                           |
| ------------------------- | ------------------------------ | -------------------------------------------------------------------------------------------------- |
| FR-16 Add/list comments   | Implemented                    | Project members can add and list task comments.                                                    |
| FR-16 Mentions            | Partially implemented          | `CommentService.extractMentions` exists, but there is no visible notification or mention workflow. |
| FR-17 Edit own comment    | Implemented, slightly extended | Authors can edit; managers/admins may also edit.                                                   |
| FR-18 Soft-delete comment | Implemented, slightly extended | Authors and managers/admins can soft-delete comments.                                              |

Notes:

- Comment length validation is implemented.
- The Phase 1 security design called for HTML output encoding on retrieval. The backend stores and returns text, but there is no explicit encoding step in the response DTO/service layer. In practice, final XSS safety also depends on the frontend rendering comments as text rather than raw HTML.

### Attachments

| Requirement                                 | Status      | Evidence                                                                              |
| ------------------------------------------- | ----------- | ------------------------------------------------------------------------------------- |
| FR-19 Upload file under 25MB with whitelist | Implemented | Size, extension, MIME type, filename, and image pixel checks are present.             |
| FR-20 List attachments per task             | Implemented | `GET /api/tasks/{taskId}/attachments`.                                                |
| FR-21 Download attachment for members only  | Implemented | Download checks project membership before serving the file.                           |
| FR-22 Delete attachment                     | Implemented | Uploader, manager, or admin may delete; file is removed and metadata is soft-deleted. |
| SR-14 Upload rate limiting                  | Implemented | Configurable per-user upload window is enforced.                                      |

This is one of the strongest matches to the Phase 1 design. The code stores generated filenames, rejects traversal attempts, validates MIME type against extension, serves files through authenticated endpoints, and records audit events for upload/download/delete.

---

## 5. Security Requirements Coverage

| Security requirement                     | Status                | Notes                                                                                                                                                           |
| ---------------------------------------- | --------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SR-1 Passwords hashed with bcrypt        | Implemented           | `BCryptPasswordEncoder` is configured. Default strength is 10, satisfying the Phase 1 minimum of salt/cost >= 10.                                               |
| Password complexity from security design | Partially implemented | Current policy is minimum 8 characters with at least one letter and digit plus forbidden-password checks; Phase 1 described 12 characters with more complexity. |
| SR-2 RBAC                                | Mostly implemented    | Spring Security route rules and service checks enforce admin/manager/user behavior.                                                                             |
| SR-3 Project isolation                   | Implemented           | Project, task, comment, and attachment services check project membership.                                                                                       |
| SR-4 Task isolation                      | Mostly implemented    | Status changes are limited to manager/admin or assignee; assignment rules are enforced.                                                                         |
| SR-5 File type validation                | Implemented           | Extension and MIME whitelist checks are present.                                                                                                                |
| SR-6 File size limit                     | Implemented           | 25MB configured in Spring multipart and attachment properties.                                                                                                  |
| SR-7 Input validation                    | Partially implemented | Important manual validation exists, but DTOs do not use a consistent Bean Validation approach.                                                                  |
| SR-8 Auth failures logged                | Implemented           | Failed login and lockout are audited.                                                                                                                           |
| SR-9 Soft-deleted data excluded          | Mostly implemented    | Project/task/comment/attachment normal queries exclude deleted records.                                                                                         |
| SR-10 Sensitive data not logged          | Mostly implemented    | Passwords/tokens are not intentionally logged; global errors include exception messages in audit details, so this should be kept under review.                  |
| SR-11 File access control                | Implemented           | File access is authenticated and project-scoped.                                                                                                                |
| SR-12 Parameterized queries              | Implemented           | Spring Data JPA and JPQL queries are used instead of string-concatenated SQL.                                                                                   |
| SR-13 Bearer JWT authentication          | Implemented           | Stateless JWT filter reads `Authorization: Bearer ...`; CSRF is disabled appropriately for this API style.                                                      |
| SR-14 Upload rate limit                  | Implemented           | `AttachmentService.enforceUploadRateLimit` implements the configured limit.                                                                                     |

Important security deviations:

- Phase 1 specified that the user role should be re-fetched from the database on every request. The current `JwtAuthFilter` uses the role claim from the JWT. Since the JWT signature is verified, attackers cannot simply edit the role without the secret, but role changes may not take effect until token expiry or session revocation.
- The JWT does not include all claims from the design example, such as `aud` or `kid`. The essential signature and expiry checks are present.
- The production profile enables TLS settings, but certificate provisioning and deployment-level HTTPS validation are outside the source-code review.

---

## 6. API Contract Alignment

The endpoint coverage is good and mostly matches the Phase 1 API specification:

- `/auth/login`, `/auth/logout`, `/auth/refresh`, plus `/auth/register`
- `/api/users/{id}`
- `/api/admin/users/{id}/role`
- `/api/projects`
- `/api/projects/{id}`
- `/api/projects/{projectId}/members`
- `/api/projects/{projectId}/tasks`
- `/api/tasks/{taskId}/comments`
- `/api/tasks/{taskId}/attachments`
- `/api/attachments/{id}/download`

The main contract differences are:

- Responses usually return direct DTOs or `{ "error": "..." }`, not the uniform Phase 1 envelope `{ "success": true, "data": ..., "message": ... }`.
- Some field names differ from the design, for example `name` instead of `title` for projects and `USER` instead of `MEMBER` for normal users.
- The implementation adds useful endpoints not emphasized in Phase 1, including registration, refresh token rotation, and session termination.

These differences are acceptable for the current backend as long as the documented API for evaluation reflects the implemented contract.

---

## 7. Non-Functional Requirements

| Requirement                         | Status                            | Notes                                                                                                                                          |
| ----------------------------------- | --------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| NFR-1 API response <500ms p95       | Not directly measured             | The source does not include performance test evidence.                                                                                         |
| NFR-2 Support 100+ concurrent users | Not directly measured             | Spring Boot/PostgreSQL can support this class of workload, but no load test evidence is included.                                              |
| NFR-3 Data consistency              | Implemented in important services | Transactional service methods are used in auth, task, comment, and attachment flows.                                                           |
| NFR-4 DDD/layered code quality      | Mostly implemented                | Domain areas are separated by package and layered by controller/service/repository.                                                            |
| NFR-5 Audit trail                   | Mostly implemented                | Authentication, projects, members, attachments, role changes, and unexpected errors are audited. Task/comment audit coverage is less complete. |
| NFR-6 Database indexes              | Partially implemented             | Task indexes are explicitly defined; other planned indexes are not all declared in entity annotations.                                         |

The implementation is reasonable for a course project backend. The main improvement would be adding measured evidence for performance and concurrency targets.

---

## 8. Testing and Verification

The source includes a broad automated test suite:

- Authentication tests:
  - [`AuthServiceTest`](../../src/test/java/com/desofs/auth/AuthServiceTest.java)
  - [`AuthControllerIT`](../../src/test/java/com/desofs/auth/AuthControllerIT.java)
  - refresh token tests
- Project tests:
  - [`ProjectServiceTest`](../../src/test/java/com/desofs/project/ProjectServiceTest.java)
  - [`ProjectMemberServiceTest`](../../src/test/java/com/desofs/project/ProjectMemberServiceTest.java)
  - [`ProjectControllerIT`](../../src/test/java/com/desofs/project/ProjectControllerIT.java)
- Task tests:
  - [`TaskServiceTest`](../../src/test/java/com/desofs/task/TaskServiceTest.java)
  - [`TaskControllerTest`](../../src/test/java/com/desofs/task/TaskControllerTest.java)
  - [`TaskControllerIT`](../../src/test/java/com/desofs/task/TaskControllerIT.java)
- Comment tests:
  - service, controller, and integration tests
- Attachment tests:
  - service, controller, and integration tests
- Audit tests:
  - [`AuditServiceTest`](../../src/test/java/com/desofs/audit/AuditServiceTest.java)
- End-to-end flow:
  - [`ApplicationE2ETest`](../../src/test/java/com/desofs/e2e/ApplicationE2ETest.java)

The tests cover many of the Phase 1 security testing goals, especially:

- invalid authentication behavior
- refresh token behavior
- authorization denial paths
- project/task isolation
- status transition validation
- file MIME mismatch rejection
- upload rate limiting
- attachment access and soft-delete behavior

Current local verification:

- Command: `mvn -B test`
- Result: **278 tests passed, 0 failures, 0 errors, 0 skipped**
- Date: 2026-06-16

Areas that would benefit from more explicit tests:

- JWT role-change behavior after a user role is updated
- tampered token rejection at controller level
- XSS payload handling through comments
- oversized multipart upload returning `413`
- direct storage-path access not being served
- performance/concurrency measurements

---

## 9. Summary of Main Gaps

The current implementation is close to the Phase 1 intent, but these items remain worth acknowledging:

1. **Role naming differs:** Phase 1 says `MEMBER`; implementation uses `USER`.
2. **JWT role is not reloaded from DB on every request:** this is the main security-design deviation.
3. **Password policy is weaker than the detailed Phase 1 security design:** bcrypt is used, but length/complexity is simpler.
4. **API responses are not wrapped in the planned uniform envelope.**
5. **Some schema details are simplified:** for example boolean soft-delete flags and simple many-to-many project membership.
6. **Audit coverage is good but not complete for every operation:** especially task/comment changes.
7. **NFR performance and concurrency targets are not backed by measured tests.**

These gaps are manageable and do not prevent the application from demonstrating the main design goals. They are mostly polish, consistency, and hardening tasks.

---

## 10. Final Conclusion

The implementation successfully delivers the main system described in Phase 1: a secure task-management backend with authentication, role-based access control, project-scoped data isolation, task workflows, comments, attachments, soft deletion, and audit logging.

The strongest implemented areas are:

- layered Spring Boot architecture
- authentication with JWT, refresh tokens, logout, and lockout
- project/task/comment/attachment access control
- file upload security
- soft-delete filtering
- automated unit/integration/E2E testing

The implementation does not match every Phase 1 detail exactly, but the deviations are understandable and mostly conservative. In several cases the implementation improves on the original design, such as refresh-token support, stricter JWT expiry, upload rate limiting, and session termination support.

For evaluation, the project can be presented as a substantially complete implementation of the Phase 1 requirements, with a clear list of follow-up improvements for full contract and security-design alignment.
