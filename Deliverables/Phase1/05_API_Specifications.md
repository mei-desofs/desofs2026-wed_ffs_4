# API Specifications

## Base

- **Base URL:** `http://localhost:8080/api`
- **Auth:** JWT Bearer token in `Authorization: Bearer <token>`
- **Format:** JSON
- **Content-Type:** application/json

## Response Format

**Success (2xx):**
```json
{ "success": true, "data": {...}, "message": "OK" }
```

**Error (4xx/5xx):**
```json
{ "success": false, "error": {"code": "ERR_CODE", "message": "..."} }
```

## Authentication

### POST /auth/login
```json
Request: { "username": "john", "password": "pass123" }
Response: { "token": "jwt...", "expiresIn": 86400, "user": {...} }
Status: 200 OK | 401 Unauthorized | 429 Too Many Requests
```

### POST /auth/logout
```
Auth: Required
Status: 204 No Content
```

### POST /auth/refresh
```json
Request: { "token": "expired_jwt" }
Response: { "token": "new_jwt", "expiresIn": 86400 }
Status: 200 OK | 401 Unauthorized
```

## Users

### GET /users/{id}
```
Auth: Required
Status: 200 OK | 404 Not Found
Response: { user: {id, username, role} }
```

### PUT /users/{id}
```json
Auth: Required
Request: { "username": "new_name" }
Status: 200 OK | 403 Forbidden | 409 Conflict
```

## Projects

### GET /projects
```
Auth: Required
Query: ?status=active|archived (optional)
Status: 200 OK
Response: { data: [{id, title, description, created_by, createdAt}] }
```

### POST /projects
```json
Auth: Required (MANAGER+)
Request: { "title": "Project A", "description": "..." }
Status: 201 Created | 403 Forbidden
Response: { data: {id, title, ...} }
```

### GET /projects/{id}
```
Auth: Required (member of project)
Status: 200 OK | 404 Not Found | 403 Forbidden
Response: { data: {id, title, description, members: [...]} }
```

### PUT /projects/{id}
```json
Auth: Required (MANAGER owner or ADMIN)
Request: { "title": "New Title", "description": "..." }
Status: 200 OK | 403 Forbidden
```

### DELETE /projects/{id}
```
Auth: Required (MANAGER owner or ADMIN)
Status: 204 No Content | 403 Forbidden
(Soft-delete)
```

### GET /projects/{id}/members
```
Auth: Required (project member)
Status: 200 OK
Response: { data: [{user_id, role_in_project, joined_at}] }
```

### POST /projects/{id}/members
```json
Auth: Required (MANAGER owner or ADMIN)
Request: { "user_id": 5, "role": "MANAGER|MEMBER" }
Status: 201 Created | 409 Conflict (duplicate)
```

### DELETE /projects/{id}/members/{user_id}
```
Auth: Required (MANAGER owner or ADMIN)
Status: 204 No Content
```

## Tasks

### GET /projects/{projectId}/tasks
```
Auth: Required (project member)
Query: ?status=TODO|IN_PROGRESS|DONE (optional)
Status: 200 OK
Response: { data: [{id, title, assigned_to, status, createdAt}] }
```

### POST /projects/{projectId}/tasks
```json
Auth: Required (project member)
Request: {
  "title": "Task 1",
  "description": "Do something",
  "assigned_to": 3,
  "priority": "LOW|MEDIUM|HIGH"
}
Status: 201 Created | 400 Bad Request
```

### GET /projects/{projectId}/tasks/{id}
```
Auth: Required (project member)
Status: 200 OK | 404 Not Found
Response: { data: {id, title, description, assigned_to, status, 
                   comments: [...], attachments: [...]} }
```

### PUT /projects/{projectId}/tasks/{id}
```json
Auth: Required (assignee/MANAGER/ADMIN)
Request: { "title": "New title", "assigned_to": 5, "priority": "..." }
Status: 200 OK | 403 Forbidden
```

### PATCH /projects/{projectId}/tasks/{id}/status
```json
Auth: Required (assignee/MANAGER/ADMIN)
Request: { "status": "TODO|IN_PROGRESS|DONE" }
Status: 200 OK | 400 Bad Request
```

### DELETE /projects/{projectId}/tasks/{id}
```
Auth: Required (assignee/MANAGER/ADMIN)
Status: 204 No Content
(Soft-delete cascades to comments/attachments)
```

## Comments

### GET /tasks/{taskId}/comments
```
Auth: Required (project member)
Status: 200 OK
Response: { data: [{id, content, author_id, createdAt, replies: [...]}] }
```

### POST /tasks/{taskId}/comments
```json
Auth: Required (project member)
Request: { "content": "Comment text" }
Status: 201 Created
Response: { data: {id, content, author_id, createdAt} }
```

### PUT /tasks/{taskId}/comments/{id}
```json
Auth: Required (author/MANAGER/ADMIN)
Request: { "content": "Updated comment" }
Status: 200 OK | 403 Forbidden
```

### POST /tasks/{taskId}/comments/{parentId}/reply
```json
Auth: Required (project member)
Request: { "content": "Reply text" }
Status: 201 Created
Response: { data: {id, parent_id, content, author_id, createdAt} }
```

### DELETE /tasks/{taskId}/comments/{id}
```
Auth: Required (author/MANAGER/ADMIN)
Status: 204 No Content
(Soft-delete)
```

## Attachments

### POST /tasks/{taskId}/attachments
```
Auth: Required (project member)
Content-Type: multipart/form-data
Body: file (max 25MB, whitelisted types)
Status: 201 Created | 400 Bad Request | 413 Payload Too Large
Response: { data: {id, filename, size, mime_type, uploadedAt} }
```

### GET /tasks/{taskId}/attachments
```
Auth: Required (project member)
Status: 200 OK
Response: { data: [{id, original_filename, file_size, uploadedAt, uploaded_by}] }
```

### GET /attachments/{id}/download
```
Auth: Required (project member)
Status: 200 OK (file binary) | 403 Forbidden | 404 Not Found
Headers: Content-Disposition: attachment; filename="..."
```

### DELETE /tasks/{taskId}/attachments/{id}
```
Auth: Required (uploader/MANAGER/ADMIN)
Status: 204 No Content
(Soft-delete)
```

## Error Codes

| Code | HTTP | Meaning |
|------|------|---------|
| INVALID_INPUT | 400 | Invalid/missing required fields |
