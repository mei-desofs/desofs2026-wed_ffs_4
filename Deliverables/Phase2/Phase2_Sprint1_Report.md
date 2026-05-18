# Teuxdeux — Phase 2 Sprint 1 Report

> **Project:** Teuxdeux — Task Management App  
> **Team ID:** wed_ffs_4

---

## 1. Sprint Goal

This sprint focused on stabilizing the backend, aligning the API with the requirements, and validating the application through automated tests.

Main goals:
- align the implemented endpoints with the project requirements
- add security and audit coverage where applicable
- make the test suite repeatable and green
- document the available endpoints and test types

---

## 2. Endpoint Coverage

The current API is documented in [Docs/API_Endpoints.md](../../Docs/API_Endpoints.md).

### Main endpoint groups

- **Authentication**
  - register, login, refresh, logout
- **Users**
  - get user by id, update username
- **Projects**
  - create, list, get by id, update, delete
- **Project members**
  - add member, list members, remove member by id
- **Tasks**
  - create, list, get by id, update, change status, delete, assign
- **Comments**
  - add, list, edit, delete
- **Attachments**
  - list, upload, download, delete

### Notes

- Project listing supports filtering by status.
- Task listing supports filtering by status.
- Task creation and update include priority and assignee support.
- Member removal uses member id in the current contract.

---

## 3. Test Coverage

The test suite is documented in [Docs/Test_Suite.md](../../Docs/Test_Suite.md).

### Test types available

- **Unit tests**
  - service-level logic
  - validation and business rules
- **Integration tests**
  - controller flow with Spring context
  - HTTP status and request/response validation
- **End-to-end tests**
  - real login, project creation, and task creation flow

### Examples

- **Authentication unit tests**
  - auth service and refresh token logic
- **Project unit tests**
  - project service and member service rules
- **Task unit tests**
  - task status, creation, update, assignment, and delete rules
- **Controller tests**
  - task controller and comment controller behavior
- **Integration tests**
  - auth, project, and task controllers
- **E2E**
  - full HTTP flow against the running application

### Validation status

- Full suite validated: **244 tests passed**
- E2E suite validated separately

Nota: algumas saídas do runner mostram mais entradas por classes aninhadas e relatórios separados, mas o total efetivo executado no Maven foi 244.

---

## 4. Security and Quality Improvements

Relevant improvements made during this sprint:

- Audit logging for security-sensitive actions
- Stronger authentication handling
- DB startup stabilization for clean local runs
- SonarCloud workflow alignment
- Endpoint and contract cleanup

---

## 5. Conclusion

Sprint 1 of Phase 2 focused on correctness, traceability, and verification. The API is now documented, the tests are categorized, and the full suite is passing.
