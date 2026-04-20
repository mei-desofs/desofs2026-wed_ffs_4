# Security Testing

## Test Categories

### Authentication Tests
- [ ] Login with valid credentials → token returned
- [ ] Login with invalid password → 401 Unauthorized
- [ ] Rate limit: 5 failed attempts → account locked 15 min
- [ ] Token expiration: use expired token → 401 Unauthorized
- [ ] Token validation: modify JWT payload → 401 Unauthorized

### Authorization Tests
- [ ] MEMBER accesses project they are not in → 403 Forbidden
- [ ] MEMBER updates task not assigned to them → 403 Forbidden
- [ ] MEMBER creates project → 403 Forbidden (only MANAGER+)
- [ ] MANAGER edits another manager's project → 403 Forbidden
- [ ] Missing auth header on protected endpoint → 401 Unauthorized

### Input Validation Tests
- [ ] Upload file >25MB → 413 Payload Too Large
- [ ] Upload `.exe` file → 400 Bad Request (not in whitelist)
- [ ] Upload with MIME type mismatch → 400 Bad Request
- [ ] Comment with 5000+ chars → 400 Bad Request
- [ ] Task title empty → 400 Bad Request

### XSS Prevention Tests
- [ ] Comment with `<script>alert('XSS')</script>` → stored as text, displayed encoded

### SQL Injection Tests
- [ ] Search with `' OR '1'='1` → no SQL injection
- [ ] Verify parameterized queries are used

### File Access Tests
- [ ] Direct URL access to `/storage/projects/1/...` → 403 Forbidden
- [ ] Download via `/attachments/{id}/download` (member) → 200 OK
- [ ] Download via `/attachments/{id}/download` (non-member) → 403 Forbidden
- [ ] Download after file soft-deleted → 404 Not Found

### Soft-Delete Tests
- [ ] Delete project → project hidden from list
- [ ] Query excludes deleted records 

### Path Traversal Tests
- [ ] Filename with `../../etc/passwd` → rejected or sanitized
- [ ] Stored filename is UUID → cannot guess paths

## Abuse Case Scenarios

| Scenario | Attack | Expected |
|----------|--------|----------|
| Brute Force | Rapid login attempts (100/min) | Account locks after 5 failures |
| Role Escalation | Modify JWT role claim | Still MEMBER (re-fetched from DB) |
| Project Bypass | Access `/projects/999/tasks` (not a member) | 403 Forbidden |
| File Access Bypass | Direct `/storage/projects/1/file.pdf` URL | 403 Forbidden |
| XSS Injection | Comment: `<img src=x onerror="alert('xss')">` | Stored as text, displayed encoded |
| SQLi in Search | Search: `term' OR '1'='1` | Only valid search results |

## Test Execution Order

1. **Unit Tests:** service layer (auth, validation, RBAC)
2. **Integration Tests:** API endpoints (CRUD, authorization)
3. **Security Tests:** abuse cases, injection, path traversal
4. **Regression Tests:** all endpoints after each fix

## Security Testing Methodology

- **Approach:** risk-based testing using threat priorities from STRIDE analysis.
- **Test design:** for each critical threat, define at least one negative test and expected secure behavior.
- **Execution evidence:** store request, response, timestamp, and tester notes for each executed test.
- **Pass/Fail rule:** a test passes only when the expected security result is observed with no bypass.

## Test Coverage Goals

- **Unit:** >70% code coverage
- **Integration:** all critical CRUD endpoints
- **Security:** all critical threat scenarios from threat model

## Threat Modeling Review Process

1. Re-check threat list after any relevant API, role, or file-handling change.
2. Update impacted tests in this document before implementation is considered complete.
3. Re-run all affected security tests and regression tests.
4. Record decisions and results in project documentation.

## Critical Security Coverage (Self-Contained)

This plan verifies the following essential protections:

- **Authentication strength:** invalid login is rejected, brute-force attempts are limited, expired/tampered tokens are rejected.
- **Authorization rules:** users cannot access projects/tasks outside their permissions.
- **Input safety:** malicious input does not execute code and does not break queries.
- **File protection:** invalid file type, MIME mismatch, oversized uploads, and unauthorized downloads are blocked.
- **Data lifecycle safety:** soft-deleted data is not returned in normal queries.
- **Secure logging:** sensitive values (passwords/tokens) are never stored in logs.

## Mini Traceability

| Security Requirement Area | How it is tested in this plan |
|---|---|
| Authentication and session control | Login success/failure, rate limiting, expired/tampered token checks |
| Authorization and role enforcement | Forbidden access tests for non-members and unauthorized role actions |
| Input validation and injection prevention | SQL injection payload tests and XSS payload encoding validation |
| Secure file handling | File size/type/MIME validation, path traversal protection, protected download access |
| Data protection and auditability | Soft-delete visibility checks and verification that logs do not expose secrets |

## ASVS Coverage Summary

| ASVS Area | Coverage in this plan |
|---|---|
| V1 Architecture | Threat model and trust-boundary-based tests |
| V2 Authentication | Login, token validation, rate limiting |
| V4 Access Control | RBAC and project/task isolation tests |
| V5 Validation | Input validation, SQLi and XSS checks |
| V8 Data Protection | Soft-delete visibility and protected data handling |
| V9 Communication | Authenticated API access and secure token usage |
| V10 Malicious Input/File | File type, size, MIME, and path traversal checks |
