# Security Design

## Authentication

**Flow:**
1. User sends `POST /auth/login` with username/password
2. API queries DB for user, hashes provided password with bcrypt
3. Compare hash with stored hash
4. Rate limit check: max 5 failed attempts/min per user, 20/hour per IP
5. Lock account 15 min after 5 failures
6. Generate JWT token: `{sub: userId, role, iat, exp: now+24h, aud: "teuxdeux-api"}`
7. Return token to client

**Token Validation:**
- On every request: Extract bearer token from Authorization header
- Validate signature with server secret (HS256)
- Check expiration (exp claim)
- Re-fetch user role from DB (never trust token role)
- If invalid: return 401 Unauthorized

**Passwords:**
- Hash with bcrypt, salt rounds ≥ 10
- Never store plaintext
- Min 12 chars: uppercase, lowercase, digits, special chars

---

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

**Authorization Checks:**

| Endpoint | Roles | Check |
|----------|-------|-------|
| POST /projects | MANAGER+ | - |
| PUT /projects/{id} | MANAGER (owner)+ADMIN | Check ownership |
| DELETE /projects/{id} | MANAGER (owner)+ADMIN | Check ownership |
| POST /projects/{id}/tasks | Any project member | Check membership |
| PUT /projects/{id}/tasks/{tId} | Task owner/MANAGER/ADMIN | Check ownership |
| POST /tasks/{id}/comments | Any project member | Check membership |
| PUT /tasks/{id}/comments/{cId} | Comment author/MANAGER/ADMIN | Check authorship |
| POST /tasks/{id}/attachments | Any project member | Check membership |
| GET /attachments/{id}/download | Any project member | Check membership |

**Implementation:**
```
Middleware on all endpoints:
1. Extract & validate JWT token
2. Re-fetch user role from DB
3. Check role against endpoint requirements
4. If insufficient: 403 Forbidden + log event
```

---

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
- Store with generated UUID: `{uuid}_{timestamp}.{ext}`

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

**Audit Log Table:**
```
audit_logs: id, user_id, operation, entity_type, entity_id,
            old_values (JSON), new_values (JSON),
            ip_address, user_agent, created_at

Index on: created_at, user_id, entity_type
```

**Retention:** Indefinite (per requirements)

---

## Threat Mitigations Applied

| Threat | Mitigation | Implementation |
|--------|-----------|-----------------|
| Brute force | Rate limiting + lockout | 5/min per user, 20/hour per IP, 15min lock |
| Token forgery | HS256 signature | Validate signature + expiry on every request |
| Session hijacking | HTTPS + HttpOnly | Phase 2 deployment |
| Role escalation | Re-validate from DB | Always fetch role from DB, never trust token |
| Unauth access | Project membership checks | Query filters + endpoint authorization |
| XSS | Output encoding | HTML entity encoding on comment display |
| Malicious files | Whitelist + MIME check | Type validation, no execution possible |
| SQL injection | Parameterized queries | All DB queries use prepared statements |
| Path traversal | Filename validation | UUID storage + sanitization |
| Sensitive in logs | Log scrubbing | Never log passwords, tokens, PII |

---

## 1. Authentication

**Flow:**
1. User sends `POST /auth/login` with username/password

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

### 2.2 Authorization Implementation

**Pattern: Role-Based Authorization at Multiple Levels**

**Level 1: Route-Level Authorization**
```
@Authorize(roles = {"ADMIN", "MANAGER"})
@PostMapping("/projects")
public ResponseEntity<Project> createProject(@RequestBody ProjectDto dto) {
  // Only ADMIN and MANAGER can reach this endpoint
  return projectService.createProject(dto);
}
```

**Level 2: Endpoint-Level Authorization**
```
@GetMapping("/projects/{id}")
public ResponseEntity<Project> getProject(
    @PathVariable int id,
    @CurrentUser User user) {
  
  Project project = projectService.getProject(id);
  
  // Verify user is member of project
  if (!projectService.isProjectMember(user.id, project.id)) {
    throw new ForbiddenException("Not a project member");
  }
  
  return ResponseEntity.ok(project);
}
```

**Level 3: Service-Level Authorization**
```
public void updateProject(int projectId, ProjectDto updates, int userId) {
  Project project = projectRepository.findById(projectId);
  User user = userRepository.findById(userId);
  
  // Permission checks
  if (user.role == Role.MEMBER) {
    throw new ForbiddenException("Members cannot update projects");
  }
  
  if (user.role == Role.MANAGER && project.createdBy != userId) {
    throw new ForbiddenException("Managers can only update own projects");
  }
  
  // Proceed with update
  project.update(updates);
  projectRepository.save(project);
}
```

