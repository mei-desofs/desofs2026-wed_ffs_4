# Threat Modeling - STRIDE Analysis

**Implementation Details:** See 04_SecurityDesign.md for all security implementations

## Threat Modeling Diagrams

- DFD Level 0: ![](../../Docs/Diagrams/dfd_lvl0.png)
- DFD Level 1: ![](../../Docs/Diagrams/dfd_lvl1.drawio.png)
- DFD Level 2 (Authentication): ![](../../Docs/Diagrams/dfd_lvl2_auth.png)

---

## Risk Score Calculation

**L (Likelihood):** Probability of attack occurrence (1-5 scale)
- 1 = Very unlikely (requires specialized access/knowledge)
- 5 = Very likely (trivial, automated tools available)

**I (Impact):** Severity if threat succeeds (1-5 scale)
- 1 = Insignificant (minimal damage)
- 5 = Catastrophic (total data loss, system compromise)

**Score = L × I:** Risk priority
- Score ≥ 15: P0 Critical (implement immediately)
- Score 12-14: P1 High (implement in Phase 1)
- Score < 12: P2 Medium (nice-to-have)

---

## System Overview

**Architecture:** REST API-only backend
- Clients ← HTTPS → API Server
- API Server ← SQL ← PostgreSQL Database
- API Server ↔ File Storage (local filesystem)

**In Scope Threats:**
- Authentication (login, token management)
- Authorization (RBAC enforcement)
- Data Access (database queries)
- File Operations (upload, download, storage)
- Audit Logging

**Threat Model Inputs:**
- Assets inventory: [Assets.md](Assets.md)
- Entry/exit surface: [EntryPoints.md](EntryPoints.md), [ExitPoints.md](ExitPoints.md)
- Trust assumptions: [TrustLevels.md](TrustLevels.md)

---

## Trust Boundaries

**Boundary 1:** User ↔ API (input validation, authentication)
**Boundary 2:** API ↔ Database (parameterized queries, authorization)
**Boundary 3:** API ↔ Filesystem (access control, path validation)

---

## STRIDE Threat Analysis

### Authentication Threats

| ID | Threat | L | I | Score | Mitigation |
|---|---|---|---|---|---|
| T1 | Brute force login | 4 | 5 | 20 | M1: Rate limiting (5 attempts/min, 20/hour/IP, 15min lockout) |
| T2 | Weak password policy | 3 | 4 | 12 | M2: Min 12 chars, mixed complexity |
| T3 | JWT token forgery | 2 | 5 | 10 | M3: HS256 signature + expiration validation |
| T4 | Session hijacking | 3 | 5 | 15 | M4: HTTPS only, HttpOnly cookies, 24h expiry |

### Authorization Threats

| ID | Threat | L | I | Score | Mitigation |
|---|---|---|---|---|---|
| T5 | Role escalation | 3 | 5 | 15 | M5: Re-validate role from DB on every request |
| T6 | Unauth project access | 3 | 5 | 15 | M6: Query filters + membership checks |
| T7 | Unauth task access | 3 | 4 | 12 | M7: Ownership/manager/admin validation |
| T8 | Missing authz checks | 4 | 4 | 16 | M8: Auth middleware on all endpoints |

### File Upload Threats

| ID | Threat | L | I | Score | Mitigation |
|---|---|---|---|---|---|
| T9 | Malicious file upload | 4 | 4 | 16 | M9: Whitelist types + MIME check, size limit |
| T10 | File access bypass | 3 | 5 | 15 | M10: API-only endpoint, DB auth check |
| T11 | Path traversal | 3 | 4 | 12 | M11: Filename validation, UUID storage |
| T12 | Oversized upload | 4 | 3 | 12 | M12: 25MB limit, Content-Length check |

### Data Access Threats

| ID | Threat | L | I | Score | Mitigation |
|---|---|---|---|---|---|
| T13 | SQL injection | 4 | 5 | 20 | M13: Parameterized queries only |
| T14 | Database exposure | 2 | 5 | 10 | M14: Network isolation, no direct access |

