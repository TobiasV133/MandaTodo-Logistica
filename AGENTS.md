# AGENTS.md

## Repository state

- Early scaffold: no source code, manifests, build/test/lint config, or CI yet. Do not assume commands exist.
- Stack is fixed by the spec: React + Bootstrap + React-Leaflet + Lucide, Spring Boot + Spring Data JPA, MySQL 8.x, OpenStreetMap/Nominatim.
- OpenSpec CLI 1.11.0 is installed globally. PowerShell blocks `openspec.ps1`, so use `cmd /c "openspec ..."` from the repo root.
- The project uses the requested `.openspec/` layout, not the CLI-native `openspec/` one. The CLI reports `No OpenSpec root found` for this repo and will not validate `.openspec/specs/manda-todo/manda-todo.spec.md`; do not treat that as a broken spec. Migrate to `openspec/specs/<capability>/spec.md` if CLI validation is required.

## OpenCode config

- `.opencode/opencode.json` sets `default_agent` to `senior-fullstack-lead`.
- Agents live in `.opencode/agents/`: the primary `senior-fullstack-lead`, plus the subagents `database-integrity-specialist` and `qa-testing-engineer`.
- Skills live in `.opencode/skills/db-seeder/` and `.opencode/skills/spec-checker/`.
- Config-time files are loaded at startup; restart opencode after changing agents, skills, or `.opencode/opencode.json`.

## Source of truth

- `.openspec/specs/manda-todo/manda-todo.spec.md` is the single, immutable source of truth for stack, data model, business rules, and the 19-endpoint API. Do not edit it without explicit user instruction; update it first if requirements change.
- `.openspec/config.yaml` declares the project metadata and the `.openspec/specs` / `.openspec/changes` directories.
- `schema.sql` is the canonical MySQL 8 DDL for `mandatodo_db` (5 tables: `clientes`, `operarios`, `pedidos`, `revisiones`, `auditoria`). It drops and recreates those tables, so running it is destructive.
- `seeds.sql` loads deterministic test data after `schema.sql` (3 operarios, 6 clientes, 9 pedidos, 2 revisiones, 17 eventos de auditoría). It uses fixed IDs and test credentials, and is not idempotent: run it only against freshly created tables.
- Schema includes implementation-required columns not present in the TS model: `operarios.password_hash` (login), `pedidos.fecha_programada` (failed-delivery retry), and `pedidos.motivo_cancelacion` (in-transit cancellation). Triggers make `auditoria` append-only.
- Apply with a MySQL 8 client: `mysql -u <user> -p < schema.sql`. No `mysql` CLI is installed in this environment (verified); the Docker daemon is also not running, so the DDL has not been executed against MySQL 8.

## Project context

- MandaTodo-Logistica: logistics management software for a small/medium transport company (university project, Grupo 16 - Caso 7).
- `README.md` and project documentation are in Spanish; keep new docs consistent with that language.

## Git

- Default/primary branch is `main`; remote is `origin`.
# AGENTS.md - Sistema MandáTodo

## Visión del Sistema
MandáTodo es una plataforma logística orientada a la gestión de envíos y paquetes en tiempo real. 

## Reglas de Negocio Estrictas
1. **Modelos de Envíos:** Ningún paquete o guía de transporte puede ser registrado sin una dirección validada/normalizada.
2. **Restricciones de Identidad:** En base de datos (MySQL), los campos DNI, CUIT, Email y Número de Seguimiento (Tracking) DEBEN contar con restricciones `UNIQUE` explícitas para prevenir duplicados.
3. **Costo de Infraestructura:** El costo operativo de consumo de APIs de geolocalización/mapas debe ser $0 USD. Se utiliza únicamente OpenStreetMap / Nominatim. Está prohibido el uso de claves o SDKs pagos (Google Maps API, Mapbox).
4. **Respeto de Rate Limit:** La API de Nominatim exige una cabecera `User-Agent` identificable y un límite de requests máximo de 1 petición por segundo desde el backend.

## Arquitectura de Software y Estándares

### Backend (Spring Boot / Java 21)
- **Tipado Estricto:** Evitar el uso de `Object` o genéricos ambiguos. Todas las entidades, DTOs y Servicios deben estar fuertemente tipados.
- **Arquitectura en Capas:** Controladores (REST) -> Servicios (Reglas) -> Repositorios (JPA) -> Base de Datos.
- **Inmutabilidad & Lombok:** Uso preferencial de DTOs con `@Value` o Java Records para respuestas API.

### Frontend (React + Vite)
- **Componentes Funcionales:** Uso exclusivo de hooks (`useState`, `useEffect`, `useCallback`, `useMemo`).
- **Respeto al Virtual DOM:** Queda prohibido manipular el DOM directamente (`document.getElementById`). Toda actualización del mapa se realiza mediante props y estado de React.
- **Rendimiento de Leaflet:** Utilizar los hooks provistos por `react-leaflet` (`useMap`, `useMapEvents`) para sincronizar marcadores.