### 2.3 Project-Level Access Control

**Principle:** Users can only access projects explicitly assigned to them

**Implementation:**
```
// Verify project membership before any operation
Query: SELECT * FROM projects p
       INNER JOIN project_members pm ON p.id = pm.project_id
       WHERE p.id = ? AND pm.user_id = ? AND p.deleted_at IS NULL

// If query returns empty: user not in project → FORBIDDEN
// If query returns row: user is member → ALLOWED
```

### 2.4 Task-Level Access Control

**Ownership Rules:**
- **Creator/Assignee:** Can edit own tasks
- **Manager:** Can edit any task in project
- **Admin:** Can edit any task globally
- **Others:** Read-only

**Implementation:**
```
function canEditTask(userId, taskId) {
  task = getTask(taskId)
  user = getUser(userId)
  
  // Admin can edit anything
  if (user.role == ADMIN) return true
  
  // Manager can edit tasks in own projects
  if (user.role == MANAGER) {
    if (isProjectOwner(userId, task.projectId)) return true
  }
  
  // Member can edit own tasks
  if (user.role == MEMBER) {
    if (task.createdBy == userId || task.assignedTo == userId) return true
  }
  
  return false
}
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

**Examples:**

**Example 1: Username Validation**
```
Field: username
Rules:
- Type: string
- Length: 3-50 characters
- Pattern: ^[a-zA-Z0-9_-]+$  (alphanumeric, underscore, hyphen only)
- Uniqueness: checked against database

Regex: ^[a-zA-Z0-9_-]{3,50}$
```

**Example 2: Email Validation** (if used)
```
Field: email
Rules:
- Type: string
- Pattern: RFC 5322 email format
- Uniqueness: checked against database

