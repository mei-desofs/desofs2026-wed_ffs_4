# Assets

Assets are the critical resources within the Teuxdeux system that require protection. These include data, infrastructure, and services that have inherent value and could be targets of security threats.

## Assets Definition

| ID | Name | Description | Trust Level | Confidentiality | Integrity | Availability |
|---|---|---|---|---|---|---|
| 1 | User Login Credentials | Username and password combinations used for authentication. Stored as bcrypt hashes with salt rounds ≥ 10. | (2) User with Valid Login Credentials (4) Admin (5) Manager (6) Member | **CRITICAL** - Never logged or transmitted in plaintext | **CRITICAL** - Hash corruption = authentication failure | **HIGH** - Service unavailable without auth |
| 2 | JWT Authentication Tokens | Bearer tokens issued after successful login, valid for 24 hours. Used to authenticate all API requests. | (2) User with Valid Login Credentials (4) Admin (5) Manager (6) Member | **CRITICAL** - Token theft enables account compromise | **HIGH** - Invalid token = access denied | **HIGH** - Token expiration = re-authentication required |
| 3 | User Roles & Permissions | Role assignments (Admin, Manager, Member) determining what actions each user can perform. | (4) Admin (5) Manager (6) Member (9) Database Read/Write User | **HIGH** - Role escalation enables unauthorized access | **CRITICAL** - Incorrect role = wrong permissions | **MEDIUM** - Role data loss = restoration from backup |
| 4 | Project Data | Project metadata including title, description, creation timestamp, ownership information. | (4) Admin (5) Manager (6) Member (9) Database Read/Write User | **HIGH** - Projects may contain sensitive business information | **HIGH** - Data corruption impacts team planning | **HIGH** - Project loss impacts productivity |
| 4.1 | Project Membership List | Assignment of users to projects, determining who can access what. | (4) Admin (5) Manager (6) Member (9) Database Read/Write User | **MEDIUM** - Reveals team structure | **CRITICAL** - Incorrect membership = wrong access | **MEDIUM** - Membership data loss requires restoration |
| 5 | Task Data | Task records including title, description, assigned user, status (TODO/IN_PROGRESS/DONE), priority. | (4) Admin (5) Manager (6) Member (9) Database Read/Write User | **MEDIUM** - Tasks reveal work-in-progress | **HIGH** - Data corruption impacts task tracking | **HIGH** - Task loss impacts project delivery |
| 6 | Comments & Discussions | Text content of comments on tasks, potentially containing sensitive business logic or decisions. | (4) Admin (5) Manager (6) Member (9) Database Read/Write User | **MEDIUM** - Comments may discuss confidential matters | **MEDIUM** - Comment corruption impacts discussion history | **MEDIUM** - Comment loss impacts knowledge retention |
| 7 | File Attachments | Uploaded documents, images, spreadsheets, and other files attached to tasks. May contain confidential information. | (2) User with Valid Login Credentials (4) Admin (5) Manager (6) Member (7) Web Server User Process (9) Database Read/Write User | **HIGH** - Files may contain proprietary or sensitive data | **HIGH** - File corruption impacts work quality | **HIGH** - File loss impacts project continuity |
| 7.1 | File Metadata | Attachment records including filename, size, upload timestamp, uploader identity, MIME type. | (4) Admin (5) Manager (6) Member (8) Database Read User (9) Database Read/Write User | **MEDIUM** - Reveals file usage patterns | **HIGH** - Metadata corruption prevents file retrieval | **MEDIUM** - Metadata loss = orphaned files |
| 8 | Audit Logs | Complete record of all system operations including authentication attempts, data modifications, file access, role changes. | (4) Admin (9) Database Read/Write User | **CRITICAL** - Logs prove/disprove security incidents | **CRITICAL** - Log tampering hides malicious activity | **HIGH** - Log loss prevents forensic investigation |
| 8.1 | Authentication Audit Records | Records of login attempts (successful and failed), logout events, token generation/revocation. | (4) Admin (9) Database Read/Write User | **CRITICAL** - Proves who accessed system when | **CRITICAL** - Tampering masks unauthorized access | **HIGH** - Loss prevents breach investigation |
| 8.2 | Authorization Audit Records | Records of permission checks, role changes, access denials. | (4) Admin (9) Database Read/Write User | **CRITICAL** - Proves enforcement of access control | **CRITICAL** - Tampering masks privilege escalation | **HIGH** - Loss prevents privilege analysis |
| 8.3 | Data Modification Audit Records | Records of all CREATE, UPDATE, DELETE operations with old/new values. | (4) Admin (9) Database Read/Write User | **CRITICAL** - Tracks who changed what and when | **CRITICAL** - Tampering hides unauthorized changes | **HIGH** - Loss prevents change audit |
| 9 | Database (PostgreSQL) | Primary storage containing all user, project, task, comment, attachment, and audit log data. | (8) Database Read User (9) Database Read/Write User (5) Database Server Administrator | **CRITICAL** - Breach exposes all application data | **CRITICAL** - Corruption destroys system integrity | **CRITICAL** - Unavailability stops application |
| 9.1 | Encryption Keys (Phase 2) | Master key for encrypting sensitive data fields (if implemented in Phase 2). | (5) Database Server Administrator (9) Database Read/Write User | **CRITICAL** - Key compromise enables decryption of all data | **CRITICAL** - Key loss makes encrypted data inaccessible | **CRITICAL** - Key unavailability = no operations |
| 10 | API Server (Application) | Spring Boot/Java application running REST API endpoints. | (7) Web Server User Process (5) Database Server Administrator | **HIGH** - Source code contains security logic | **CRITICAL** - Corruption = application malfunction | **CRITICAL** - Downtime stops all users |
| 10.1 | Application Configuration | Environment variables, database credentials, JWT secret, salt rounds, rate limits, file upload whitelist. | (7) Web Server User Process (5) Database Server Administrator | **CRITICAL** - Config contains secrets and security settings | **CRITICAL** - Wrong config = misconfigured security | **MEDIUM** - Config loss requires restoration from version control |
| 10.2 | JWT Secret Key | Signing key used to generate and validate JWT tokens. Must be strong and secret. | (7) Web Server User Process (5) Database Server Administrator | **CRITICAL** - Key compromise enables token forgery | **CRITICAL** - Key compromise = access control bypass | **MEDIUM** - Key loss requires token refresh on new key |
| 11 | File Storage Directory | /var/opt/teuxdeux/storage/ containing all uploaded attachments. | (7) Web Server User Process (10) Filesystem (5) Database Server Administrator | **HIGH** - Unauthorized access = confidential data exposure | **HIGH** - File corruption/deletion = data loss | **HIGH** - Directory unavailability = no uploads/downloads |
| 12 | System Logs (Application & OS) | Application logs and operating system logs containing request/response records, errors, performance metrics. | (7) Web Server User Process (5) Database Server Administrator | **MEDIUM** - Logs may reveal system architecture or vulnerabilities | **HIGH** - Log corruption = loss of diagnostic information | **MEDIUM** - Log loss prevents troubleshooting |

