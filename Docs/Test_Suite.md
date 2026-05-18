# Test Suite

Documento com os testes atualmente existentes no projeto.

## Overview

- Suite total validada: 244 testes
- Cobertura por camadas: unitários, controller, integration e end-to-end

Nota: alguns runners mostram mais entradas por causa de classes aninhadas e relatórios separados, mas o total efetivo executado no Maven foi 244.

## Authentication

- [src/test/java/com/desofs/auth/AuthServiceTest.java](../src/test/java/com/desofs/auth/AuthServiceTest.java)
- [src/test/java/com/desofs/auth/AuthServiceRefreshTest.java](../src/test/java/com/desofs/auth/AuthServiceRefreshTest.java)
- [src/test/java/com/desofs/auth/RefreshTokenServiceTest.java](../src/test/java/com/desofs/auth/RefreshTokenServiceTest.java)
- [src/test/java/com/desofs/auth/AuthControllerIT.java](../src/test/java/com/desofs/auth/AuthControllerIT.java)

## Projects

- [src/test/java/com/desofs/project/ProjectServiceTest.java](../src/test/java/com/desofs/project/ProjectServiceTest.java)
- [src/test/java/com/desofs/project/ProjectMemberServiceTest.java](../src/test/java/com/desofs/project/ProjectMemberServiceTest.java)
- [src/test/java/com/desofs/project/ProjectControllerIT.java](../src/test/java/com/desofs/project/ProjectControllerIT.java)

## Tasks

- [src/test/java/com/desofs/task/TaskStatusTest.java](../src/test/java/com/desofs/task/TaskStatusTest.java)
- [src/test/java/com/desofs/task/TaskServiceTest.java](../src/test/java/com/desofs/task/TaskServiceTest.java)
- [src/test/java/com/desofs/task/TaskControllerTest.java](../src/test/java/com/desofs/task/TaskControllerTest.java)
- [src/test/java/com/desofs/task/TaskControllerIT.java](../src/test/java/com/desofs/task/TaskControllerIT.java)

## Attachments

- [src/test/java/com/desofs/attachment/AttachmentServiceTest.java](../src/test/java/com/desofs/attachment/AttachmentServiceTest.java)
- [src/test/java/com/desofs/attachment/AttachmentControllerTest.java](../src/test/java/com/desofs/attachment/AttachmentControllerTest.java)

## Comments

- [src/test/java/com/desofs/comment/CommentServiceTest.java](../src/test/java/com/desofs/comment/CommentServiceTest.java)
- [src/test/java/com/desofs/comment/CommentControllerTest.java](../src/test/java/com/desofs/comment/CommentControllerTest.java)

## Users

- [src/test/java/com/desofs/user/UserServiceTest.java](../src/test/java/com/desofs/user/UserServiceTest.java)

## End-to-end

- [src/test/java/com/desofs/e2e/ApplicationE2ETest.java](../src/test/java/com/desofs/e2e/ApplicationE2ETest.java)

## Notes

- Os testes de controller validam os endpoints e códigos HTTP.
- Os testes de service validam regras de negócio e permissões.
- Os testes de integração validam o comportamento Spring com contexto real.
- O teste E2E valida login, criação de projeto e criação de task num fluxo real.
