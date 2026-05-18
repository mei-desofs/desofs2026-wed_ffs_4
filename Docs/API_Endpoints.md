# API Endpoints

Document listing all endpoints currently available in the project.

## Authentication

| Method | Path | Description | Access |
|---|---|---|---|
| POST | /auth/register | Registers a user. Accepts `username` or `email` + `password`. | Public |
| POST | /auth/login | Authenticates a user. Accepts `username` or `email` + `password`. | Public |
| POST | /auth/refresh | Generates new tokens from a `refreshToken`. | Public |
| POST | /auth/logout | Invalidates the current token. | Authenticated |

## Admin users

| Method | Path | Description | Access |
|---|---|---|---|
| PUT | /api/admin/users/{id}/role | Updates a user role (`ADMIN`, `MANAGER`, `USER`). | ADMIN |

## Projects

| Method | Path | Description | Access |
|---|---|---|---|
| POST | /api/projects | Creates a project. | ADMIN |
| GET | /api/projects | Lists projects visible to the authenticated user. | Authenticated |
| GET | /api/projects/{id} | Gets project details. | Authenticated |
| PUT | /api/projects/{id} | Updates a project name/description. | ADMIN, MANAGER |
| DELETE | /api/projects/{id} | Soft-deletes a project. | ADMIN |

## Project members

| Method | Path | Description | Access |
|---|---|---|---|
| POST | /api/projects/{projectId}/members | Adds a member to the project by email. | ADMIN, MANAGER |
| GET | /api/projects/{projectId}/members | Lists project members. | ADMIN, MANAGER, member |
| DELETE | /api/projects/{projectId}/members | Removes a member by email. | ADMIN, MANAGER |
| DELETE | /api/projects/{projectId}/members/{memberId} | Removes a member by id. | ADMIN, MANAGER |

## Tasks

| Method | Path | Description | Access |
|---|---|---|---|
| POST | /api/projects/{projectId}/tasks | Creates a task inside a project. | Authenticated member |
| GET | /api/projects/{projectId}/tasks | Lists project tasks. | Authenticated member |
| PUT | /api/projects/{projectId}/tasks/{taskId} | Updates a task title and description. | Authenticated member |
| PATCH | /api/projects/{projectId}/tasks/{taskId}/status | Updates the task status. | Authenticated member |
| DELETE | /api/projects/{projectId}/tasks/{taskId} | Soft-deletes a task. | Authenticated member |
| PATCH | /api/projects/{projectId}/tasks/{taskId}/assignee | Assigns or removes the task assignee. | Authenticated member |

## Comments

| Method | Path | Description | Access |
|---|---|---|---|
| POST | /api/tasks/{taskId}/comments | Adds a comment to a task. | Authenticated member |
| GET | /api/tasks/{taskId}/comments | Lists task comments. | Authenticated member |
| PUT | /api/tasks/{taskId}/comments/{commentId} | Updates a comment. | Authenticated member |
| DELETE | /api/tasks/{taskId}/comments/{commentId} | Deletes a comment. | Authenticated member |

## Attachments

| Method | Path | Description | Access |
|---|---|---|---|
| GET | /api/tasks/{taskId}/attachments | Lists task attachments. | Authenticated member |
| POST | /api/tasks/{taskId}/attachments | Uploads an attachment (`multipart/form-data`, field `file`). | Authenticated member |
| GET | /api/attachments/{id}/download | Downloads an attachment. | Authenticated member |
| DELETE | /api/tasks/{taskId}/attachments/{id} | Deletes an attachment. | Authenticated member |

## Notes

- Endpoints marked as "member" require the user to belong to the project.
- The backend uses JWT authentication.
- The endpoint `/api/projects/{projectId}/members` supports both email and id removal variants.
- This document reflects the endpoints currently present in the source code.
