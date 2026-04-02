# Teuxdeux — Phase 1 Report

> **Project:** Teuxdeux — Task Management App

> **Class ID:** wed_ffs

> **Team ID:** wed_ffs_4

> **Git Repository:** [desofs2026_wed_ffs_4](https://github.com/DESOFS2026/desofs2026_wed_ffs_4)

---

## Team Members

| Name             | Student ID | Class |
| ---------------- | ---------- | ----- |
| Milan Marcinco   | 1252431    | M1C   |
| Agata Szysiak    | 1252365    | M1C   |
| Filipe Magalhães | 1211606    | M1C   |
| Tiago Pinto      | 1250552    | M1C   |

---

## Table of Contents

1. [System Overview](#1-system-overview)
   - [1.1 System Purpose](#11-system-purpose)
   - [1.2 Functional Requirements](#12-functional-requirements)
   - [1.3 Non-Functional Requirements](#13-non-functional-requirements)
   - [1.4 Domain Model](#14-domain-model)

---

## 1. System Overview

### 1.1 System Purpose

Teuxdeux is a collaborative task management platform built for teams, enabling them to organize work into projects and tasks while allowing for easy communication through comments and file attachments. It supports a hierarchy of user roles — Admins, Managers, and Members — each with appropriate levels of access and control. Security is treated as a foundational priority throughout the platform, ensuring that every user can only see and do what their role permits.

---

### 1.2 Functional Requirements

| User Story                                                                                                               | Role(s)                            |
| ------------------------------------------------------------------------------------------------------------------------ | ---------------------------------- |
| As a visitor, I want to register with an email and password so that I can access the platform.                           | —                                  |
| As a registered user, I want to log in with my email and password.                                                       | All                                |
| As an authenticated user, I want to log out and have my session invalidated.                                             | All                                |
| As an Admin, I want to create a new project with a title and description.                                                | Admin, Manager                     |
| As an Admin, I want to delete a project.                                                                                 | Admin, Manager (own projects only) |
| As an Admin or Manager, I want to edit a project's title and description.                                                | Admin, Manager (own projects only) |
| As an Admin, I want to add users to a project and assign them the Member role.                                           | Admin, Manager (own projects only) |
| As any project member, I want to view the projects I belong to or have created.                                          | All                                |
| As an Admin or Manager, I want to create a task within a project, setting its title, description, and assignee.          | Admin, Manager (own projects only) |
| As an Admin or Manager, I want to edit all task fields (title, description, assignee).                                   | Admin, Manager (own projects only) |
| As an Admin or Manager, I want to delete a task.                                                                         | Admin, Manager (own projects only) |
| As any project member, I want to update the status of a task.                                                            | All                                |
| As any project member, I want to upload a file attachment to a task.                                                     | All                                |
| As any project member, I want to download a file attachment from a task.                                                 | All                                |
| As any project member, I want to add a comment to a task.                                                                | All                                |
| As a system, I want key actions (login, logout, role changes, task updates, file events) to be recorded in an audit log. | System                             |

---

### 1.3 Non-Functional Requirements

| Category        | Requirement                                                                                                                         |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| Scalability     | The application layer must be stateless so that horizontal scaling by adding additional instances requires no architectural change. |
| Data Integrity  | All database writes must be performed within ACID transactions, partial states must be rolled back on failure.                      |
| Maintainability | All Maven dependencies must be pinned to explicit release versions.                                                                 |
| Observability   | All security-relevant events must emit log entries to the audit log.                                                                |

---

### 1.4 Domain Model

![](/Docs/Diagrams/Domain_Model.svg)

**User** is the root aggregate for identity. Every platform interaction requires an authenticated User.
**Project** is the primary organizational aggregate created by an Admin. Project membership is represented by the `ProjectMember` join entity.
**Task** is the unit of work within a Project. It carries its own lifecycle via `TaskStatus`. Optionally, the `assigneeId` links to an assigned User. Tasks are the primary attachment point for both `Attachment` records and `Comment` records.
**Attachment** represents a file uploaded to a Task. The user-supplied `userFileName` is stored as metadata only and never used in filesystem operations directly.

---
