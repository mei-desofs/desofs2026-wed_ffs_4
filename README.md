# Teuxdeux

Teuxdeux is a collaborative task management platform built for teams, enabling them to organize work into projects and tasks while allowing for easy communication through comments and file attachments. It supports a hierarchy of user roles — Admins, Managers, and Members — each with appropriate levels of access and control. Security is treated as a foundational priority throughout the platform, ensuring that every user can only see and do what their role permits.

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
[Phase 2 Report](Deliverables/Phase2/Phase2_Report.md)

