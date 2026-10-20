# AGENTS.md

## Repository state

- Early scaffold: no source code, manifests, build/test/lint config, CI, or `opencode.json` yet. Do not assume commands exist.
- Stack is fixed by the spec: React + Bootstrap + React-Leaflet + Lucide, Spring Boot + Spring Data JPA, MySQL 8.x, OpenStreetMap/Nominatim.
- OpenSpec CLI 1.11.0 is installed globally. PowerShell blocks `openspec.ps1`, so use `cmd /c "openspec ..."` from the repo root.
- The project uses the requested `.openspec/` layout, not the CLI-native `openspec/` one. The CLI reports `No OpenSpec root found` for this repo and will not validate `.openspec/specs/manda-todo/manda-todo.spec.md`; do not treat that as a broken spec. Migrate to `openspec/specs/<capability>/spec.md` if CLI validation is required.

## Source of truth

- `.openspec/specs/manda-todo/manda-todo.spec.md` is the single, immutable source of truth for stack, data model, business rules, and the 19-endpoint API. Do not edit it without explicit user instruction; update it first if requirements change.
- `.openspec/config.yaml` declares the project metadata and the `.openspec/specs` / `.openspec/changes` directories.
- `schema.sql` is the canonical MySQL 8 DDL for `mandatodo_db` (5 tables: `clientes`, `operarios`, `pedidos`, `revisiones`, `auditoria`). It drops and recreates those tables, so running it is destructive.
- Schema includes implementation-required columns not present in the TS model: `operarios.password_hash` (login), `pedidos.fecha_programada` (failed-delivery retry), and `pedidos.motivo_cancelacion` (in-transit cancellation). Triggers make `auditoria` append-only.
- Apply with a MySQL 8 client: `mysql -u <user> -p < schema.sql`. No `mysql` CLI is installed in this environment (verified); the Docker daemon is also not running, so the DDL has not been executed against MySQL 8.

## Project context

- MandaTodo-Logistica: logistics management software for a small/medium transport company (university project, Grupo 16 - Caso 7).
- `README.md` and project documentation are in Spanish; keep new docs consistent with that language.

## Git

- Default/primary branch is `main`; remote is `origin`.
