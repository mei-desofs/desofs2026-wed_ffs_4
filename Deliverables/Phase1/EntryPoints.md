# Entry Points

Entry points represent the interfaces through which external users or systems interact with the Teuxdeux application. These are the initial access vectors that require authentication and authorization validation.

## Entry Points Definition

Trust level values in this table follow [TrustLevels.md](TrustLevels.md).

| ID | Name | Description | Trust Level |
|---|---|---|---|
| 1 | HTTPS Port (Phase 2) / HTTP Port (Phase 1) | The Teuxdeux API is accessible via HTTP (development) and will be HTTPS only in production. All API requests pass through this entry point. | TL0, TL1, TL2 |
| 1.1 | Login Endpoint | POST /auth/login - Users must authenticate with valid username and password credentials to obtain a JWT token. This is the primary authentication entry point. | TL0, TL1 |
| 1.2 | File Upload Endpoint | POST /projects/{projectId}/tasks/{taskId}/attachments - Project members can upload files to tasks. Requires authentication and project membership. | TL1, TL2 |
| 1.3 | File Download Endpoint | GET /projects/{projectId}/tasks/{taskId}/attachments/{attachmentId}/download - Project members can download previously uploaded files. Requires authentication and project membership. | TL1, TL2 |
| 1.4 | Project API Endpoints | POST /projects, GET /projects, PUT /projects/{id}, DELETE /projects/{id} - Management of projects. Requires authentication and appropriate role (Manager/Admin). | TL1, TL2 |
| 1.5 | Task API Endpoints | POST /projects/{id}/tasks, GET /projects/{id}/tasks, PUT /projects/{id}/tasks/{tId}, DELETE /projects/{id}/tasks/{tId} - Management of tasks within projects. Requires authentication and project membership. | TL1, TL2 |
| 1.6 | Comments API Endpoints | POST /tasks/{id}/comments, PUT /tasks/{id}/comments/{cId}, DELETE /tasks/{id}/comments/{cId} - Members can add, edit, and delete comments on tasks. Requires authentication and project membership. | TL1, TL2 |
| 1.7 | Token Refresh Endpoint | POST /auth/refresh (Phase 2) - Users can refresh expired JWT tokens without re-authenticating. | TL1, TL2 |
| 1.8 | Logout Endpoint | POST /auth/logout - Users terminate their authenticated session. Token is blacklisted. | TL1, TL2 |
| 1.9 | Project Members Management | POST /projects/{id}/members, DELETE /projects/{id}/members/{userId} - Managers and admins manage project membership. | TL1, TL2 |

## Entry Point Security Requirements

All entry points enforce:
- **Authentication:** JWT Bearer token validation on Authorization header
- **Input Validation:** Type, format, length, and range validation before processing
- **Rate Limiting:** Maximum 5 failed attempts per minute per user, 20 per hour per IP
- **HTTPS (Phase 2):** All communications encrypted via TLS 1.2+
- **Audit Logging:** All requests logged with user ID, operation type, timestamp, IP address