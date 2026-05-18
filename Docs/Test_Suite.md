# Test Suite

Document listing the tests currently available in the project.

## Overview

- Validated total suite: 244 tests
- Coverage by layer: unit, controller, integration, and end-to-end

Note: some runners show more entries because of nested classes and separate reports, but the effective Maven total was 244.

## Authentication

- [src/test/java/com/desofs/auth/AuthServiceTest.java](../src/test/java/com/desofs/auth/AuthServiceTest.java)
- [src/test/java/com/desofs/auth/AuthServiceRefreshTest.java](../src/test/java/com/desofs/auth/AuthServiceRefreshTest.java)
- [src/test/java/com/desofs/auth/RefreshTokenServiceTest.java](../src/test/java/com/desofs/auth/RefreshTokenServiceTest.java)
- [src/test/java/com/desofs/auth/AuthControllerIT.java](../src/test/java/com/desofs/auth/AuthControllerIT.java)

## Projects

- [src/test/java/com/desofs/project/ProjectServiceTest.java](../src/test/java/com/desofs/project/ProjectServiceTest.java)
- [src/test/java/com/desofs/project/ProjectMemberServiceTest.java](../src/test/java/com/desofs/project/ProjectMemberServiceTest.java)
- [src/test/java/com/desofs/project/ProjectControllerIT.java](../src/test/java/com/desofs/project/ProjectControllerIT.java)

## Tasks

- [src/test/java/com/desofs/task/TaskStatusTest.java](../src/test/java/com/desofs/task/TaskStatusTest.java)
- [src/test/java/com/desofs/task/TaskServiceTest.java](../src/test/java/com/desofs/task/TaskServiceTest.java)
- [src/test/java/com/desofs/task/TaskControllerTest.java](../src/test/java/com/desofs/task/TaskControllerTest.java)
- [src/test/java/com/desofs/task/TaskControllerIT.java](../src/test/java/com/desofs/task/TaskControllerIT.java)

## Attachments

- [src/test/java/com/desofs/attachment/AttachmentServiceTest.java](../src/test/java/com/desofs/attachment/AttachmentServiceTest.java)
- [src/test/java/com/desofs/attachment/AttachmentControllerTest.java](../src/test/java/com/desofs/attachment/AttachmentControllerTest.java)

## Comments

- [src/test/java/com/desofs/comment/CommentServiceTest.java](../src/test/java/com/desofs/comment/CommentServiceTest.java)
- [src/test/java/com/desofs/comment/CommentControllerTest.java](../src/test/java/com/desofs/comment/CommentControllerTest.java)

## Users

- [src/test/java/com/desofs/user/UserServiceTest.java](../src/test/java/com/desofs/user/UserServiceTest.java)

## End-to-end

- [src/test/java/com/desofs/e2e/ApplicationE2ETest.java](../src/test/java/com/desofs/e2e/ApplicationE2ETest.java)

## Notes

- Controller tests validate endpoints and HTTP status codes.
- Service tests validate business rules and permissions.
- Integration tests validate Spring behavior with a real context.
- The E2E test validates login, project creation, and task creation in a real flow.
