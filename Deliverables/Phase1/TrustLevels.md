# Trust Levels

Canonical trust levels used across Phase 1 tables.

| Code | Name | Typical Actors | Meaning |
|---|---|---|---|
| TL0 | Untrusted External | Anonymous users, invalid login attempts, internet traffic | No implicit trust. Input must always be validated and authenticated. |
| TL1 | Authenticated User | Member users with valid JWT | Known identity, limited privileges, strict authorization required. |
| TL2 | Privileged Business User | Manager, Admin | Higher business permissions, but not trusted for infrastructure control. |
| TL3 | Application/Internal Service | API server process, DB service account, filesystem service operations | Internal runtime zone protected by least privilege and monitoring. |
| TL4 | Infrastructure Administrator | DB/OS administrators, deployment operators | Highest operational trust; requires strong restriction and full auditing. |

Usage rule: table rows may include multiple trust levels when multiple trust zones interact with the same asset/entry/exit point.