### Application Threats

| ID | Threat | L | I | Score | Mitigation |
|---|---|---|---|---|---|
| T15 | XSS in comments | 4 | 4 | 16 | M15: HTML entity encoding on output |
| T16 | Unauthorized comment edit | 3 | 4 | 12 | M16: Author/manager only validation |
| T17 | Soft-delete visibility | 2 | 3 | 6 | M17: Query filters (WHERE deletedAt IS NULL) |
| T21 | CSRF attack | 3 | 5 | 15 | M21: Bearer token in Authorization header |

### Logging Threats

| ID | Threat | L | I | Score | Mitigation |
|---|---|---|---|---|---|
| T18 | Sensitive data in logs | 3 | 4 | 12 | M18: Log scrubbing, no passwords/tokens |
| T19 | Audit log tampering | 2 | 4 | 8 | M19: Append-only log, restricted access |
| T20 | Incomplete audit trail | 3 | 3 | 9 | M20: Log all auth/authz/data operations |

---

## Risk Assessment Matrix

**Critical Priority (P0) - Score ≥ 15:**
- T1 (Brute force): 20 - M1 rate limiting
- T4 (Session hijacking): 15 - M4 HTTPS/expiry
- T5 (Role escalation): 15 - M5 DB re-validation
- T6 (Unauth project): 15 - M6 membership checks
- T8 (Missing authz): 16 - M8 middleware
- T9 (Malicious upload): 16 - M9 whitelist
- T10 (File bypass): 15 - M10 API endpoint
- T13 (SQL injection): 20 - M13 parameterized queries
- T15 (XSS): 16 - M15 output encoding
- T21 (CSRF): 15 - M21 Bearer token

**High Priority (P1) - Score 12-14:**
- T2 (Weak password): 12 - M2 complexity rules
- T7 (Unauth task): 12 - M7 ownership check
- T11 (Path traversal): 12 - M11 UUID storage
- T12 (Oversized file): 12 - M12 size limit
- T16 (Comment edit): 12 - M16 auth check
- T18 (Logs): 12 - M18 scrubbing

**Medium Priority (P2) - Score < 12:**
- T3 (Token forgery): 10 - M3 signature validation
- T17 (Soft-delete): 6 - M17 query filters
- T19 (Log tampering): 8 - M19 access control
- T20 (Audit incomplete): 9 - M20 logging

---

## Threat-to-Mitigation Mapping

**P0 Mitigations Required for Phase 1:**
- M1: Rate limiting implementation
- M3: JWT signature validation
- M4: Token expiration + HTTPS requirement
- M5: Role re-validation from DB
- M6: Project membership filters
- M8: Authorization middleware
- M9: File type whitelist + MIME validation
- M10: Authenticated file endpoint
- M13: Parameterized SQL queries
- M15: Output encoding
- M21: Bearer token authentication (CSRF prevention)

**P1 Mitigations Required for Phase 1:**
- M2: Password complexity rules
- M7: Task access authorization
- M11: Path traversal prevention
- M12: File size enforcement
- M16: Comment authorization
- M18: Log scrubbing

**P2 Mitigations (Nice-to-have Phase 1):**
- M17: Soft-delete filtering
- M19: Audit log protection
- M20: Comprehensive logging

---

## Implementation References

| Mitigation | See Document | Section |
|---|---|---|
| M1-M5, M7-M8 | 04_SecurityDesign.md | Authentication & Authorization |
| M9-M12 | 04_SecurityDesign.md | Secure File Handling |
| M13, M18 | 04_SecurityDesign.md | Input Validation & Logging |
| M15 | 04_SecurityDesign.md | Output Encoding (XSS Prevention) |
| All test cases | 06_SecurityTesting.md | Security Test Strategy |
| All requirements | 01_Requirements.md | Security Requirements (SR-1 to SR-13) |
