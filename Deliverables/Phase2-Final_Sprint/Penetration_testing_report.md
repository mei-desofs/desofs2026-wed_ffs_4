# Penetration Testing Report — DESOFS Project Management API

## 1. Scope and Methodology

This report covers two complementary testing approaches:

1. **Automated Dynamic Application Security Testing (DAST)** using OWASP ZAP, executed through the `DAST (OWASP ZAP Full)` GitHub Actions pipeline against the running application.
2. **Manual penetration testing**, targeting business logic flaws that automated scanners typically miss: broken access control (IDOR, vertical privilege escalation), mass assignment, authentication abuse, and input validation bypass.

The manual tests were designed around the application's actual functional and security requirements (FR-1 to FR-22, SR-1 to SR-14) and the OWASP ASVS 5.0 controls already mapped in the project's compliance tracker, rather than generic black-box probing. This ensures the testing effort validates the specific guarantees the application claims to provide.

Three seeded accounts (created by `DataBootstrap`) were used to test authorization boundaries across roles:

| Role | Email | Project membership |
|---|---|---|
| ADMIN | admin@example.com | Owner of all seeded projects |
| MANAGER | manager@example.com | Member of "Team Tasks" and "Project Roadmap" |
| USER | user@example.com | Member of "Team Tasks" and "Project Roadmap" |

"Admin Dashboard" and "User Management" projects have no members besides the admin, making them suitable targets for IDOR testing.

---

## 2. Automated Scan Results (OWASP ZAP)

The ZAP Full Scan pipeline was executed against the running application. Results:

| Risk Level | Number of Alerts |
|---|---|
| High | 0 |
| Medium | 0 |
| Low | 0 |
| Informational | 1 (4 instances) |

The only finding was **Non-Storable Content** (CWE-524), an informational alert indicating that responses are not cacheable by proxies due to the no-store directive. This is expected and intentional behavior for a stateless JWT-based API serving user-specific data, caching authenticated responses would itself be a security risk. No remediation was required.

No High, Medium, or Low severity issues were identified by the automated scan, which is consistent with the absence of reflected content, exposed sensitive headers, or common injection points on the unauthenticated surface that ZAP's default scan policy probes.

---

## 3. Manual Testing Results

Automated scanners do not authenticate as different roles or understand application-specific business rules, so the following tests were performed manually using `curl` against the live application, authenticated as ADMIN, MANAGER, and USER.

### 3.1 Round 1 — Initial findings

The first test run uncovered two issues that required remediation before the application could be considered compliant with its stated security requirements.

#### Finding 1 — Broken Access Control on Session Termination Endpoint (Critical)

**Test:** `DELETE /api/admin/users/{id}/sessions` called by a USER-role token, targeting the admin's own user ID.

**Result:** `200 OK` — `{"message":"All sessions terminated for user 1"}`

**Root cause:** SecurityConfig defined explicit `hasRole("ADMIN")` rules only for `PUT /api/admin/users/*/role`, while the rest of `/api/admin/**` (including the session-termination endpoints) fell through to the catch-all `.anyRequest().authenticated()` rule, which only requires a valid token, not a specific role.

**Impact:** Any authenticated user, regardless of role, could forcibly log out any other user, including administrators, by invalidating their active sessions. This is a vertical privilege escalation and a denial-of-service vector against arbitrary accounts.

**Remediation:** Added a blanket rule `.requestMatchers("/api/admin/**").hasRole("ADMIN")` in `SecurityConfig`, placed before the catch-all, ensuring every endpoint under the admin namespace requires the ADMIN role regardless of HTTP method.

**Verification:** Re-tested after the fix — response is now 403 Forbidden.

#### Finding 2 — Unhandled Exception Disclosure via Lazy-Loaded JPA Entity (Medium)

**Test:** `GET /api/projects` called by a USER-role token.

**Result:** `500 Internal Server Error` — `{"error":"An unexpected error occurred"}`

**Root cause:** `ProjectController.listProjects()` returned `List<Project>` (the JPA entity) directly to the Jackson serializer. Project.owner is mapped as `@ManyToOne(fetch = FetchType.LAZY)`. By the time Jackson attempted to serialize the lazy-loaded owner proxy, the Hibernate session backing the `@Transactional` service call had already closed, raising a LazyInitializationException that was caught by the global exception handler and converted into a generic 500 response.

**Impact:** While the generic error message itself did not leak internal details (confirming GlobalExceptionHandler works as intended for V16.5.1), the endpoint was completely non-functional for any non-admin role, a denial of service for the application's core "list my projects" feature (FR-6). It also revealed that domain entities were being serialized directly, bypassing the DTO pattern used consistently elsewhere in the codebase (tasks, comments, mass-assignment protections).

