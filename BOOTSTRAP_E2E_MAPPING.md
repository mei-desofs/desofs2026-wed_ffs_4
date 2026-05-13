# Bootstrap Data & E2E Test Mapping

## Overview
This document maps bootstrap data (test users) to end-to-end test scenarios. All tests leverage bootstrap users seeded on application startup for consistent, reproducible E2E test flows.

## Bootstrap Users

### User 1: Admin
- **Email**: `admin@example.com`
- **Password**: `password123`
- **Role**: `ADMIN`
- **ID**: Auto-generated (typically 1L)

### User 2: Manager
- **Email**: `manager@example.com`
- **Password**: `password123`
- **Role**: `MANAGER`
- **ID**: Auto-generated (typically 2L)

### User 3: Regular User
- **Email**: `user@example.com`
- **Password**: `password123`
- **Role**: `USER`
- **ID**: Auto-generated (typically 3L)

## Authentication Flow (E2E)

1. **Register**: POST `/auth/register` with new email → 201 Created
2. **Login**: POST `/auth/login` with bootstrap user credentials → 200 OK + JWT token
3. **Authenticated Requests**: Include `Authorization: Bearer <token>` in headers

## Project CRUD Endpoints

All endpoints require JWT authentication. Ownership is enforced via user ID from JWT token.

### CREATE Project
**Endpoint**: `POST /api/projects`
**Auth**: Required (any authenticated user)
**Request Body**:
```json
{
  "name": "My Project",
  "description": "Project details"
}
```
**Response**: 201 Created
```json
{
  "id": 1,
  "name": "My Project",
  "description": "Project details"
}
```
**E2E Test Mapping**:
- Use `admin@example.com` to create projects with ADMIN ownership
- Use `user@example.com` to create projects with USER ownership
- Verify 400/500 for missing fields

### LIST Projects
**Endpoint**: `GET /api/projects`
**Auth**: Required
**Query Params**: None
**Response**: 200 OK
```json
[
  {
    "id": 1,
    "name": "Project 1",
    "description": "Description",
    "owner": {...},
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-01-15T10:30:00"
  }
]
```
**E2E Test Mapping**:
- Login as `admin@example.com` → List should only show projects owned by admin
- Login as `user@example.com` → List should only show projects owned by user
- Verify isolation: admin's list excludes user's projects

### GET Single Project
**Endpoint**: `GET /api/projects/{id}`
**Auth**: Required
**Path Params**: `id` (project ID)
**Response**: 200 OK (if owner) / 403 Forbidden (if not owner) / 404 Not Found (if doesn't exist)
```json
{
  "id": 1,
  "name": "Project 1",
  "description": "Description"
}
```
**E2E Test Mapping**:
- Create project as `admin@example.com`
- GET as `admin@example.com` → 200 OK
- GET as `user@example.com` → 403 Forbidden (not owner)
- GET non-existent ID → 404 Not Found

### DELETE Project
**Endpoint**: `DELETE /api/projects/{id}`
**Auth**: Required
**Path Params**: `id` (project ID)
**Response**: 204 No Content (if owner) / 403 Forbidden (if not owner) / 404 Not Found
**E2E Test Mapping**:
- Create project as `user@example.com`
- DELETE as `user@example.com` → 204 No Content
- Verify project is deleted (GET returns 404)
- Create project as `admin@example.com`
- DELETE as `user@example.com` → 403 Forbidden
- Verify project still exists (GET as admin returns 200)

## Testable Scenarios

### Scenario 1: Single User Project Lifecycle
1. Login as `user@example.com`
2. Create project "My Task List"
3. GET `/api/projects` → Verify 1 project returned
4. GET `/api/projects/{id}` → Verify project details
5. DELETE `/api/projects/{id}` → Verify 204 No Content
6. GET `/api/projects/{id}` → Verify 404 Not Found

### Scenario 2: Multi-User Ownership
1. Login as `admin@example.com`
2. Create project "Admin Project"
3. Login as `user@example.com`
4. GET `/api/projects` → Verify 0 projects (none owned by user)
5. GET `/api/projects/{admin_project_id}` → Verify 403 Forbidden
6. CREATE own project "User Project"
7. Login as `admin@example.com`
8. GET `/api/projects/{user_project_id}` → Verify 403 Forbidden

### Scenario 3: Ownership Verification
1. Login as `manager@example.com`
2. Create project "Manager Project"
3. Try DELETE as `user@example.com` → Verify 403 Forbidden
4. Verify project still exists when listing as manager

## Unit Test Coverage

### ProjectServiceTest
- ✅ `createProjectShouldPersistProjectForOwner()` - Verifies save is called
- ✅ `getUserProjectsShouldReturnProjectsForUserId()` - Verifies filtering by owner ID
- ✅ `deleteProjectShouldRemoveOwnedProject()` - Verifies delete for owner
- ✅ `deleteProjectShouldRejectNonOwner()` - Verifies 403 for non-owner
- ✅ `getProjectByIdShouldReturnOwnedProject()` - Verifies retrieval for owner
- ✅ `getProjectByIdShouldRejectNonOwner()` - Verifies 403 for non-owner
- ✅ `getProjectByIdShouldThrowWhenNotFound()` - Verifies 404 for missing project

### ProjectControllerIT
- ✅ `createProjectShouldReturnCreatedProject()` - POST returns 201 with project details
- ✅ `listProjectsShouldReturnUserProjects()` - GET /api/projects returns user's projects
- ✅ `deleteProjectShouldReturnNoContent()` - DELETE returns 204
- ✅ `getProjectShouldReturnProjectById()` - GET /{id} returns 200 with details
- ✅ `getProjectShouldReturn404WhenNotFound()` - GET /{id} returns 404 for missing
- ✅ `getProjectShouldReturn403WhenNotOwner()` - GET /{id} returns 403 for non-owner
- ✅ `projectEndpointsShouldRejectUnauthenticatedRequests()` - Verify 401 without JWT

## Running E2E Tests

### Using curl (manual)
```bash
# Register/Login flow
JWT=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.token')

# Create project
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Project","description":"Test"}'

# GET single project
curl -H "Authorization: Bearer $JWT" \
  http://localhost:8080/api/projects/1

# DELETE project
curl -X DELETE -H "Authorization: Bearer $JWT" \
  http://localhost:8080/api/projects/1
```

### Using Testcontainers (Future Phase)
```java
@Testcontainers
class ProjectE2ETest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(...)
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");

  // Tests will inherit bootstrap data from DataBootstrap.java
  // No need to manually seed users - they're created on startup
}
```

## Notes
- All bootstrap data is seeded via `DataBootstrap.java` CommandLineRunner bean on application startup
- Password hashing uses BCrypt (BCrypt strength 10)
- JWT tokens expire after 15 minutes
- All endpoints require HTTPS in production (configure in SecurityConfig)
- Current implementation uses simple email-based user lookup; scale to UUID if needed
