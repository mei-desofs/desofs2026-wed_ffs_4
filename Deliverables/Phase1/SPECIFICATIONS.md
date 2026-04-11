# Teuxdeux - Technical Specifications

## Stack
- **Backend:** TBD (Spring Boot, Node, Django, etc.)
- **Database:** PostgreSQL/MySQL (relational, no in-memory)
- **API:** REST only

## Roles & Permissions

*See 04_SecurityDesign.md for complete RBAC matrix and authorization rules*

| Role    | Quick Reference |
|---------|-----------------|
| Admin   | Full system access |
| Manager | Own projects + team mgmt |
| Member  | Assigned tasks only |

### Authentication & Authorization

*Detailed in 04_SecurityDesign.md*
- **Method:** JWT + Bcrypt (decided)
- **Token Expiry:** 24 hours (decided)

---

## Data Management

### File Storage

- **Location:** Local filesystem (not cloud)
- **Organization:** `/storage/projects/{projectId}/tasks/{taskId}/attachments/`
- **File Size Limit:** 25 MB per file
- **Accepted File Types:**
  - Documents: `.pdf`, `.doc`, `.docx`, `.xls`, `.xlsx`, `.ppt`, `.pptx`
  - Images: `.jpg`, `.jpeg`, `.png`, `.gif`
  - Other: `.txt`, `.csv` (may expand per requirements)
- **Validation:** Whitelist-based file type checking (extension + MIME type verification)

### Soft Delete Policy

*See 04_SecurityDesign.md for implementation details*
- **Method:** Logical delete with `deletedAt` + `deletedBy` fields
- **Scope:** All entities

### Audit Trail

*See 04_SecurityDesign.md for comprehensive audit logging*
- **Fields:** `createdBy`, `createdAt`, `updatedBy`, `updatedAt`, `deletedBy`, `deletedAt`

---

## Security Requirements

*See 04_SecurityDesign.md for comprehensive security implementation*
- Bcrypt password hashing (salt ≥ 10)
- JWT authentication (24h expiry)
- RBAC authorization (Admin/Manager/Member)
- Whitelist-based file validation
- Soft-delete on all entities
- Parameterized SQL queries
- Input validation + output encoding
- Centralized audit logging

## Core Entities (DDD)
- **User:** Credentials + Role
- **Project:** Title, Description, Members
- **ProjectMember:** User-Project Association
- **Task:** Title, Description, Status, Assignee
- **Comment:** Content, Nested Replies
- **Attachment:** File + Metadata
- **TaskStatus:** Enum (TODO, IN_PROGRESS, DONE)
- **AuditLog:** Security Events

---

## Testing Strategy

*See 06_SecurityTesting.md for complete test plan*
- Unit & integration tests
- Security testing (auth, authz, input validation)
- Abuse case scenarios

---

## Standards & Threat Modeling

*See 02_ThreatModeling.md for STRIDE analysis*
- OWASP Top 10
- ASVS Level 1
- 18 threats identified → 16 mitigations mapped

---

## Known TBDs & Future Decisions

1. Technology stack finalization (language, framework, database)
2. Token/session strategy for authentication
3. Password policy requirements
4. API rate limiting thresholds
5. Soft-delete data retention period
6. File upload encryption at rest requirements
7. PDF generation for Phase 1 report
8. Database migration strategy
