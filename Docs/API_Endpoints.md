# API Endpoints

Documento com todos os endpoints atualmente disponíveis no projeto.

## Authentication

| Method | Path | Description | Access |
|---|---|---|---|
| POST | /auth/register | Regista um utilizador. Aceita `username` ou `email` + `password`. | Public |
| POST | /auth/login | Autentica um utilizador. Aceita `username` ou `email` + `password`. | Public |
| POST | /auth/refresh | Gera novos tokens a partir de um `refreshToken`. | Public |
| POST | /auth/logout | Invalida o token atual. | Authenticated |

## Admin users

| Method | Path | Description | Access |
|---|---|---|---|
| PUT | /api/admin/users/{id}/role | Atualiza o role de um utilizador (`ADMIN`, `MANAGER`, `USER`). | ADMIN |

## Projects

| Method | Path | Description | Access |
|---|---|---|---|
| POST | /api/projects | Cria um projeto. | ADMIN |
| GET | /api/projects | Lista os projetos visíveis para o utilizador autenticado. | Authenticated |
| GET | /api/projects/{id} | Obtém os detalhes de um projeto. | Authenticated |
| PUT | /api/projects/{id} | Atualiza nome/descrição de um projeto. | ADMIN, MANAGER |
| DELETE | /api/projects/{id} | Elimina logicamente um projeto. | ADMIN |

## Project members

| Method | Path | Description | Access |
|---|---|---|---|
| POST | /api/projects/{projectId}/members | Adiciona um membro ao projeto por email. | ADMIN, MANAGER |
| GET | /api/projects/{projectId}/members | Lista os membros do projeto. | ADMIN, MANAGER, member |
| DELETE | /api/projects/{projectId}/members | Remove um membro por email. | ADMIN, MANAGER |
| DELETE | /api/projects/{projectId}/members/{memberId} | Remove um membro por id. | ADMIN, MANAGER |

## Tasks

| Method | Path | Description | Access |
|---|---|---|---|
| POST | /api/projects/{projectId}/tasks | Cria uma task dentro de um projeto. | Authenticated member |
| GET | /api/projects/{projectId}/tasks | Lista tasks do projeto. | Authenticated member |
| PUT | /api/projects/{projectId}/tasks/{taskId} | Atualiza título e descrição de uma task. | Authenticated member |
| PATCH | /api/projects/{projectId}/tasks/{taskId}/status | Atualiza o estado da task. | Authenticated member |
| DELETE | /api/projects/{projectId}/tasks/{taskId} | Elimina logicamente uma task. | Authenticated member |
| PATCH | /api/projects/{projectId}/tasks/{taskId}/assignee | Atribui ou remove o responsável da task. | Authenticated member |

## Comments

| Method | Path | Description | Access |
|---|---|---|---|
| POST | /api/tasks/{taskId}/comments | Adiciona um comentário a uma task. | Authenticated member |
| GET | /api/tasks/{taskId}/comments | Lista comentários de uma task. | Authenticated member |
| PUT | /api/tasks/{taskId}/comments/{commentId} | Atualiza um comentário. | Authenticated member |
| DELETE | /api/tasks/{taskId}/comments/{commentId} | Elimina um comentário. | Authenticated member |

## Attachments

| Method | Path | Description | Access |
|---|---|---|---|
| GET | /api/tasks/{taskId}/attachments | Lista anexos de uma task. | Authenticated member |
| POST | /api/tasks/{taskId}/attachments | Faz upload de um anexo (`multipart/form-data`, campo `file`). | Authenticated member |
| GET | /api/attachments/{id}/download | Faz download de um anexo. | Authenticated member |
| DELETE | /api/tasks/{taskId}/attachments/{id} | Elimina um anexo. | Authenticated member |

## Notes

- Os endpoints assinalados como "member" requerem que o user pertença ao projeto.
- O backend aceita autenticação por JWT.
- O endpoint `/api/projects/{projectId}/members` suporta a versão por email e por id para remoção.
- Este documento reflete os endpoints atualmente presentes no código-fonte.