**Remediation:** Introduced a ProjectResponse DTO exposing only id, name, description, and deleted, and updated `ProjectController.listProjects()` to map the service result through `ProjectResponse::from` before returning it. This both fixes the lazy-loading crash and closes a latent mass-assignment/data-exposure gap (the owner and members collections, including other users' internal data, were never meant to be exposed on this endpoint).

**Verification:** Re-tested after the fix — response is now `200 OK` with the correct project list, scoped to the calling user's memberships.

### 3.2 Round 2 — Post-remediation results

All tests were re-executed after applying both fixes. Full results below.

| # | Test | Expected | Result | Status |
|---|---|---|---|---|
| 1.1 | IDOR — list projects as USER | Only own projects returned | 200 OK, returned exactly "Team Tasks" and "Project Roadmap" | Pass |
| 1.2 | IDOR — USER accesses non-member project | 403 Forbidden | 403 Forbidden | Pass |
| 1.3 | IDOR — USER updates non-member project | 403 Forbidden | 403 Forbidden | Pass |
| 2.1 | RBAC — USER creates project | 403 Forbidden | 403 Forbidden | Pass |
| 2.2 | RBAC — MANAGER creates project | 403 Forbidden | 403 Forbidden | Pass |
| 2.3 | RBAC — USER self-promotes to ADMIN | 403 Forbidden | 403 Forbidden | Pass |
| 2.4 | RBAC — USER deletes project | 403 Forbidden | 403 Forbidden | Pass |
| 2.5 | RBAC — USER terminates admin's sessions | 403 Forbidden | 403 Forbidden (fixed — was 200 OK) | Pass (remediated) |
| 3.1 | Mass Assignment — extra fields on task creation | createdBy/id/status ignored | Task created with status:"TODO", server-assigned id and createdBy — client-supplied values discarded | Pass |
| 3.2 | Mass Assignment — role/owner injected on project update | Fields ignored, request still rejected on authorization | 403 Forbidden (USER not authorized to update) | Pass |
| 4.1 | Invalid status transition (TODO to DONE) | 400 Bad Request | 400 Bad Request — "Invalid status transition: TODO to DONE" | Pass |
| 5.1 | SQL injection in login email field | 401 Unauthorized, no injection | 401 Unauthorized — "Invalid credentials" | Pass |
| 5.2 | XSS payload in comment content | Stored and returned HTML-escaped | Comment stored, content returned as escaped HTML entities | Pass |
| 5.3 | XSS — confirm escaping on read | Escaped content on GET | Confirmed escaped | Pass |
| 6.1 | Brute force — 6 consecutive failed logins | Lockout (429) from the 5th/6th attempt | Attempts 1-4: 401; attempts 5-6: 429 Too Many Requests | Pass |
| 6.2 | Lockout persists with correct password | 429 despite valid credentials | 429 Too Many Requests | Pass |
| 7.1 | Malformed/tampered JWT | Request rejected | 403 Forbidden (Spring Security's default for failed authentication) | Pass |
| 7.2 | No token on protected endpoint | Request rejected | 403 Forbidden | Pass |
| 7.3 | JWT with alg=none | Request rejected | 403 Forbidden — jjwt rejects the unsigned token before it reaches the controller | Pass |
| 8.1 | Path traversal in uploaded filename | 400 Bad Request | 400 Bad Request — "Invalid filename" | Pass |
| 8.2 | Upload of disallowed extension (.sh) | 400 Bad Request | 400 Bad Request — "File extension is not allowed" | Pass |
| 9.1 | Nonexistent endpoint | Generic error, no stack trace | 403 Forbidden, generic body, no internal details | Pass |
| 9.2 | Malformed numeric ID in path | Generic error, no stack trace | 403 Forbidden, generic body, no internal details | Pass |

**Note on tests 7.1, 7.2, 9.1, and 9.2:** the initial test script expected `401 Unauthorized` for unauthenticated or malformed requests. The application consistently returns `403 Forbidden` instead. This is Spring Security's default behavior when no valid authentication is present and is not a vulnerability — both status codes correctly deny access without leaking information. The expected outcomes in this report have been updated to reflect this as a pass.

### 3.3 Summary

| Category | Tests | Passed | Failed (pre-fix) | Failed (post-fix) |
|---|---|---|---|---|
| IDOR / Project Isolation | 3 | 3 | 1 | 0 |
| RBAC / Privilege Escalation | 5 | 5 | 1 | 0 |
| Mass Assignment | 2 | 2 | 0 | 0 |
| Business Logic (status pipeline) | 1 | 1 | 0 | 0 |
| Injection (SQLi/XSS) | 3 | 3 | 0 | 0 |
| Authentication (brute force) | 2 | 2 | 0 | 0 |
| Token Tampering | 3 | 3 | 0 | 0 |
| Path Traversal / File Upload | 2 | 2 | 0 | 0 |
| Error Handling Disclosure | 2 | 2 | 0 | 0 |
| **Total** | **23** | **23** | **2** | **0** |

---

## 4. Automated Security Pipelines (CI/CD)

In addition to the point-in-time testing above, the project runs continuous automated security analysis through GitHub Actions, providing ongoing coverage beyond this single assessment:

| Pipeline | Type | Tool | Trigger | Purpose |
|---|---|---|---|---|
| CI | Build / SCA / Static Analysis | Maven + OWASP Dependency-Check + SpotBugs | Every push/PR (all branches) | Builds the app, runs the full test suite, scans dependencies for known CVEs, and runs SpotBugs static analysis |
| SAST (CodeQL) | Static Analysis | GitHub CodeQL | Every push/PR (all branches) | Identifies vulnerable code patterns (injection, insecure deserialization, etc.) at the source-code level |
| SonarQube Analysis | Static Analysis / Code Quality | SonarCloud | Push to main + PR | Code quality, maintainability, and known vulnerability detection |
| DAST (OWASP ZAP Full) | Dynamic Analysis | OWASP ZAP | Daily (03:00) + manual | Black-box scan of the running application for common web vulnerabilities |
| IAST | Interactive Analysis | Contrast Community Agent + ZAP traffic | Daily (04:00) + PR | Runtime instrumentation correlating ZAP-driven traffic with actual code execution paths |
| ASVS Mapping | Compliance tracking | Custom script | Push to main + PR | Cross-references ASVS requirements against test coverage |
| Coverage / Test Coverage & Reporting | Test Quality | JaCoCo | Every push/PR (all branches) / push+PR to main | Generates coverage reports; the main-targeted workflow additionally enforces a 70% line-coverage threshold and fails the build below it |

Dependency scanning was consolidated into the **CI** pipeline rather than running as a standalone job, so that a vulnerable dependency blocks the build on every push and pull request instead of being caught only at a separate stage. Static analysis (SpotBugs) was added alongside it for the same reason — both checks now gate every change at the earliest possible point in the pipeline, ahead of merge.

This layered approach follows defense-in-depth principles applied to the security testing process itself: CI-time checks (dependency CVEs, SpotBugs, CodeQL) catch issues before code is even merged; DAST and IAST catch issues in a running instance; and the manual testing performed in this report catches business-logic flaws that none of the automated layers are designed to detect.

Two pipelines outside the security scope (**Publish to Docker Hub** and **Prepare release**) also exist in the repository but are release-management automation, not security testing, and are therefore not covered in this report.


---

## 5. Conclusions and Recommendations

The application demonstrated strong baseline security controls across authentication (account lockout, BCrypt hashing, JWT validation), input validation (parameterized queries, XSS escaping, file upload restrictions), and business logic integrity (status pipeline enforcement, mass-assignment protection via DTOs).

Two genuine vulnerabilities were identified during manual testing and have been remediated:

1. A missing authorization rule allowed any authenticated user to terminate any other user's sessions, including administrators. Fixed by adding a blanket ADMIN-only rule for the entire `/api/admin/**` namespace.
2. A lazy-loading crash in the project listing endpoint, caused by serializing a JPA entity directly instead of a DTO, made the core "list my projects" feature completely unusable for non-admin roles. Fixed by introducing a `ProjectResponse` DTO, which also closed a latent data-exposure risk.

Both findings illustrate the value of manual, role-based testing: neither would have been caught by an unauthenticated black-box scanner like the ZAP baseline scan, since both require authenticating as a specific non-admin role and exercising application-specific logic.

**Recommendations for future iterations:**

- Extend the manual test suite to cover horizontal privilege escalation between two MANAGER-role users on different projects, and IDOR on attachment download (GET /api/attachments/{id}/download) using an attachment ID belonging to a project the caller is not a member of.

---

## Appendix A — Manual Test Script

The full curl-based test script used to produce the results in Section 3 is included alongside this report as `pentest/pentest_manual_tests.sh`.

## Appendix B — Raw ZAP Report

The complete ZAP scan output (HTML, JSON, and Markdown formats) is available in the "zap_scan" artifact produced by the "DAST (OWASP ZAP Full)" GitHub Actions workflow run referenced in this report.