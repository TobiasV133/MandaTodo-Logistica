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
| Backend | Java Spring Boot 3.x, Spring Data JPA / Hibernate | API REST, reglas de negocio, persistencia y auditoría. |
| Base de Datos | MySQL 8.x (`mandatodo_db`) | Almacenamiento transaccional de clientes, pedidos, revisiones, operarios y auditoría. |
| Documentación / PDF | OpenPDF / iText | Generación y reimpresión de remitos de entrega en PDF. |

### Puesta en marcha local

Requisitos: Node.js 20.19+ o 22.12+, JDK 21+, MySQL 8.x y el cliente `mysql` en el `PATH`.
Maven es opcional: el wrapper incluido en `backend/` descarga Maven 3.9.11 la primera vez.

**Backend**

```bash
mysql -u root -p < schema.sql
mysql -u root -p < seeds.sql
cd backend
DB_USERNAME=root DB_PASSWORD=tu_clave ./mvnw spring-boot:run
```

En PowerShell:

```powershell
Get-Content .\schema.sql | mysql -u root -p
Get-Content .\seeds.sql | mysql -u root -p
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "tu_clave"
.\mvnw.cmd spring-boot:run
```

Si `mysql` no está en el `PATH`, usá la ruta completa, por ejemplo
`Get-Content .\schema.sql | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p`.
En PowerShell no funciona la redirección `<` de Bash.

Queda disponible en `http://localhost:8080`.

**Frontend**

```bash
npm install
cp .env.example .env    # en Windows: copy .env.example .env
npm run dev
```

Abre `http://localhost:5173`. El login sigue siendo de demostración (`admin` / `admin`) y no
autentica contra el backend.

| Variable | Dónde se configura | Valor por defecto | Uso |
| --- | --- | --- | --- |
| `VITE_API_BASE_URL` | `.env` del frontend | `http://localhost:8080` | URL base del backend. |
| `VITE_OPERARIO_ID` | `.env` del frontend | sin valor | Operario que queda en la auditoría. Con `seeds.sql`, `1` = Laura Gómez (Admin). |
| `DB_URL` | Entorno del backend | `jdbc:mysql://localhost:3306/mandatodo_db?...` | Conexión MySQL. |
| `DB_USERNAME` / `DB_PASSWORD` | Entorno del backend | `root` / vacío | Credenciales de MySQL. |
| `CORS_ALLOWED_ORIGINS` | Entorno del backend | `http://localhost:5173,http://127.0.0.1:5173` | Orígenes permitidos, separados por coma. |
| `PORT` | Entorno del backend | `8080` | Puerto de la API. |

Notas de configuración:

- Las variables `VITE_*` se leen al iniciar Vite. Al modificar `.env`, Vite reinicia el servidor; si el
  valor no se refleja, reiniciá `npm run dev`.
- Vite usa `strictPort` en `5173`; si el puerto está ocupado, falla en lugar de cambiar de puerto y
  romper CORS. Si usás `npm run preview` (puerto `4173`), agregá ese origen a `CORS_ALLOWED_ORIGINS`.
- El login no determina `VITE_OPERARIO_ID`: la pantalla de Gestión de pedidos necesita un ID de
  operario existente para poder auditar cambios de estado y remitos.
- `npm run typecheck` valida TypeScript y `npm run build` genera `dist/`.

## 4. Arquitectura de datos y DER

### Entidades principales

- **`clientes`**: libreta digital de clientes. Almacena `tipo_documento`, `numero_documento` (UNIQUE), `nombre_razon_social`, `telefono`, `email`, `tipo_cliente` (`Particular` / `Empresa`) y `es_frecuente`.
- **`pedidos`**: núcleo operativo. Referencia a `clientes` y registra descripción, peso, monto de envío, estado, motivo de falla, dirección normalizada (`calle`, `altura`, `piso_depto`, `localidad`), coordenadas, fechas y motivo de cancelación en tránsito.
- **`revisiones`**: bandeja de seguridad. Referencia a `pedidos` y retiene aquellos casos con `Dirección ambigua` o `Posible duplicado comercial`, con resolución `PENDIENTE`, `APROBADO` o `RECHAZADO`.
- **`operarios`**: usuarios del sistema. Incluye `email` (UNIQUE), `password_hash`, `rol` (`Admin` / `Operario`) y `estado` (`Activo` / `Inactivo`).
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
- Un pedido idéntico de un cliente `Empresa` registrado el mismo día genera una revisión por `Posible duplicado comercial`; para clientes `Particular` el pedido pasa directamente a `PENDIENTE`.
- La tabla `auditoria` es **append-only**: los triggers `trg_auditoria_no_update` y `trg_auditoria_no_delete` bloquean `UPDATE` y `DELETE` a nivel de base de datos.

## 5. Principales endpoints de la API REST

| Método | Endpoint | Descripción |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Inicia sesión de un operario. |
| `POST` | `/api/geo/normalizar` | Normaliza y geocodifica una dirección mediante Nominatim. |
| `POST` | `/api/pedidos` | Crea un pedido en `PENDIENTE` o `EN_REVISION` según las reglas. |
| `GET` | `/api/clientes/buscar?doc=...` | Autocompleta un cliente por DNI/CUIT. |
| `GET` | `/api/clientes` | Lista todos los clientes. |
| `PUT` | `/api/clientes/{id}` | Edita los datos de un cliente. |
| `GET` | `/api/pedidos` | Lista pedidos; acepta el filtro `?estado=...`. |
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

### Backend implementado (corte actual)

El backend Spring Boot vive en [`backend/`](backend/README.md). Por ahora implementa el corte del
**Motor de Retención "En Revisión"**, la **lógica de Entregas Fallidas** y la **generación de
remitos PDF**:

- `POST /api/pedidos`: evalúa dirección ambigua (altura 0, S/N, geocodificación no confiable) y
  duplicado comercial de clientes Empresa del mismo día.
- `GET /api/pedidos/en-revision`: bandeja de pedidos retenidos, agrupados por pedido.
- `PUT /api/pedidos/{id}/revision`: aprueba o rechaza la revisión.
- `PATCH /api/pedidos/{id}/estado`: al marcar `FALLIDO`, exige motivo, reprograma para el siguiente
  día hábil y aplica un recargo del 50% sobre el costo de envío.
- `POST /api/remitos/{idPedido}/imprimir`: devuelve el remito A4 en PDF con detalle, kilos,
  kilómetros, importe y espacio de firma. Las reimpresiones exigen motivo y quedan registradas como
  evento `Reimpresión` en la auditoría.
- `GET /api/pedidos` y `GET /api/pedidos/{id}`: consulta operativa.

El resto de los endpoints del contrato queda pendiente para próximos cortes. La puesta en marcha,
los ejemplos de request y las decisiones de implementación están documentados en
[`backend/README.md`](backend/README.md).

La pantalla de Gestión de pedidos (`src/components/gestion-pedidos/`) ya consume el listado,
los cambios de estado y la descarga de remitos PDF del backend. Para registrar las acciones,
configurá en el entorno de Vite `VITE_API_BASE_URL` (por defecto `http://localhost:8080`) y
`VITE_OPERARIO_ID` con el ID de un operario activo; con los datos de prueba, el Admin es el ID `1`.
El login de demostración del frontend aún no está conectado a autenticación y no determina ese ID.

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
