# Test Suite

Document listing the tests currently available in the project and the latest local validation result.

## Overview

- Test command: `mvn -B test`
- Latest validation date: 2026-06-16
- Latest result: **278 tests passed**
- Failures: **0**
- Errors: **0**
- Skipped: **0**
- Coverage by layer: unit, controller, integration, and end-to-end
- Test runtime in latest local run: about 17 seconds

Note: Surefire reports nested JUnit classes separately, so the report directory contains more entries than the number of source test files. The effective Maven total from the latest run is 278 executed tests.

## Test Files By Area

### Authentication

- [AuthServiceTest.java](../../src/test/java/com/desofs/auth/AuthServiceTest.java)
- [AuthServiceRefreshTest.java](../../src/test/java/com/desofs/auth/AuthServiceRefreshTest.java)
- [RefreshTokenServiceTest.java](../../src/test/java/com/desofs/auth/RefreshTokenServiceTest.java)
- [AuthControllerIT.java](../../src/test/java/com/desofs/auth/AuthControllerIT.java)

Coverage:

- registration and password hashing behavior
- duplicate account rejection
- login success and failure behavior
- account lockout behavior
- refresh-token creation, validation, rotation, and expiry behavior
- logout/token revocation behavior through controller-level integration tests

### Projects

- [ProjectServiceTest.java](../../src/test/java/com/desofs/project/ProjectServiceTest.java)
- [ProjectMemberServiceTest.java](../../src/test/java/com/desofs/project/ProjectMemberServiceTest.java)
- [ProjectControllerIT.java](../../src/test/java/com/desofs/project/ProjectControllerIT.java)

Coverage:

- project creation, listing, retrieval, update, and soft delete
- admin/manager/member permission rules
- project isolation for non-members
- member add/list/remove behavior
- HTTP-level project behavior with the Spring test context

### Tasks

- [TaskStatusTest.java](../../src/test/java/com/desofs/task/TaskStatusTest.java)
- [TaskServiceTest.java](../../src/test/java/com/desofs/task/TaskServiceTest.java)
- [TaskControllerTest.java](../../src/test/java/com/desofs/task/TaskControllerTest.java)
- [TaskControllerIT.java](../../src/test/java/com/desofs/task/TaskControllerIT.java)

Coverage:

- task creation, listing, update, status change, assignment, and soft delete
- project-membership enforcement
- assignee validation
- manager/admin/member task permissions
- valid and invalid status transitions
- controller response behavior and integration behavior for task endpoints

### Comments

- [CommentServiceTest.java](../../src/test/java/com/desofs/comment/CommentServiceTest.java)
- [CommentControllerTest.java](../../src/test/java/com/desofs/comment/CommentControllerTest.java)
- [CommentControllerIntegrationTest.java](../../src/test/java/com/desofs/comment/CommentControllerIntegrationTest.java)

Coverage:

- comment creation, listing, editing, and soft delete
- project-membership checks before comment access
- author-only edit/delete rules with manager/admin extensions
- content validation, maximum length handling, and branch coverage
- mention extraction behavior
- controller and Spring integration behavior for comment endpoints

### Attachments

- [AttachmentServiceTest.java](../../src/test/java/com/desofs/attachment/AttachmentServiceTest.java)
- [AttachmentControllerTest.java](../../src/test/java/com/desofs/attachment/AttachmentControllerTest.java)
- [AttachmentControllerIT.java](../../src/test/java/com/desofs/attachment/AttachmentControllerIT.java)

Coverage:

- task-scoped upload, listing, download, and delete behavior
- project-member access control
- MIME type and extension validation
- upload rate-limit rejection
- image pixel-count validation
- file deletion plus metadata soft delete
- HTTP status behavior for upload/download/delete paths

### Users

- [UserServiceTest.java](../../src/test/java/com/desofs/user/UserServiceTest.java)

Coverage:

- role update validation
- username/email update behavior
- user-session termination support

### Audit

- [AuditServiceTest.java](../../src/test/java/com/desofs/audit/AuditServiceTest.java)

Coverage:

- audit event persistence
- anonymous actor fallback
- null resource-id fallback
- repository failure handling without breaking the application flow

### End-to-end

- [ApplicationE2ETest.java](../../src/test/java/com/desofs/e2e/ApplicationE2ETest.java)

Coverage:

- real application startup on a random port
- login with valid and invalid credentials
- authenticated project creation
- authenticated task creation
- basic retrieval of created project data

## Test Types

| Type | Files | Purpose |
| --- | ---: | --- |
| Unit tests | Service and model tests | Validate business rules, validation, permissions, and isolated branches. |
| Controller tests | Mocked controller tests | Validate endpoint behavior and HTTP responses without a full external server. |
| Integration tests | `*IT` and integration test classes | Validate behavior with Spring context, security filters, repositories, and request handling. |
| End-to-end tests | `ApplicationE2ETest` | Validate a real flow through the running application. |

## Security-Relevant Coverage

The current suite covers several controls from the Phase 1 security testing plan:

- invalid login returns unauthorized behavior
- repeated failed login attempts trigger lockout behavior
- refresh tokens are validated and rotated
- unauthorized project/task/comment/attachment access is rejected
- project membership is checked before accessing scoped resources
- task status transitions are constrained
- uploaded files are checked for extension and MIME type consistency
- upload rate limits are enforced
- deleted tasks, comments, and attachments are excluded from normal operations
- audit events are persisted for relevant flows

## Current Limitations

The suite is strong for service and API behavior, but these areas would still be useful additions if time allowed:

- explicit tampered-JWT controller test
- explicit expired-JWT controller test
- XSS payload test for comments through the HTTP layer
- oversized multipart upload test expecting `413 Payload Too Large`
- direct storage-path access test
- load or concurrency test for the 100+ concurrent-user non-functional requirement

## Latest Execution Evidence

Latest command:

```bash
mvn -B test
```

Latest Maven summary:

```text
Tests run: 278, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
