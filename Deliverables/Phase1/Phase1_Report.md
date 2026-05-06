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
   - [1.2 Phase 1: Analysis](#12-phase-1-analysis)
2. [Phase 1 Document Set](#2-phase-1-document-set)
3. [Conformance Check (Project.pdf)](#3-conformance-check-projectpdf)

---

## 1. System Overview

### 1.1 System Purpose

Teuxdeux is a collaborative task management platform built for teams, enabling them to organize work into projects and tasks while allowing for easy communication through comments and file attachments. It supports a hierarchy of user roles — Admins, Managers, and Members — each with appropriate levels of access and control. Security is treated as a foundational priority throughout the platform, ensuring that every user can only see and do what their role permits.

---

### 1.2 Phase 1: Analysis

Our Phase 1 approach followed a simple sequence: define what must be built, understand what can go wrong, design a secure structure, decide concrete protections, and finally define how those protections will be tested.

1. **Start with clear scope and security expectations** — [01_Requirements.md](01_Requirements.md)
   - **Why:** We needed a shared baseline for functionality, constraints, and explicit security requirements before making design decisions.
   - **Expected outcome:** A stable set of FR/NFR/SR requirements to drive all next documents and avoid ambiguity.

2. **Assess realistic threats early** — [02_ThreatModeling.md](02_ThreatModeling.md)
   - **Why:** Security controls should be justified by risk, not chosen arbitrarily.
   - **Expected outcome:** Prioritized STRIDE threats, risk scoring, and mitigation mapping to focus effort on highest-impact issues.

3. **Translate requirements and risks into technical structure** — [03_Architecture.md](03_Architecture.md)
   - **Why:** The architecture had to support role separation, secure data flows, and controlled file handling from the start.
   - **Expected outcome:** A coherent layered architecture, domain/data model context, storage boundaries, and secure integration points.

4. **Define how security is implemented in practice** — [04_SecurityDesign.md](04_SecurityDesign.md)
   - **Why:** High-level architecture is not enough; we needed concrete rules for authentication, authorization, validation, file handling, and auditing.
   - **Expected outcome:** Implementation-ready security decisions aligned with identified threats and project constraints.

5. **Plan verification of all critical controls** — [06_SecurityTesting.md](06_SecurityTesting.md)
   - **Why:** Security requirements are only meaningful if we can verify them through repeatable tests and abuse scenarios.
   - **Expected outcome:** A practical security testing plan with traceability from risks/requirements to test cases.

This sequence gave the team a consistent narrative from intent to verification, reducing rework and improving alignment with the Phase 1 deliverable goals.


---

## 2. Phase 1 Document Set

### Core Deliverables

| Topic | Document |
| --- | --- |
| Requirements (FR/NFR/SR) | [01_Requirements.md](01_Requirements.md) |
| Secure Architecture | [03_Architecture.md](03_Architecture.md) |
| Threat Modeling (STRIDE + Risk) | [02_ThreatModeling.md](02_ThreatModeling.md) |
| Security Design (AuthN/AuthZ/Data/Input/Logging) | [04_SecurityDesign.md](04_SecurityDesign.md) |
| Security Test Plan + Abuse Cases | [06_SecurityTesting.md](06_SecurityTesting.md) |

### Supporting Deliverables

| Topic | Document |
| --- | --- |
| Assets inventory | [Assets.md](Assets.md) |
| Entry points | [EntryPoints.md](EntryPoints.md) |
| Exit points | [ExitPoints.md](ExitPoints.md) |
| Trust levels | [TrustLevels.md](TrustLevels.md) |

## 3. Conformance Check (Project.pdf)

Based on [Project.pdf](../../Project.pdf), Phase 1 expectations are addressed as follows:

- Analysis/Requirements: covered in [01_Requirements.md](01_Requirements.md)
- Threat modeling + risk + mitigations: covered in [02_ThreatModeling.md](02_ThreatModeling.md)
- Secure architecture/design: covered in [03_Architecture.md](03_Architecture.md) and [04_SecurityDesign.md](04_SecurityDesign.md)
- Security test planning + abuse cases + traceability: covered in [06_SecurityTesting.md](06_SecurityTesting.md)

- Main-document linking (rubric: organization): consolidated in this report and referenced document set

Notes for rubric alignment:
- DFD references included in threat modeling document.
- Domain model and architecture diagrams linked in requirements/architecture sections.
- Security flow and RBAC diagrams linked in security design.

### Security Standards / Checklist

- ASVS checklist: [ASVS_5.0_Tracker.xlsx](../../Materials/ASVS_5.0_Tracker.xlsx)
- ASVS reference standard: [OWASP_Application_Security_Verification_Standard_5.0.0_en.pdf](../../Materials/OWASP_Application_Security_Verification_Standard_5.0.0_en.pdf)
