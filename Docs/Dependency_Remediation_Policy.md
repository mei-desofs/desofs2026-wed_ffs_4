# Dependency Remediation Policy

## Purpose

This policy defines the required remediation time frames for vulnerable third-party components and routine library updates used by the application. It applies to direct dependencies, transitive dependencies, Maven plugins, container images, database drivers, test libraries, and runtime platform components.

## Vulnerability Remediation Time Frames

| Risk level | Examples | Required action |
| --- | --- | --- |
| Critical | Remote code execution, authentication bypass, active exploitation, exposed secrets | Start assessment within 24 hours and remediate or mitigate within 7 calendar days. |
| High | Privilege escalation, serious data exposure, exploitable denial of service | Remediate or mitigate within 14 calendar days. |
| Medium | Limited data exposure, moderate denial of service, hard-to-exploit flaws | Remediate within 30 calendar days. |
| Low | Low-impact issues, defense-in-depth fixes, unlikely exploit paths | Remediate within 90 calendar days or the next scheduled maintenance release, whichever comes first. |

If a vulnerability is known to be actively exploited, it is treated as Critical regardless of its published CVSS score.

## Acceptable Remediation

Remediation may include upgrading the affected component, removing or replacing the dependency, changing configuration to disable the vulnerable behavior, applying a vendor-supported patch, or documenting a temporary compensating control until an upgrade is available.

Any exception to the time frames must document the affected component, risk rationale, compensating controls, owner, review date, and target remediation date.

## Routine Dependency Updates

Dependencies are reviewed at least monthly, even when no known vulnerability is reported. Patch and minor updates should be applied when compatibility risk is low. Major upgrades are planned separately and must include regression testing for authentication, authorization, file upload, and persistence behavior.

## Ownership and Tracking

The development team is responsible for reviewing dependency vulnerability reports, issue tracker alerts, build output, and dependency scan results. Findings are tracked as security tasks until closed. Each task records the component, affected version, severity, decision, remediation action, and completion date.

## Verification

Before marking a dependency remediation task complete, the team verifies that the vulnerable version is no longer present or that the documented mitigation is active. Verification may include Maven dependency output, SBOM review, vulnerability scanner results, or targeted regression tests.