Regex: ^[^\s@]+@[^\s@]+\.[^\s@]+$
```

**Example 3: Task Title Validation**
```
Field: title
Rules:
- Type: string
- Length: 1-255 characters
- Disallow: null/empty
- No restrictions on content (sanitized on output)
```

**Example 4: Task Status Validation**
```
Field: status
Rules:
- Type: enum
- Allowed values: TODO, IN_PROGRESS, DONE
- Reject any other value
```

**Example 5: File Upload Validation**
```
Field: file
Rules:
- Extension whitelist: {.pdf, .doc, .docx, .xls, .xlsx, .ppt, .pptx, .jpg, .jpeg, .png, .gif, .txt, .csv}
- MIME type whitelist: {application/pdf, application/msword, application/vnd.openxmlformats-officedocument.*, image/*, text/plain, text/csv}
- File size: ≤ 25 MB
- Name: sanitize (remove path traversal characters)

Pseudo-code:
function validateFileUpload(file) {
  // Check extension
  ext = getExtension(file.name).toLowerCase()
  if ext NOT in ALLOWED_EXTENSIONS:
    throw ValidationError("File type not allowed")
  
  // Check MIME type
  actualMimeType = detectMimeType(file.content)
  expectedMimeType = MIME_TYPE_MAP[ext]
  if actualMimeType NOT in expectedMimeType:
    throw ValidationError("File type mismatch (content doesn't match extension)")
  
  // Check size
  if file.size > 25_MB:
    throw ValidationError("File exceeds 25MB limit")
  
  // Sanitize filename
  sanitizedName = sanitizeFilename(file.name)
  
  return true
}
```

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

**Implementation:**

**Example 1: Comment Display**
```
User input: <script>alert('XSS')</script>
Stored in DB: <script>alert('XSS')</script>  (unchanged)

When displaying:
function displayComment(comment) {
  encodedContent = htmlEncode(comment.content)
  
  // encodedContent = &lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;
  
  return `<div class="comment">${encodedContent}</div>`
}

Browser receives: <div class="comment">&lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;</div>
Rendered as: <div class="comment"><script>alert('XSS')</script></div>  (literal text, not executed)
```

**Example 2: Java Implementation**
```java
import org.apache.commons.lang3.StringEscapeUtils;

public String displayComment(Comment comment) {
  String encodedContent = StringEscapeUtils.escapeHtml4(comment.getContent());
  return "<div class='comment'>" + encodedContent + "</div>";
}

// Alternative: Use Apache Commons Text
import org.apache.commons.text.StringEscapeUtils;
String encoded = StringEscapeUtils.escapeHtml4(userInput);
```

**Example 3: Content Security Policy (Phase 2)**
```
Response Header:
Content-Security-Policy: 
  default-src 'self';
  script-src 'self';
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https:;
  font-src 'self';
  connect-src 'self';
  frame-ancestors 'none';
  object-src 'none';
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

**Upload Endpoint:**
```
POST /projects/{projectId}/tasks/{taskId}/attachments

Authorization: Bearer <token>
Content-Type: multipart/form-data

Validation:
1. Authenticate user (token validation)
2. Verify user is project member
3. Verify task exists in project
4. Validate file (type, size, content)
5. Sanitize filename
6. Store file with unique name
7. Create attachment record in DB
8. Log upload event
9. Return 201 Created with attachment metadata
```

### 5.2 File Download Security

**Download Endpoint:**
```
GET /projects/{projectId}/tasks/{taskId}/attachments/{attachmentId}/download

Authorization: Bearer <token>

Validation:
1. Authenticate user (token validation)
2. Verify user is project member
3. Verify attachment exists in task
4. Verify task in project
5. Serve file from secure location (outside web root)
6. Log download access
7. Return 200 OK with appropriate Content-Disposition header
```

### 5.3 File Deletion Security

**Soft-Delete Process:**
```
DELETE /projects/{projectId}/tasks/{taskId}/attachments/{attachmentId}

Authorization: Bearer <token>

Validation:
1. Authenticate user
2. Verify project membership
3. Verify attachment ownership (uploader or manager/admin)
4. Update attachment: deletedAt = now, deletedBy = userId
5. Do NOT delete physical file (keep for audit/recovery)
6. Log deletion
7. Return 204 No Content
```

---

## 6. Audit Logging & Monitoring

### 6.1 Logging Architecture

**Centralized Audit Log Table:**
```sql
CREATE TABLE audit_logs (
  id SERIAL PRIMARY KEY,
  user_id INTEGER,
  operation VARCHAR(20),  -- CREATE, UPDATE, DELETE, LOGIN, LOGOUT, AUTHFAIL
  entity_type VARCHAR(50), -- User, Project, Task, Comment, Attachment
  entity_id INTEGER,
  old_values JSON,
  new_values JSON,
  ip_address VARCHAR(45),
  user_agent VARCHAR(500),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_timestamp (timestamp),
  INDEX idx_user_id (user_id),
  INDEX idx_operation (operation)
);
```

### 6.2 What Gets Logged

**Security Events (Always):**
- Authentication attempts (success/failure with IP)
- Authorization failures
- Role changes
- User deletions (soft)
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
- Passwords (plaintext or hashed)
- Authentication tokens
- API keys or secrets
- File contents
- Unnecessary verbose request/response bodies

### 6.3 Logging Implementation

**Example: Project Creation Audit Log**
```java
public void logProjectCreation(Project project, User creator, String ip) {
  AuditLog log = new AuditLog();
  log.setUserId(creator.getId());
  log.setOperation("CREATE");
  log.setEntityType("Project");
  log.setEntityId(project.getId());
  
  // JSON representation of new project (without sensitive data)
  Map<String, Object> newValues = new HashMap<>();
  newValues.put("title", project.getTitle());
  newValues.put("description", project.getDescription());
  newValues.put("createdAt", project.getCreatedAt());
  
  log.setNewValues(new JSONObject(newValues).toString());
  log.setIpAddress(ip);
  log.setUserAgent(getUserAgent());
  log.setTimestamp(new Date());
  
  auditRepository.save(log);
}
```

---

## 7. Security Configuration Summary

| Aspect | Configuration | Phase |
|---|---|---|
| Authentication | JWT + Bcrypt | 1 |
| Password Storage | Bcrypt ($2a$12$) | 1 |
| Token Expiration | 24 hours | 1 |
| Token Algorithm | HS256 | 1 |
| Rate Limiting | 5/min per user, 20/hour per IP | 1 |
| HTTPS | HTTP allowed (dev), HTTPS required (Phase 2) | 2 |
| TLS Version | 1.2+ required | 2 |
| Input Validation | Whitelist-based | 1 |
| Output Encoding | HTML entity encoding | 1 |
| CSP Headers | Strict policy (Phase 2) | 2 |
| File Storage | Outside web root, OS permissions 0640 | 1 |
| File Size Limit | 25MB max | 1 |
| File Types | Whitelist of 12 types | 1 |
| Audit Logging | Centralized, comprehensive | 1 |
| Soft-Delete | Logical delete with audit trail | 1 |
| SQL Protection | Parameterized queries only | 1 |
| XSS Protection | Output encoding, CSP | 1 |

