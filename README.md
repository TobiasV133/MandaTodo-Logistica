# 📦 MandáTodo Logística — Sistema de Normalización y Geolocalización de Envíos

> "Validación y control de direcciones en la última milla urbana"

## 1. Encabezado y datos académicos

### Resumen ejecutivo

MandáTodo Logística es una plataforma de gestión de envíos orientada a pequeñas y medianas empresas de transporte urbano. Su objetivo principal es **prevenir los viajes en vano** mediante la normalización y geocodificación de direcciones antes de que un pedido salga a la calle, la consolidación de una libreta digital de clientes y una trazabilidad operativa inalterable.

El sistema combina la validación de direcciones con OpenStreetMap/Nominatim, una bandeja de seguridad para pedidos con información ambigua o posible duplicado comercial, y un circuito de estados que contempla reintentos de entrega con recargo. Cada alta, edición, reimpresión o cambio de estado queda registrado en una auditoría global que no admite modificaciones ni eliminaciones.

La solución se desarrolla bajo un enfoque **Spec-Driven Development (SDD)**: la especificación viva es la fuente de verdad y toda implementación debe verificarse contra ella.

### Datos académicos

| Campo | Valor |
| --- | --- |
| Institución | Universidad Nacional de Lanús (UNLa) |
| Carrera | Licenciatura en Sistemas |
| Asignatura | Prácticas Pre Profesionales 1 (PPP 1) |
| Caso | 7 |
| Equipo | Grupo 16 |

## 2. Metodología: Spec-Driven Development (SDD) & entorno OpenCode

### La especificación viva como fuente de verdad

En SDD el contrato de comportamiento se define antes de escribir código. La especificación no es documentación pasiva: es el artefacto que gobierna el alcance, los nombres del modelo de datos, las reglas de negocio y el contrato de la API. Cualquier cambio de alcance debe reflejarse primero en la especificación y luego en la implementación.

La especificación oficial del proyecto se encuentra en:

- [`.openspec/specs/manda-todo/manda-todo.spec.md`](.openspec/specs/manda-todo/manda-todo.spec.md)
- Configuración del proyecto: `.openspec/config.yaml`
- Espacio para propuestas y cambios: `.openspec/changes/`

La spec es **única e inalterable** dentro del ciclo de trabajo: no se modifica sin una instrucción explícita. El agente `spec-checker` y las revisiones por Pull Request verifican que entidades JPA, DTOs, interfaces de React y endpoints respeten exactamente el DER y el contrato REST.

### Infraestructura de IA en `.opencode/`

La configuración de OpenCode vive en `.opencode/opencode.json`, que establece como agente por defecto al **Senior Full-Stack Lead**. Los archivos de configuración se cargan al iniciar OpenCode: después de modificarlos hay que reiniciar la herramienta.

**Agentes modulares** (`.opencode/agents/`):

| Agente | Rol | Modo |
| --- | --- | --- |
| `senior-fullstack-lead` | Hace cumplir la Spec viva y guía las implementaciones React, Spring Boot y MySQL. | `primary` |
| `database-integrity-specialist` | Mantiene `schema.sql` y `seeds.sql`, la integridad referencial y la auditoría inalterable. | `subagent` |
| `qa-testing-engineer` | Garantiza la cobertura de pruebas de los 19 endpoints del contrato REST. | `subagent` |

**Skills automatizadas** (`.opencode/skills/`):

| Skill | Propósito |
| --- | --- |
| `db-seeder` | Regenerar y poblar la base MySQL 8.x local usando `schema.sql` y `seeds.sql`. |
| `spec-checker` | Auditar que DTOs Java, entidades JPA e interfaces React coincidan con el DER y el contrato API. |

## 3. Stack tecnológico oficial

| Capa | Tecnología | Uso en el proyecto |
| --- | --- | --- |
| Frontend | React + Vite, Bootstrap 5, Lucide Icons | Interfaz operativa, tableros, formularios y componentes de gestión. |
| Mapas & Geolocalización | Leaflet / React-Leaflet, Nominatim API (OpenStreetMap) | Visualización de pedidos y normalización/geocodificación de direcciones. |
| Backend | Java 21 + Spring Boot 4.0.8, Spring Data JPA / Hibernate | API REST, reglas de negocio, persistencia y auditoría. |
| Validación | Jakarta Bean Validation (`jakarta.validation`) | Validación declarativa de DTOs de entrada (`@NotBlank`, `@Email`, `@Size`, etc.). |
| Seguridad | Spring Security Crypto (BCrypt) + JWT | Hash de contraseñas con `BCryptPasswordEncoder`, autenticación y sesiones de operarios. |
| Base de Datos | MySQL 8.x (`mandatodo_db`) | Almacenamiento transaccional de clientes, pedidos, revisiones, operarios y auditoría. |
| Documentación / PDF | OpenPDF / iText | Generación y reimpresión de remitos de entrega en PDF. |

