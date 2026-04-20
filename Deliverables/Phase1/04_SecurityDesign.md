# Security Design

## Authorization (RBAC)

**Model:**

```
┌─────────────────────────────────────────────┐
│       Role-Based Access Control             │
├─────────────────────────────────────────────┤
│ ADMIN  → All CRUD ops, user mgmt            │
│ MANAGER→ Own project CRUD, team management  │
│ MEMBER → Assigned tasks, comments, files    │
└─────────────────────────────────────────────┘
```

## Data Security

**Sensitive Data:**

- Passwords: Never logged, only hashed
- JWT tokens: Never logged
- Audit log scrubbing: Regex to remove sensitive patterns

**Soft-Delete:**

- All entities have `deletedAt` & `deletedBy`
- Queries automatically exclude deleted records (WHERE deletedAt IS NULL)
- Admins can restore via separate endpoint (Phase 2)

**File Access Control:**

- Only project members can upload/download
- Files served via authenticated endpoint, not direct URLs
- Original filename stored, served filename is UUID
- Access logged to audit table

---

## Input Validation

**File Upload:**

- Whitelist types: pdf, doc, docx, xls, xlsx, ppt, pptx, jpg, jpeg, png, gif
- Max size: 25MB (check Content-Length before upload)
- MIME type verification (match extension)
- Filename sanitization: reject `.., /, //, \`
- Store with generated UUID: `{uuid}.{ext}`

**Comments:**

- Max length: 5000 chars
- No raw HTML (output encode on retrieval)
- HTML entity encoding: `&`, `<`, `>`, `"`, `'`

**Task/Project Fields:**

- Title/description: max 500/2000 chars
- Status enum validation: TODO, IN_PROGRESS, DONE
- Priority enum: LOW, MEDIUM, HIGH

**Authentication:**

- Username: alphanumeric + underscore, 3-50 chars
- Password: min 12 chars, complexity check (via regex)

---

## Audit

**Tracked Events:**

- CREATE: entity_type, entity_id, new_values
- UPDATE: entity_type, entity_id, old_values, new_values
- DELETE: entity_type, entity_id (soft-delete only records deletion)
- LOGIN: username, result (success/fail), IP
- AUTH_FAIL: username, reason, IP
- FILE_UPLOAD: filename, size, user_id
- FILE_DOWNLOAD: filename, user_id
- AUTH_CHANGE: role change, by_user, for_user

**Retention:** Indefinite (per requirements)

---

## Threat Mitigations Applied

| Threat            | Mitigation                           | Implementation                               |
| ----------------- | ------------------------------------ | -------------------------------------------- |
| Brute force       | Rate limiting + lockout              | 5/min per user, 20/hour per IP, 15min lock   |
| Token forgery     | HS256 signature                      | Validate signature + expiry on every request |
| Session hijacking | HTTPS + HttpOnly                     | Phase 2 deployment                           |
| Role escalation   | Re-validate from DB                  | Always fetch role from DB, never trust token |
| Unauth access     | Project membership checks            | Query filters + endpoint authorization       |
| XSS               | Output encoding                      | HTML entity encoding on comment display      |
| Malicious files   | Whitelist + MIME check               | Type validation, no execution possible       |
| SQL injection     | Parameterized queries                | All DB queries use prepared statements       |
| Path traversal    | Filename validation                  | UUID storage + sanitization                  |
| Sensitive in logs | Log scrubbing                        | Never log passwords, tokens, PII             |
| CSRF              | Bearer token in Authorization header | Immune by design                             |

---

## 1. Authentication

**Algorithm:** Bcrypt with adaptive hashing

**Configuration:**

- Salt rounds: 12 (takes ~100ms per hash, resistant to GPU attacks)
- Cost factor: 12
- Encoding: $2a$12$...

**Example Hash:**

```
plaintext: "MySecurePassword123!"
bcrypt hash: "$2a$12$R9h7cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jKMUe"
```

**Verification Process:**

```
1. Receive plaintext password from user
2. Hash received password with bcrypt
3. Compare hashes (byte-for-byte)
4. Never store plaintext; never log password
```

### 1.3 Token/Session Management

**Token Type:** JWT (JSON Web Token)

**Token Structure:**

```
Header:
{
  "alg": "HS256",           // Algorithm
  "typ": "JWT",             // Type
  "kid": "key-2026-04"      // Key ID (for rotation)
}

Payload (Claims):
{
  "sub": "1",               // Subject (User ID)
  "iat": 1712847600,        // Issued At
  "exp": 1712934000,        // Expiration (24h later)
  "aud": "teuxdeux-api",    // Audience
  "role": "MANAGER",        // User Role
  "username": "john"        // Username (convenience)
}

Signature:
HMACSHA256(Base64(Header) + "." + Base64(Payload), SECRET_KEY)
```

**Token Validation:**

```
On each API request:
1. Extract token from Authorization header
2. Verify signature with server secret key
3. Validate expiration time (exp claim)
4. Validate audience (aud claim)
5. Re-fetch user role from database (never trust token role)
6. Proceed with request if all valid
```

**Token Expiration:**

- Standard expiration: 24 hours
- Refresh mechanism: TBD (client asks for new token before expiration)

**Token Storage (Client-side - Phase 2):**

- Recommended: HttpOnly cookie (protects against XSS)
- Alternative: LocalStorage (if HttpOnly not possible)
- Never send token in URL parameters

### 1.4 Logout Mechanism

**Logout Flow:**

```
POST /auth/logout
Authorization: Bearer <token>

Server Action:
1. Extract user ID from token
2. Invalidate token (add to blacklist or remove from session store)
3. Log logout event in audit trail
4. Return 200 OK
```

