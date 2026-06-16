# Postman Collection

## Files
- `DESOFS_API.postman_collection.json` — Full API collection with all endpoints
- `DESOFS_Local.postman_environment.json` — Local environment variables

## How to Import
1. Open Postman
2. Click **Import**
3. Select both `.json` files

## Seeded Test Users
| Email | Password | Role |
|---|---|---|
| admin@example.com | password123 | ADMIN |
| manager@example.com | password123 | MANAGER |
| user@example.com | password123 | USER |

## Demo Flow
The `Demo Flow` folder contains an automated sequence:
1. Login as admin
2. Create Project
3. Create Task
4. Add Comment

To run: Click the folder → Run 