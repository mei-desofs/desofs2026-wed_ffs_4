# Security Testing

## Test Categories

### Authentication Tests
- [ ] Login with valid credentials → token returned
- [ ] Login with invalid password → 401 Unauthorized
- [ ] Login with non-existent user → 401 Unauthorized
- [ ] Rate limit: 5 failed attempts → account locked 15 min
- [ ] Token expiration: Use expired token → 401 Unauthorized
- [ ] Token validation: Modify JWT payload → 401 Unauthorized

### Authorization Tests
- [ ] MEMBER accesses project they're not in → 403 Forbidden
- [ ] MEMBER updates task not assigned → 403 Forbidden
- [ ] MEMBER creates project → 403 Forbidden (only MANAGER+)
- [ ] MANAGER edits other manager's project → 403 Forbidden
- [ ] ADMIN can access any resource → 200 OK
- [ ] Missing auth header → 401 Unauthorized

### Input Validation Tests
- [ ] Upload file >25MB → 413 Payload Too Large
- [ ] Upload .exe file → 400 Bad Request (not in whitelist)
- [ ] Upload with MIME type mismatch → 400 Bad Request
- [ ] Comment with 5000+ chars → 422 Validation Error
- [ ] Task title empty → 400 Bad Request
- [ ] Username with special chars → 400 Bad Request

### XSS Prevention Tests
- [ ] Comment with `<script>alert('XSS')</script>` → Stored as-is, displayed encoded
- [ ] Verify output: `&lt;script&gt;` → XSS prevented

### SQL Injection Tests
- [ ] Search with `' OR '1'='1` → No SQL injection
- [ ] Username with `';DROP TABLE users;--` → No SQL injection
- [ ] Verify parameterized queries used

### File Access Tests
- [ ] Direct URL access to /storage/projects/1/... → 403 Forbidden
- [ ] Download via `/attachments/{id}/download` (member) → 200 OK
- [ ] Download via `/attachments/{id}/download` (non-member) → 403 Forbidden
- [ ] Download after file soft-deleted → 404 Not Found

### Soft-Delete Tests
- [ ] Delete project → Project hidden from list
- [ ] Query includes `WHERE deletedAt IS NULL` → Deleted records excluded
- [ ] Admin views audit log → Can see soft-deleted records

### Path Traversal Tests
- [ ] Filename with `../../etc/passwd` → Rejected or sanitized
- [ ] Filename with `../../../ → Rejected
- [ ] Stored filename is UUID → Cannot guess paths

## Abuse Case Scenarios

| Scenario | Attack | Expected | Result |
|----------|--------|----------|--------|
| Brute Force | Rapid login attempts (100/min) | Account locks after 5 failures | Pass |
| Role Escalation | Modify JWT role claim | Still MEMBER (re-fetched from DB) | Pass |
| Project Bypass | Access `/projects/999/tasks` (not a member) | 403 Forbidden | Pass |
| File Access Bypass | Direct `/storage/projects/1/file.pdf` URL | 403 Forbidden | Pass |
| XSS Injection | Comment: `<img src=x onerror="alert('xss')">` | Stored as text, displayed encoded | Pass |
| SQLi in Search | Search: `term' OR '1'='1` | Only valid search results | Pass |
| Privilege via File | Upload malware.exe as document.pdf | Rejected (MIME type check) | Pass |
| Account Takeover | Intercept session token (HTTP) | HTTPS prevents (Phase 2) | - |

## ASVS Mapping (v4)

| ASVS Control | Test Case | Status |
|---|---|---|
| V1: Architecture | Threat model documented | Pass |
| V2: Authentication | Login, token validation, rate limiting | Pass |
| V4: Access Control | RBAC, project isolation, task ownership | Pass |
| V5: Validation | Input validation, file upload | Pass |
| V8: Data Protection | Passwords hashed, soft-delete | Pass |
| V9: Communication | HTTPS enforced (Phase 2) | - |
| V10: Malware | File type whitelist, no execution | Pass |

## Test Execution Order

1. **Unit Tests:** Service layer (auth, validation, RBAC)
2. **Integration Tests:** API endpoints (CRUD, authorization)
3. **Security Tests:** Abuse cases, injection, path traversal
4. **Regression Tests:** All endpoints after each fix

## Test Coverage Goals

- **Unit:** >70% code coverage
- **Integration:** All CRUD endpoints
- **Security:** All threat scenarios from threat model
   - Soft-delete integrity
   - Audit trail completeness
   - Database constraints

5. **API Security** (10% of tests)
   - HTTPS enforcement (Phase 2)
   - CORS configuration
   - Error handling (no sensitive info leakage)
   - Rate limiting enforcement

---

## 2. Unit Tests

### 2.1 Authentication Tests

**Test Suite: `AuthServiceTest.java`**

```java
class AuthServiceTest {
  
  // TC-1.1: Valid credentials should return JWT token
  @Test
  void testLoginWithValidCredentials() {
    // Setup
