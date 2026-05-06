# Teuxdeux — Spring Boot API (Phase 2)

Teuxdeux is a collaborative task management platform built for teams. This is the Phase 2 implementation: a Spring Boot REST API with JWT authentication and projects management.

---

## Quick Start

### 1. Start Database (Docker)
```bash
docker-compose up -d
```

### 2. Build & Run
```bash
./mvnw clean install
./mvnw spring-boot:run
```

App starts on `http://localhost:8080`.

---

## API Endpoints

### Auth (Public)
- **Register**: `POST /auth/register`
  ```bash
  curl -X POST http://localhost:8080/auth/register \
    -H "Content-Type: application/json" \
    -d '{"email":"user@example.com","password":"password123"}'
  ```
  Response: `{"id": 1, "email": "user@example.com"}`

- **Login**: `POST /auth/login`
  ```bash
  curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"user@example.com","password":"password123"}'
  ```
  Response: `{"token": "eyJhbGc..."}`

### Projects (Authenticated)
- **Create Project**: `POST /api/projects`
  ```bash
  TOKEN="<token_from_login>"
  curl -X POST http://localhost:8080/api/projects \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"name":"My Project","description":"Project description"}'
  ```
  Response: `{"id": 1, "name": "My Project", ...}`

- **List Projects**: `GET /api/projects`
  ```bash
  curl -X GET http://localhost:8080/api/projects \
    -H "Authorization: Bearer $TOKEN"
  ```

---

## Tech Stack
- Spring Boot 3.1.4
- Spring Security + JWT (JJWT)
- Spring Data JPA
- PostgreSQL 15
- Maven

---

## Team

**Project:** Teuxdeux — Task Management App

**Team ID:** `wed_ffs_4`

### Members

| Name             | Student ID | Class |
| ---------------- | ---------- | ----- |
| Milan Marcinco   | 1252431    | M1C   |
| Agata Szysiak    | 1252365    | M1C   |
| Filipe Magalhães | 1211606    | M1C   |
| Tiago Pinto      | 1250552    | M1C   |

---

## Table of Contents

[Phase 1 Report](Deliverables/Phase1/Phase1_Report.md)