**Token Blacklist (if using DB sessions):**

- Store revoked tokens in `blacklist` table
- Check blacklist on each request
- Clean up expired tokens periodically

**Alternative (Stateless):**

- No server-side logout needed
- Client simply deletes token
- Token remains valid until expiration
- Limited impact: only valid for 24 hours

---

## 2. Authorization Design

### 2.1 Role-Based Access Control (RBAC)

**Three Roles with Hierarchical Permissions:**

```
┌─────────────────────────────────────────────────────────────┐
│ ADMIN                                                       │
│ Create/delete users                                         │
│ Assign system-level roles                                   │
│ Access all projects/tasks/files                             │
│ View all audit logs                                         │
│ Delete any data (soft or hard)                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ MANAGER                                                     │
│ Create projects                                             │
│ Manage members in own projects                              │
│ Create/edit/delete all tasks in own projects                │
│ Upload/download files in own projects                       │
│ View project audit logs                                     │
│ Assign tasks to members                                     │
│ Cannot access other managers' projects                      │
│ Cannot change user roles                                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ MEMBER                                                      │
│ View assigned projects                                      │
│ Create/edit own tasks (assigned to self)                    │
│ Update own task status                                      │
│ Upload/download files for own tasks                         │
│ Add comments to any project task                            │
│ Edit own comments                                           │
│ Cannot assign tasks to others                               │
│ Cannot create/delete projects                               │
│ Cannot manage project members                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Data Protection

### 3.1 Data at Rest

**Password Storage:**

- Algorithm: Bcrypt (adaptive hashing)
- Never stored plaintext
- Never logged

**Sensitive Fields Not Currently Encrypted:**

- Tokens (valid only for 24 hours)
- User data (in Phase 2: encrypt sensitive fields if needed)
- File content (Phase 2: evaluate encryption based on data sensitivity)

**File Storage Security:**

- Files stored outside web root
- Files served through authenticated API endpoint
- OS-level file permissions: 0640 (readable by app user, not world)
- Filename sanitization: no path traversal opportunities

### 3.2 Data in Transit

**Phase 1 (Development):**

- HTTP allowed (for testing)
- Tokens valid only in memory

**Phase 2 (Production):**

- **Requirement:** HTTPS/TLS 1.2 minimum
- **Certificate:** Self-signed for development, CA-signed for production
- **Cipher Suites:** AES-GCM, ChaCha20-Poly1305
- **HSTS Header:** `Strict-Transport-Security: max-age=31536000; includeSubDomains`

---

## 4. Input Validation & Output Encoding

### 4.1 Input Validation Strategy

**Principle:** Whitelist-based validation (accept known good, reject everything else)

**Validation Points:**

1. **Type Validation:** Ensure correct data type
2. **Format Validation:** Regex/pattern matching
3. **Length Validation:** Min/max string length
4. **Range Validation:** Min/max numeric values
5. **Whitelist Validation:** Only allowed values/characters

### 4.2 Output Encoding (XSS Prevention)

**Principle:** Always encode output, never trust user input

**Encoding Strategy:** HTML Entity Encoding

**Encoding Rules:**

```
Character | Encoded   | Usage
--------  | --------- | -----
&         | &amp;     | ampersand
<         | &lt;      | less-than
>         | &gt;      | greater-than
"         | &quot;    | double quote
'         | &#39;     | single quote
/         | &#x2F;    | forward slash (optional)
```

---

## 5. Secure File Handling

### 5.1 File Upload Security

**Validation Checklist:**

- [ ] Extension whitelist (no executable types)
- [ ] MIME type verification (not just extension)
- [ ] File size limit enforcement (25MB max)
- [ ] Content scanning (optional: antivirus)
- [ ] Filename sanitization (remove path traversal)
- [ ] Storage outside web root
- [ ] File permissions restricted (0640)

---

## 6. Audit Logging & Monitoring

### 6.1 What Gets Logged

**Security Events (Always):**

- Authentication attempts
- Authorization failures
- Role changes
- User deletions
- Project member additions/removals
- File uploads
- File downloads
- Soft-deletions

**Data Modifications (Always):**

- Project create/update/delete
- Task create/update/delete
- Comment create/update/delete
- Attachment create/delete

**What NOT to Log:**

- Passwords
- Authentication tokens
- API keys or secrets
- File contents
- Unnecessary verbose request/response bodies

---

## 7. Security Configuration Summary

| Aspect           | Configuration                                | Phase |
| ---------------- | -------------------------------------------- | ----- |
| Authentication   | JWT + Bcrypt                                 | 1     |
| Password Storage | Bcrypt ($2a$12$)                             | 1     |
| Token Expiration | 24 hours                                     | 1     |
| Token Algorithm  | HS256                                        | 1     |
| Rate Limiting    | 5/min per user, 20/hour per IP               | 1     |
| HTTPS            | HTTP allowed (dev), HTTPS required (Phase 2) | 2     |
| TLS Version      | 1.2+ required                                | 2     |
| Input Validation | Whitelist-based                              | 1     |
| Output Encoding  | HTML entity encoding                         | 1     |
| CSP Headers      | Strict policy (Phase 2)                      | 2     |
| File Storage     | Outside web root, OS permissions 0640        | 1     |
| File Size Limit  | 25MB max                                     | 1     |
| File Types       | Whitelist of 12 types                        | 1     |
| Audit Logging    | Centralized, comprehensive                   | 1     |
| Soft-Delete      | Logical delete with audit trail              | 1     |
| SQL Protection   | Parameterized queries only                   | 1     |
| XSS Protection   | Output encoding, CSP                         | 1     |
