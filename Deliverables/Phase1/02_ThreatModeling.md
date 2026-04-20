# Threat Modeling - STRIDE Analysis

**Implementation Details:** See 04_SecurityDesign.md for all security implementations

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

---

## Trust Boundaries

**Boundary 1:** User ↔ API (input validation, authentication)
**Boundary 2:** API ↔ Database (parameterized queries, authorization)
**Boundary 3:** API ↔ Filesystem (access control, path validation)

---

## STRIDE Threat Analysis (Baseado no DFD Nível 1)

### Authentication Threats (Spoofing & Info Disclosure)
Foco nas interações com o **AuthService** e o tráfego que cruza a fronteira da **Internet**.

| ID | Threat | L | I | Score | Mitigation |
|---|---|---|---|---|---|
| T1 | Brute force/Credential stuffing on `AuthService` | 4 | 5 | 20 | M1: Rate limiting by IP/User, account lockout mechanisms |
| T2 | JWT token interception over `Internet` | 3 | 5 | 15 | M2: Mandatory TLS 1.2+ for all traffic, Secure/HttpOnly flags |
| T3 | Weak password generation | 3 | 4 | 12 | M3: Enforce password complexity (min 12 chars, entropy check) |

### Authorization Threats (Elevation of Privilege & Info Disclosure)
Foco nos controlos de acesso entre os papéis (User, Member, Manager, Admin) nos vários serviços.

| ID | Threat | L | I | Score | Mitigation |
|---|---|---|---|---|---|
| T4 | Role escalation to `Admin` or `Manager` | 3 | 5 | 15 | M4: Strict server-side RBAC validation from DB on every request |
| T5 | IDOR (Insecure Direct Object Reference) on `TaskService` | 4 | 4 | 16 | M5: Validate resource ownership/membership against JWT claims |
| T6 | Unauthorized project modification via `ProjectService` | 3 | 5 | 15 | M6: Authorization middleware matching user role to project ID |

### File Upload Threats (Tampering, DoS & Info Disclosure)
Foco no **FileService** e na escrita/leitura no **Filesystem (FileStorage)**.

| ID | Threat | L | I | Score | Mitigation |
|---|---|---|---|---|---|
| T7 | Malicious executable upload to `FileStorage` | 4 | 5 | 20 | M7: Strict MIME validation, malware scanning, remove execute permissions on disk |
| T8 | Storage exhaustion DoS (mass uploads) | 4 | 4 | 16 | M8: File size limits (e.g., 25MB), user/tenant quota enforcement |
| T9 | Path traversal leading to arbitrary file access | 3 | 4 | 12 | M9: Strip paths from filenames, store files using UUIDs |