## Asset Protection Strategy

### Confidentiality Protection
- **User Credentials:** Bcrypt hashing with salt ≥ 10, never logged or transmitted plaintext
- **JWT Tokens:** 24-hour expiration, blacklist on logout, HTTPS transmission (Phase 2)
- **Project/Task Data:** Project isolation enforced via membership checks, role-based filtering
- **Files:** Stored outside web root with UUID names, served only to authenticated/authorized members
- **Audit Logs:** Admin-only access, never contains passwords/tokens
- **Database:** Network isolation, restricted access to designated users only

### Integrity Protection
- **Passwords:** Bcrypt hashing prevents reverse lookup; bcrypt validates hash integrity
- **Data Modifications:** Parameterized SQL queries prevent injection; ACID transactions ensure consistency
- **Audit Logs:** Append-only table prevents tampering; indexes on critical columns
- **Files:** MIME type + extension validation before storage; filesystem permissions (0640)
- **Soft-Deletes:** Logical deletion preserves data for audit/recovery; queries filter deleted records

### Availability Protection
- **Authentication:** Rate limiting prevents brute force attacks; 15-minute lockout after 5 failures
- **Database:** ACID compliance ensures data consistency; indexes on frequently queried columns
- **Files:** 25MB size limit prevents storage exhaustion; rate-limiting on uploads (10 files/hour)
- **Audit Logs:** Indefinite retention enables historical analysis; periodic backup/archival (Phase 2)
- **Application:** Deployed on web server with restart mechanisms; database replication (Phase 2)