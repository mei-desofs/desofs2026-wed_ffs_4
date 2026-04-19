# Exit Points

Exit points represent the interfaces through which the Teuxdeux system returns data or performs actions that affect external systems or users. These are critical control points for data integrity and confidentiality.

## Exit Points Definition

| ID | Name | Description | Trust Level |
|---|---|---|---|
| 1 | API Response (Success) | JSON responses containing project, task, comment, or file metadata returned to authenticated users. Data is filtered based on user permissions and role. | (2) User with Valid Login Credentials (4) Admin (5) Manager (6) Member |
| 1.1 | JSON Response Format | Standardized API responses with success flag, data payload, and messages. No sensitive data (passwords, tokens) included. | (2) User with Valid Login Credentials (4) Admin (5) Manager (6) Member |
| 1.2 | File Download Response | Files served from authenticated endpoint with Content-Disposition headers. Original filename served, but actual file stored with UUID to prevent guessing. | (2) User with Valid Login Credentials (4) Manager (6) Member (7) Filesystem |
| 1.3 | Error Response | Standardized error responses with error code and message. Error details do not leak sensitive information or stack traces. | (1) Anonymous Web User (2) User with Valid Login Credentials (3) User with Invalid Login Credentials (4) Admin (5) Manager (6) Member |
| 2 | Database Query Results | Data retrieved from PostgreSQL database and returned to API layer. Results are filtered by role-based access control and soft-delete status. | (8) Database Read User (9) Database Read/Write User |
| 2.1 | User Data Queries | SELECT queries on users table filtered by authentication context. Password hashes never returned to API layer. | (8) Database Read User (9) Database Read/Write User |
| 2.2 | Project Data Queries | SELECT queries on projects and project_members tables. Users see only projects they are members of. | (8) Database Read User (9) Database Read/Write User |
| 2.3 | Task Data Queries | SELECT queries on tasks table. Results filtered by project membership and visibility rules. | (8) Database Read User (9) Database Read/Write User |
| 2.4 | Audit Log Queries | SELECT queries on audit_logs table. Results filtered by user role and access level. Admins see all logs; users see limited logs. | (8) Database Read User (9) Database Read/Write User |
| 3 | File System Operations | Files written to and read from /var/opt/teuxdeux/storage/. Files stored with UUID naming to prevent direct access or traversal. | (7) Web Server User Process (10) Filesystem |
| 3.1 | File Write Operation | Files uploaded by users are persisted to filesystem with restricted permissions (0640). Original filename sanitized; storage filename is UUID. | (7) Web Server User Process (10) Filesystem |
| 3.2 | File Read Operation | Files retrieved from filesystem during download. Access logs recorded in audit_logs table. | (7) Web Server User Process (10) Filesystem |
| 3.3 | File Deletion | Soft-deletion: deletedAt timestamp set in database, physical file retained for audit/recovery purposes. | (7) Web Server User Process (9) Database Read/Write User |
| 4 | Audit Log Entries | Append-only entries written to audit_logs table recording all operations, failures, and security events. | (9) Database Read/Write User |
| 4.1 | Authentication Audit | Records of successful/failed login attempts including IP, timestamp, username. | (9) Database Read/Write User |
| 4.2 | Authorization Audit | Records of RBAC decisions, permission denials, and role changes. | (9) Database Read/Write User |
| 4.3 | Data Modification Audit | Records of CREATE, UPDATE, DELETE operations on entities with old/new values. Passwords/tokens excluded. | (9) Database Read/Write User |

## Exit Point Data Protection

All exit points enforce:
- **Output Encoding:** HTML entity encoding on user-generated content to prevent XSS
- **Data Filtering:** Role-based filtering ensures users see only authorized data
- **Soft-Delete Enforcement:** Deleted records excluded from all queries (WHERE deletedAt IS NULL)
- **Sensitive Data Exclusion:** Passwords, tokens, and PII never included in responses
- **Audit Logging:** All data exits logged with user context and timestamp
- **Rate Limiting (Phase 2):** Response throttling for resource-intensive queries