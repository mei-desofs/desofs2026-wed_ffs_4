# Architecture

## Layered Design

```
┌────────────────────┐
│  API Controllers   │ (REST endpoints)
├────────────────────┤
│  Services          │ (Auth, Project, Task, Comment, File, Audit)
├────────────────────┤
│  Repositories      │ (Data access, parameterized queries)
├────────────────────┤
│  Database │ Filesystem │ Audit Log
└────────────────────┘
```

## Components

**API Layer:**
- AuthController (login, logout, refresh)
- ProjectController (CRUD, members)
- TaskController (CRUD, status)
- CommentController (CRUD, nested)
- AttachmentController (upload, download)

**Service Layer:**
- AuthService: JWT validation, bcrypt
- ProjectService: RBAC checks, soft-delete
- TaskService: Assignment, status workflow
- CommentService: Output encoding
- FileService: Validation, storage, access control
- AuditService: Log operations

**Data Access:**
- UserRepository, ProjectRepository, TaskRepository, CommentRepository, AttachmentRepository
- All use parameterized queries
- Soft-delete filters on all queries

## Database Schema

```
users (id, username, password_hash, role, createdBy, updatedBy, deletedAt, deletedBy)
projects (id, title, description, created_by, createdAt, updatedAt, deletedAt, deletedBy)
project_members (id, project_id, user_id, role_in_project, joined_at)
tasks (id, project_id, title, description, assigned_to, status, priority, 
       created_by, createdAt, updatedAt, deletedAt, deletedBy)
comments (id, task_id, parent_id, content, author_id, createdAt, updatedAt, deletedAt, deletedBy)
attachments (id, task_id, original_filename, stored_filename, file_size, mime_type,
             uploaded_by, uploadedAt, deletedAt, deletedBy)
audit_logs (id, user_id, operation, entity_type, entity_id, old_values, new_values, 
            ip_address, user_agent, created_at)
```

## File Storage

```
/var/opt/teuxdeux/storage/projects/{projectId}/tasks/{taskId}/attachments/
├── {uuid}_{timestamp}.pdf
├── {uuid}_{timestamp}.jpg
└── {uuid}_{timestamp}.docx
```

- Files stored outside web root (NOT directly accessible)
- Named with UUID to prevent guessing
- Served via authenticated API endpoint only

## Indexes

**Performance:**
- users.username, projects.created_by, tasks.project_id
- tasks.assigned_to, comments.task_id, attachments.task_id

**Soft-Delete:**
- projects.deleted_at, tasks.deleted_at, comments.deleted_at
- attachments.deleted_at

**Audit:**
- audit_logs.created_at, audit_logs.user_id

## API Response Format

**Success:**
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation successful"
}
```

**Error:**
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid token"
  }
}
```

## Authentication Headers

```
Authorization: Bearer <JWT_TOKEN>
```

**JWT Payload:**
```json
{
  "sub": "userId",
  "iat": 1234567890,
  "exp": 1234571490,
  "role": "MANAGER",
  "aud": "teuxdeux-api"
}
```

## Security Layers

1. **Network:** HTTPS/TLS (Phase 2)
2. **Authentication:** JWT tokens, bcrypt passwords
3. **Authorization:** RBAC middleware on all endpoints
4. **Input Validation:** Type checks, whitelist file types
5. **Data Access:** Parameterized queries, soft-delete enforcement

## Phase 2+ Scalability

- Horizontal: Load balancer, stateless API, Redis for sessions
- Database: Master-slave replication
- Storage: NAS or distributed (S3-like)
- Performance: Caching (Redis), query optimization
- CDN for static files
