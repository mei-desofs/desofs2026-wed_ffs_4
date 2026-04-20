# Architecture

## Layered Design

```
┌─────────────────────────────────────────┐
│  API Controllers                        │ (REST endpoints)
├─────────────────────────────────────────┤
│  Services                               │ (Auth, Project, Task, Comment, File, Audit)
├─────────────────────────────────────────┤
│  Repositories                           │ (Data access, parameterized queries)
├─────────────────────────────────────────┤
│  Database │ Filesystem │ Audit Log      │
└─────────────────────────────────────────┘
```

## Components

**API Layer:**
| Controller | Endpoints |
|---|---|
| `AuthController` | `POST /auth/login`, `POST /auth/logout` |
| `ProjectController` | `GET/POST /projects`, `GET/PUT/DELETE /projects/{id}`, `GET/POST /projects/{id}/members`, `DELETE /projects/{id}/members/{userId}` |
| `TaskController` | `GET/POST /projects/{id}/tasks`, `GET/PUT/DELETE /tasks/{id}`, `PATCH /tasks/{id}/status` |
| `CommentController` | `GET/POST /tasks/{id}/comments`, `PUT/DELETE /comments/{id}` |
| `AttachmentController` | `POST /tasks/{id}/attachments`, `GET /attachments/{id}`, `DELETE /attachments/{id}` |

**Service Layer:**
| Service | Responsibility |
|---|---|
| `AuthService` | JWT creation & validation, bcrypt password verification |
| `ProjectService` | Project CRUD, RBAC checks, soft-delete |
| `TaskService` | Task CRUD, assignment, status workflow, soft-delete |
| `CommentService` | Comment CRUD, output encoding, soft-delete |
| `FileService` | File validation, storage, access control, soft-delete |
| `AuditService` | Audit logging |

**Data Access:**
- UserRepository, ProjectRepository, TaskRepository, CommentRepository, AttachmentRepository
- All use parameterized queries
- Soft-delete filters on all queries

## Database Schema

```
users (
  id, username, email, password_hash, role,
  created_by, updated_by,
  created_at, updated_at, deleted_at, deleted_by
)

projects (
  id, title, description,
  created_by,
  created_at, updated_at, deleted_at, deleted_by
)

project_members (
  id, project_id, user_id,
  role_in_project,
  joined_at
)

tasks (
  id, project_id, title, description,
  assigned_to, status, priority,
  created_by,
  created_at, updated_at, deleted_at, deleted_by
)

comments (
  id, task_id, content, author_id,
  created_at, updated_at, deleted_at, deleted_by
)

attachments (
  id, task_id,
  original_filename, stored_filename,
  file_size, mime_type,
  uploaded_by, uploaded_at, deleted_at, deleted_by
)

audit_logs (
  id, user_id, operation, entity_type, entity_id,
  old_values, new_values,
  ip_address, user_agent, created_at
)
```

## File Storage

```
/var/opt/teuxdeux/storage/
└── projects/{projectId}/
    └── tasks/{taskId}/
        └── attachments/
            ├── {uuid}_{timestamp}.pdf
            ├── {uuid}_{timestamp}.jpg
            └── {uuid}_{timestamp}.docx
```

- Files stored outside web root (NEVER directly accessible via URL)
- Named with UUID to prevent guessing
- Served via authenticated API endpoint only
- Only allowed MIME types (whitelist)
- Max file size: 25MB
  
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

## Authentication

**Header:**
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
**Expiry:**
```
24 hours (deliberate tradeoff — no refresh token mechanism exists)

Known limitation: No server-side token revocation.
Logout = client deletes the token. A stolen token remains valid until expiry.
Accepted for Phase 1. Mitigation: short expiry window.
```
## Security Layers

1. **Network:** HTTPS/TLS (phase 1, self-signed)
2. **Authentication:** JWT (stateless, 24h expiry), bcrypt >= 12 rounds
3. **Authorization:** RBAC middleware on all endpoints
4. **Input Validation:** Type checks, whitelist file types
5. **Data Access:** Parameterized queries, soft-delete enforcement