## 4. Arquitectura de datos y DER

### Entidades principales

- **`clientes`**: libreta digital de clientes. Almacena `tipo_documento`, `numero_documento` (UNIQUE), `nombre_razon_social`, `telefono`, `email`, `tipo_cliente` (`Particular` / `Empresa`) y `es_frecuente`.
- **`pedidos`**: núcleo operativo. Referencia a `clientes` y registra descripción, peso, monto de envío, estado, motivo de falla, dirección normalizada (`calle`, `altura`, `piso_depto`, `localidad`), coordenadas, fechas y motivo de cancelación en tránsito.
- **`revisiones`**: bandeja de seguridad. Referencia a `pedidos` y retiene aquellos casos con `Dirección ambigua` o `Posible duplicado comercial`, con resolución `PENDIENTE`, `APROBADO` o `RECHAZADO`.
- **`operarios`**: usuarios del sistema. Incluye `username` (UNIQUE, login), `email` (UNIQUE), `password_hash`, `rol` (`Admin` / `Operario`) y `estado` (`Activo` / `Inactivo`).
- **`auditoria`**: registro global e inalterable. Referencia al `operario` responsable y, opcionalmente, al `pedido`; almacena `actividad`, `tipo_evento` (`Alta`, `Edición`, `Reimpresión`, `Cambio de Estado`) y `fecha_hora`.

### Diagrama de relaciones

```text
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│  clientes   │ 1     N │   pedidos    │ 1     N │  revisiones │
│─────────────│─────────│──────────────│─────────│─────────────│
│ id_cliente  │         │ id_pedido    │         │ id_revision │
│ numero_doc  │         │ id_cliente   │         │ id_pedido   │
│ tipo_cliente│         │ estado       │         │ motivo      │
└─────────────┘         └──────┬───────┘         └─────────────┘
                               │ 1
                               │
                               │ N
                        ┌──────┴───────┐         ┌─────────────┐
                        │  auditoria   │ N     1 │  operarios  │
                        │──────────────│─────────│─────────────│
                        │ id_auditoria │         │ id_operario │
                        │ id_operario  │         │ email       │
                        │ id_pedido    │         │ rol         │
                        └──────────────┘         └─────────────┘
```

### Reglas de integridad y negocio

- `clientes.numero_documento` y `operarios.email` son **UNIQUE**.
- Las claves foráneas usan `ON DELETE RESTRICT`: no se puede eliminar un cliente con pedidos, un pedido con revisiones/auditoría ni un operario con eventos asociados.
- `pedidos.estado = 'FALLIDO'` exige `motivo_falla` (`Ausente`, `Dirección Inexistente` o `Dirección Incompleta`). El pedido se reprograma para el día siguiente y se aplica un **50% de recargo** sobre `monto_envio`.
- `pedidos.estado = 'CANCELADO_EN_TRANSITO'` exige `motivo_cancelacion`.
- Una dirección con altura `0`, calle `S/N` o geocodificación no confiable genera una revisión por `Dirección ambigua` y deja el pedido en `EN_REVISION`.
- Si la dirección queda ambigua, el operario puede arrastrar el pin o hacer clic en el mapa para corregir la ubicación. El pedido se registra con las coordenadas corregidas, sigue en `EN_REVISION` y la auditoría del alta deja constancia del ajuste manual.
- Un pedido idéntico de un cliente `Empresa` registrado el mismo día genera una revisión por `Posible duplicado comercial`; para clientes `Particular` el pedido pasa directamente a `PENDIENTE`.
- La tabla `auditoria` es **append-only**: los triggers `trg_auditoria_no_update` y `trg_auditoria_no_delete` bloquean `UPDATE` y `DELETE` a nivel de base de datos.

## 5. Principales endpoints de la API REST

