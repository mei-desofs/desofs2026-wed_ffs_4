## Description
<!-- Briefly describe the changes in this PR -->

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Security improvement
- [ ] Documentation
- [ ] CI/Pipeline change

## Security Checklist
**All security items below must be reviewed before merging:**

- [ ] **Authentication/Authorization**
  - [ ] No hardcoded credentials or secrets
  - [ ] JWT/session handling is correct
  - [ ] Authorization checks on all endpoints

- [ ] **Input Validation**
  - [ ] All user inputs validated
  - [ ] No SQL injection vulnerabilities (parameterized queries used)
  - [ ] No path traversal vulnerabilities
  - [ ] No XSS vulnerabilities

- [ ] **Data Protection**
  - [ ] Sensitive data is not logged
  - [ ] No plaintext passwords in code
  - [ ] Proper encryption for sensitive fields

- [ ] **File Handling** (if applicable)
  - [ ] File size limits enforced (25MB max)
  - [ ] File type whitelist validated
  - [ ] MIME type verification
  - [ ] Files stored outside web root

- [ ] **Error Handling**
  - [ ] No sensitive info in error messages
  - [ ] No stack traces exposed to users
  - [ ] Proper HTTP status codes returned

- [ ] **Testing**
  - [ ] Unit tests added/updated
  - [ ] Security tests added for security-relevant changes
  - [ ] All tests pass locally and in CI

## ASVS Mapping
<!-- If this relates to an ASVS requirement, list it here -->
<!-- Example: Implements ASVS V2.1.1 (password hashing) -->

## Related Issue(s)
<!-- Fixes -->

## Test Evidence
<!-- Describe or attach test results for security-related changes -->