| Método | Endpoint | Descripción |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Inicia sesión con email y contraseña; devuelve un JWT. |
| `GET` | `/api/auth/me` | Devuelve el operario autenticado a partir del JWT. |
| `POST` | `/api/geo/normalizar` | Normaliza y geocodifica una dirección mediante Nominatim. |
| `POST` | `/api/pedidos` | Crea un pedido en `PENDIENTE` o `EN_REVISION` según las reglas. |
| `GET` | `/api/clientes/buscar?doc=...` | Autocompleta un cliente por DNI/CUIT. |
| `GET` | `/api/clientes` | Lista todos los clientes. |
| `PUT` | `/api/clientes/{id}` | Edita los datos de un cliente. |
| `GET` | `/api/pedidos` | Lista pedidos; acepta el filtro `?estado=...`. |
| `GET` | `/api/pedidos/resumen` | Totales de pendientes, en revisión, entregados y clientes para el dashboard. |
| `GET` | `/api/pedidos/{id}` | Obtiene el detalle de un pedido. |
| `PATCH` | `/api/pedidos/{id}/estado` | Cambia el estado; `FALLIDO` reprograma y aplica el 50% de recargo. |
| `POST` | `/api/pedidos/{id}/cancelar-transito` | Registra una cancelación en tránsito con motivo. |
| `GET` | `/api/pedidos/en-revision` | Lista los pedidos retenidos en la bandeja de revisión. |
| `PUT` | `/api/pedidos/{id}/revision` | Resuelve una revisión (`APROBAR` o `RECHAZAR`). |
| `POST` | `/api/remitos/{idPedido}/imprimir` | Genera o reimprime el remito PDF con motivo. |
| `GET` | `/api/usuarios` | Lista todos los operarios. |
| `POST` | `/api/usuarios` | Da de alta un operario (`Admin` u `Operario`). |
| `PUT` | `/api/usuarios/{id}` | Edita datos o estado (`Activo` / `Inactivo`). |
| `POST` | `/api/auditoria` | Registra un evento de auditoría. |
| `GET` | `/api/auditoria` | Consulta el listado general de logs. |
| `GET` | `/api/auditoria?fecha=...&operario=...` | Consulta auditoría con filtros por fecha y operario. |

> **Estado del corte integrado:** están implementados login/me, geocodificación, listado y búsqueda
> de clientes, alta/listado/detalle/resumen de pedidos, cambio de estado, revisiones y remitos. El ABM
> de usuarios, la edición de clientes, la cancelación dedicada y la consulta de auditoría forman
> parte del contrato objetivo y todavía no están expuestos. La cancelación se resuelve con
> `PATCH /api/pedidos/{id}/estado` enviando `estado: CANCELADO_EN_TRANSITO` y `motivoCancelacion`.
> Todos los endpoints salvo el login requieren `Authorization: Bearer <token>`.

## 5.1 Puesta en marcha local

1. Crear y poblar MySQL 8.x:

   ```bash
   mysql --default-character-set=utf8mb4 -u root -p < schema.sql
   mysql --default-character-set=utf8mb4 -u root -p < seeds.sql
   ```

   En PowerShell usá `cmd /c "mysql ... < schema.sql"` para conservar UTF-8.

2. Iniciar el backend:

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

   Por defecto se conecta a `localhost:3306` con el usuario `root` y **sin contraseña**. Si tu
   MySQL tiene clave, definí `$env:DB_PASSWORD` antes de arrancar.

3. Iniciar el frontend:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

4. Ingresar en `http://localhost:5173` con `laura.gomez@mandatodo.com` / `Admin123!` (usuario de
   `seeds.sql`). El detalle de variables y endpoints está en `backend/README.md` y
   `frontend/README.md`.

## 6. Estrategia de branching (Git Flow)

El repositorio trabaja con `main` como rama estable y protegida. Las funcionalidades se desarrollan en ramas cortas y se integran exclusivamente mediante Pull Requests (PR).

```text
main
├── feature/frontend-mapa
├── feature/backend-nominatim-pdf
├── feature/logica-revision-recargos
└── feature/backend-clientes-auditoria
```

Reglas de trabajo:

- Crear cada rama desde `main` y mantenerla acotada a una funcionalidad o módulo.
- No hacer push directo a `main`: todo cambio entra por PR.
- Cada PR debe indicar el alcance, la spec afectada, los endpoints involucrados y las pruebas ejecutadas.
- El PR requiere revisión de al menos otro integrante y debe pasar las verificaciones del proyecto antes del merge.
- Usar `spec-checker` para validar el contrato API/DER y `db-seeder` cuando el cambio requiera recrear la base local.
- Eliminar la rama de funcionalidad después del merge.
