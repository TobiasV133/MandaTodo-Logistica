# MandáTodo Logística — Backend

Backend Spring Boot del sistema **MandáTodo Logística**, integrado con el frontend oficial de
`main`. Implementa autenticación JWT, alta de pedidos con motor de retención, bandeja de revisión,
entregas fallidas con recargo, remitos PDF, clientes y geocodificación con Nominatim.
Se apoya en el esquema MySQL existente (`schema.sql` / `seeds.sql`) y en la especificación viva
(`.openspec/specs/manda-todo/manda-todo.spec.md`).

## Alcance implementado

Todos los endpoints requieren `Authorization: Bearer <token>` salvo `POST /api/auth/login`.

| Endpoint | Descripción |
| --- | --- |
| `POST /api/auth/login` | Login por email y contraseña (BCrypt). Devuelve el JWT y los datos del operario. |
| `GET /api/auth/me` | Devuelve el operario autenticado a partir del JWT. |
| `POST /api/geo/normalizar` | Normaliza y geocodifica una dirección con OpenStreetMap/Nominatim. |
| `GET /api/clientes` | Lista los clientes ordenados por razón social. |
| `GET /api/clientes/buscar?doc=...` | Busca un cliente por DNI/CUIT normalizado. |
| `POST /api/pedidos` | Crea un pedido y aplica el motor de retención. Queda `PENDIENTE` o `EN_REVISION`. |
| `GET /api/pedidos` | Lista pedidos; acepta `?estado=...`. |
| `GET /api/pedidos/resumen` | Totales de pendientes, en revisión, entregados y clientes para el dashboard. |
| `GET /api/pedidos/{id}` | Detalle de un pedido con sus revisiones. |
| `GET /api/pedidos/en-revision` | Bandeja de pedidos retenidos, agrupados por pedido. |
| `PUT /api/pedidos/{id}/revision` | Resuelve la revisión con `APROBAR` o `RECHAZAR`. |
| `PATCH /api/pedidos/{id}/estado` | Cambia el estado. Si el destino es `FALLIDO`, aplica la lógica de entrega fallida. |
| `POST /api/remitos/{idPedido}/imprimir` | Genera o reimprime el remito en PDF; en reimpresiones exige y registra el motivo. |

Quedan pendientes del contrato completo el ABM de usuarios, la consulta de auditoría y el endpoint
dedicado de cancelación en tránsito. La cancelación se resuelve con
`PATCH /api/pedidos/{id}/estado` enviando `estado: CANCELADO_EN_TRANSITO` y `motivoCancelacion`.
La auditoría se **escribe** automáticamente en cada operación, pero todavía no se expone un endpoint
de consulta.

## Requisitos

- JDK 21 o superior.
- Maven 3.9 o superior, o usar el wrapper incluido (`./mvnw` / `.\mvnw.cmd`).
- MySQL 8.x con la base `mandatodo_db` creada a partir de `schema.sql`.
- Opcional: cargar `seeds.sql` para tener operarios, clientes y pedidos de prueba.
- Node.js 20 o superior si se levanta el frontend de `frontend/`.

## Puesta en marcha

```bash
# Desde la raíz del repositorio
mysql --default-character-set=utf8mb4 -u root -p < schema.sql
mysql --default-character-set=utf8mb4 -u root -p < seeds.sql

cd backend
DB_USERNAME=root DB_PASSWORD=tu_clave JWT_SECRET=una_clave_larga ./mvnw spring-boot:run
```

En PowerShell la redirección `<` no funciona directamente. Usá `cmd /c` para conservar la
codificación UTF-8 del esquema y de los seeds:

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -u root -p < schema.sql"
cmd /c "mysql --default-character-set=utf8mb4 -u root -p < seeds.sql"
```

Si `mysql` no está en el `PATH`, reemplazalo por la ruta completa del cliente, por ejemplo
`"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"`.

En PowerShell, el wrapper no requiere Maven instalado:

```powershell
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "tu_clave"
$env:JWT_SECRET = "una_clave_larga_para_desarrollo"
.\mvnw.cmd spring-boot:run
```

Variables de entorno útiles:

| Variable | Valor por defecto |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/mandatodo_db?...` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | vacío |
| `PORT` | `8080` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` |
| `SECURITY_ENABLED` | `true` (solo se desactiva en pruebas automáticas) |
| `JWT_SECRET` | secreto de desarrollo; **cambialo fuera del prototipo** |
| `JWT_EXPIRATION_SECONDS` | `28800` (8 horas) |
| `NOMINATIM_URL` | `https://nominatim.openstreetmap.org/search` |
| `NOMINATIM_USER_AGENT` | identificador de desarrollo de MandáTodo |
| `NOMINATIM_MIN_INTERVAL_MS` | `1100` (respeta el límite de 1 request/segundo) |

La aplicación escucha en `http://localhost:8080`. La raíz `/` devuelve un JSON informativo con el
estado del servicio y la URL del frontend; **no es la interfaz de usuario**. Para usar la aplicación
abrí `http://localhost:5173` con Vite en ejecución.

### Integración con el frontend

El frontend oficial vive en `frontend/` y usa el proxy de Vite: todas las llamadas a `/api` se
redirigen a `http://localhost:8080`. Para levantarlo:

```powershell
cd frontend
npm install
npm run dev
```

Luego ingresá en `http://localhost:5173` con un operario de `seeds.sql`:

| Email | Contraseña | Rol |
| --- | --- | --- |
| `laura.gomez@mandatodo.com` | `Admin123!` | Admin |
| `martin.perez@mandatodo.com` | `Operario123!` | Operario |
| `sofia.ramirez@mandatodo.com` | `Logistica123!` | Operario inactivo (no puede ingresar) |

El frontend guarda el JWT en `localStorage` y lo envía en `Authorization: Bearer ...`. **No se usa
`VITE_OPERARIO_ID`**: el operario de auditoría se toma del token. Si en lugar del proxy se consume el
backend de forma directa, creá `frontend/.env` con:

```dotenv
VITE_API_URL=http://localhost:8080/api
```

y agregá el origen exacto del frontend a `CORS_ALLOWED_ORIGINS`. Los headers
`X-Remito-Numero` y `X-Remito-Reimpresion` están expuestos por CORS para que la interfaz pueda
mostrar el número y si el remito es una reimpresión.

MySQL debe estar en ejecución y `DB_USERNAME` / `DB_PASSWORD` deben coincidir con las credenciales
locales. El backend no crea ni modifica el esquema (`ddl-auto: none`): hay que ejecutar `schema.sql`
y, opcionalmente, `seeds.sql`.

## Autenticación y autorización

- El campo `username` de `POST /api/auth/login` contiene el **email** del operario; la contraseña se
  verifica contra `operarios.password_hash` con BCrypt.
- El token es un JWT HS256 firmado con `JWT_SECRET`. El `LoginResponse` incluye `token`,
  `tokenType`, `expiresIn` y el operario autenticado.
- `JwtAuthInterceptor` protege `/api/**` excepto `POST /api/auth/login`; las solicitudes `OPTIONS`
  de CORS pasan sin token.
- `operarioId` sigue aceptándose en algunos DTOs por compatibilidad con pruebas y clientes antiguos,
  pero los controllers usan el identificador del JWT cuando está disponible.
- `SECURITY_ENABLED=false` existe únicamente para las pruebas automáticas. No lo uses en un entorno
  compartido.

## Contrato de alta compatible con el frontend

`POST /api/pedidos` acepta dos formas de payload:

- **Frontend de `main`**: `cliente` anidado (`tipoDocumento`, `numeroDocumento`,
  `nombreRazonSocial`, `telefono`, `email`, `tipoCliente`, `esFrecuente`),
  `descripcionPaquete`, `kilometros`, `esAmbigua` y `observacionGeo`.
- **Canónica**: `clienteId`, `descripcion`, `montoEnvio` y `fiabilidadGeocodificacion`.

Si no se envía `montoEnvio`, el backend lo calcula con la tarifa escalonada vigente:

| Nivel | Condición | Precio |
| --- | --- | --- |
| 1 | Hasta 5 kg **y** hasta 5 km | `$10.000` |
| 2 | Supera 5 kg **o** 5 km, sin superar 20 kg ni 15 km | `$30.000` |
| 3 | Más de 20 kg **o** más de 15 km | `$30.000 + $5.000` por km extra `+ $3.000` por kg extra |

Los recargos se calculan en forma proporcional a la fracción de kilómetro o kilogramo: por ejemplo,
15,5 km y 20,5 kg suman `$4.000` de recargo. La capacidad máxima es de **80 kg y 30 km**. Si el peso o la distancia superan esos límites, el alta
responde `400` con el mensaje `El pedido excede la capacidad logística máxima (80kg / 30km)` y no se
persiste el pedido. La validación también se aplica cuando el cliente envía un `montoEnvio` explícito.

La respuesta tiene la forma `{ "pedido": {...}, "retencion": {...} }`; `pedido` incluye
los campos derivados que consume el frontend (`codigoPedido`, `nombreCliente`, `numeroDocumento`,
`clienteFrecuente`, `descripcionPaquete`, `direccionCompleta`, `precioTotal`,
`esDireccionIncompleta`, `motivoRevision`, entre otros).

## Geocodificación de direcciones

`POST /api/geo/normalizar` usa Nominatim con un `User-Agent` identificable y un máximo de
una consulta por segundo. Para evitar falsos negativos cuando OpenStreetMap no indexa la altura
exacta, el servicio prueba en orden:

1. búsqueda estructurada con calle, altura y localidad;
2. búsqueda libre con calle, altura y localidad;
3. búsqueda estructurada solo por calle;
4. búsqueda libre solo por calle;
5. búsqueda por localidad.

La primera consulta con resultados se usa como ubicación. Si no se encuentra la altura exacta, o
si solo se pudo ubicar la calle o la localidad, la respuesta devuelve `esAmbigua: true` y una
`observacion` que explica el nivel de precisión. El pedido se deriva a `EN_REVISION` para
validación manual. Si ninguna consulta devuelve resultados, la API responde `422` con un mensaje
que sugiere revisar calle, localidad o agregar la provincia.

Los resultados que pertenecen a otra localidad se descartan aunque el nombre de la localidad
aparezca en el `display_name`; si la calle devuelta no coincide con la ingresada, la respuesta se
marca como ambigua con una observación específica en lugar de darla por precisa.

Cuando la dirección queda ambigua, el operario puede corregir el pin desde el mapa (arrastrando el
marcador o haciendo clic en el punto exacto). El alta se envía con las coordenadas corregidas y
`pinAjustado: true`; el pedido igualmente queda en `EN_REVISION` y la actividad de auditoría del
alta registra `ubicación del pin corregida manualmente`.

Ejemplo: `Ruta 8 45, San Miguel` no tiene altura indexada en OpenStreetMap; el servicio la ubica
a nivel de localidad y la marca como ambigua en lugar de rechazarla.

## Reglas del motor de retención

### 1. Dirección ambigua → `Dirección ambigua`

El pedido pasa a `EN_REVISION` y se crea un registro en `revisiones` cuando:

- la altura es `0` o nula (`altura_cero`);
- la calle es `S/N`, `SN`, `Sin número` o equivalente (`sin_numero`);
- la calle o la localidad están vacías (`geo_ambigua`);
- la geocodificación informada tiene fiabilidad `BAJA` o `MEDIA` (`fiabilidad_baja` / `fiabilidad_media`).

La fiabilidad es opcional en el request. Si no se envía, solo se evalúa la estructura de la dirección.

### 2. Posible duplicado comercial → `Posible duplicado comercial`

Solo aplica a clientes con `tipoCliente = Empresa`. Se compara el pedido nuevo contra los pedidos
del mismo cliente creados **el mismo día** y con la misma firma:

- descripción,
- peso,
- monto de envío,
- calle,
- altura,
- piso/depto,
- localidad.

La comparación normaliza mayúsculas, acentos, espacios y signos. Se excluyen los pedidos
`CANCELADO_EN_TRANSITO` porque una cancelación explícita no debe bloquear un nuevo pedido. Los
clientes `Particular` nunca se retienen por duplicado.

Si se cumplen ambas reglas, se persisten dos filas en `revisiones` y el pedido se muestra una sola vez
en la bandeja, con los dos motivos.

## Reglas de entrega fallida

Al hacer `PATCH /api/pedidos/{id}/estado` con `estado = FALLIDO`:

1. Se exige `motivoFalla` (`Ausente`, `Dirección Inexistente` o `Dirección Incompleta`).
2. Se aplica un recargo del **50%** sobre el `montoEnvio` vigente.
3. El monto final se guarda en `pedidos.monto_envio` (igual que en `seeds.sql`).
4. Se reprograma `fecha_programada` para el **siguiente día hábil**.
5. Se registra el evento en `auditoria` con el operario responsable y el detalle del recargo.

El siguiente día hábil se calcula desde `max(hoy, fecha_programada)` para no adelantar una entrega
que estaba programada a futuro. Se saltean sábados, domingos y los feriados configurados en:

```yaml
mandatodo:
  calendario:
    feriados:
      - 2026-12-25
      - 2027-01-01
```

Si el pedido vuelve a fallar en un reintento posterior, el 50% se calcula sobre el monto vigente en
ese momento. Un pedido ya `FALLIDO` no admite un segundo recargo sin pasar antes por otro estado.

## Generación de remitos PDF

`POST /api/remitos/{idPedido}/imprimir` devuelve el remito como descarga directa
(`application/pdf`, A4). El documento incluye:

- encabezado del emisor configurable (`mandatodo.remito.empresa`);
- número de remito determinístico `R-00000042`, derivado del identificador del pedido;
- datos del cliente: razón social, documento, teléfono y email;
- dirección de entrega y fecha programada;
- detalle de la carga con kilos, kilómetros e importe;
- totales de kilos, kilómetros y monto a cobrar;
- si el pedido tiene entregas fallidas registradas, el detalle muestra el **subtotal original** y
  debajo el **recargo por entrega fallida**; el total a cobrar es la suma de ambos;
- espacio para firma, aclaración y DNI del receptor, y fecha de entrega.

### Primera impresión y reimpresión

El esquema MySQL no tiene una columna `remito_generado`, por lo que la auditoría es la fuente de
verdad. La primera impresión registra un evento de tipo `Alta` con el texto
`Remito generado para el pedido #N (R-...)`. Si el pedido ya tiene un evento cuyo texto empieza con
`Remito `, la impresión se considera una **reimpresión**:

| Situación | `motivoReimpresion` | Evento de auditoría | Documento |
| --- | --- | --- | --- |
| Primera impresión | Opcional; se ignora | `Alta` | Remito normal |
| Reimpresión | **Obligatorio** | `Reimpresión` | Banda roja con el motivo |

El motivo se guarda en el texto del evento `Reimpresión`, que es append-only. Una reimpresión sin
motivo responde `409` y no genera PDF ni evento.

Estados que no admiten remito:

- `EN_REVISION`: primero hay que resolver la revisión.
- `CANCELADO_EN_TRANSITO`: el pedido ya no se entrega.

### Kilómetros

`schema.sql` no guarda los kilómetros del pedido. El endpoint acepta `kilometros` de forma opcional:
si se informa, se imprime en el detalle y en los totales; si no, el documento muestra
`Sin informar`. El valor no se persiste.

### Ejemplo

```http
POST /api/remitos/42/imprimir
Authorization: Bearer <token>
Content-Type: application/json

{
  "motivoReimpresion": null,
  "kilometros": 45.00
}
```

Respuesta `200 OK`:

```text
Content-Type: application/pdf
Content-Disposition: attachment; filename="remito-R-00000042.pdf"
X-Remito-Numero: R-00000042
X-Remito-Reimpresion: false
```

Para reimprimir:

```json
{
  "motivoReimpresion": "Extravío del remito original",
  "kilometros": 45.00
}
```

### Datos del emisor

```yaml
mandatodo:
  remito:
    empresa:
      nombre: MandáTodo
      subtitulo: Logística y distribución
      direccion: Av. Corrientes 1234, CABA
      telefono: 11 4567-8901
      cuit: ""
```

Los valores por defecto son de demostración: hay que reemplazarlos por los datos reales de la
empresa antes de usar los PDF fuera del prototipo.

## Ejemplos

### Crear un pedido con dirección ambigua

```http
POST /api/pedidos
Authorization: Bearer <token>
Content-Type: application/json

{
  "clienteId": 1,
  "descripcion": "Carga paletizada",
  "pesoKg": 25.00,
  "montoEnvio": 25000.00,
  "calle": "S/N",
  "altura": 0,
  "pisoDepto": null,
  "localidad": "San Miguel",
  "latitud": null,
  "longitud": null,
  "fechaProgramada": "2026-10-26",
  "fiabilidadGeocodificacion": "BAJA"
}
```

Respuesta abreviada:

```json
{
  "pedido": {
    "idPedido": 10,
    "estado": "EN_REVISION",
    "revisiones": [
      { "idRevision": 3, "motivoRetencion": "Dirección ambigua", "estadoResolucion": "PENDIENTE" }
    ]
  },
  "retencion": {
    "retenido": true,
    "motivos": ["Dirección ambigua"],
    "alertas": ["altura_cero", "sin_numero", "fiabilidad_baja"]
  }
}
```

### Aprobar una revisión corrigiendo la dirección

```http
PUT /api/pedidos/10/revision
Authorization: Bearer <token>
Content-Type: application/json

{
  "accion": "APROBAR",
  "calle": "Av. Corrientes",
  "altura": 1234,
  "pisoDepto": "4 B",
  "localidad": "Ciudad Autónoma de Buenos Aires"
}
```

### Marcar una entrega como fallida

```http
PATCH /api/pedidos/10/estado
Authorization: Bearer <token>
Content-Type: application/json

{
  "estado": "FALLIDO",
  "motivoFalla": "Ausente",
  "notaFalla": "El receptor no se encontraba en el domicilio"
}
```

Respuesta abreviada:

```json
{
  "pedido": {
    "idPedido": 10,
    "estado": "FALLIDO",
    "montoEnvio": 37500.00,
    "fechaProgramada": "2026-10-27",
    "motivoFalla": "Ausente"
  },
  "reprogramado": true,
  "recargoAplicado": 12500.00,
  "fechaReprogramada": "2026-10-27"
}
```

### Rechazar una revisión

`RECHAZAR` marca el pedido como `FALLIDO` y aplica la misma lógica de reprogramación y recargo. Si no
se informa `motivoFalla`, se usa `Dirección Inexistente`, que es el motivo que utiliza el prototipo al
rechazar una revisión.

## Decisiones de implementación

- **`notaFalla` no se persiste** porque el esquema MySQL no tiene una columna equivalente; se incluye
  en el texto del evento de auditoría.
- **El recargo se persiste dentro de `monto_envio`** porque `schema.sql` y `seeds.sql` no tienen una
  columna separada para el recargo. El response de `PATCH` devuelve `recargoAplicado` para que la
  interfaz pueda mostrarlo. Para el remito, el subtotal original se reconstruye desde la cantidad de
  eventos `marcado como FALLIDO` de la auditoría y el factor `1,50` por evento; el recargo es la
  diferencia entre `monto_envio` y ese subtotal. Así no se modifica el esquema canónico y también se
  contemplan reintentos con más de un recargo.
- **No se aplica la exención de clientes frecuentes del prototipo** porque la especificación no la
  define como regla de backend. El motor evalúa todas las direcciones y todos los duplicados de
  Empresa.
- **El login es por email** (`username` en el request) y la contraseña se valida con BCrypt contra
  `operarios.password_hash`. El JWT HS256 identifica al operario en cada request; `operarioId` en el
  body queda solo como compatibilidad para pruebas y clientes anteriores.
- **La seguridad se puede desactivar solo en tests** con `mandatodo.security.enabled=false`. En
  ejecución normal el interceptor exige el token en `/api/**` salvo el login.
- **El alta acepta el contrato del frontend de `main`**: cliente anidado, `descripcionPaquete`,
  `kilometros` y `esAmbigua`, además del contrato canónico. Si falta `montoEnvio`, se calcula con la
  tarifa vigente.
- **La tabla `auditoria` es append-only** a nivel de base de datos (triggers de `schema.sql`) y el
  repositorio de aplicación solo expone `save` y consultas.
- **No se agrega una columna `remito_generado`**: la primera impresión y las reimpresiones se
  detectan contando los eventos de auditoría del pedido cuyo texto empieza con `Remito `. Así no se
  modifica el esquema canónico y el motivo queda en un registro inalterable.
- **El número de remito es determinístico** (`R-` + `id_pedido` con 8 dígitos) porque el esquema no
  tiene una secuencia de remitos. Una reimpresión conserva el mismo número, como corresponde al
  mismo documento.
- **El PDF se genera con OpenPDF** (licencia LGPL) y fuentes estándar Helvetica/Courier, sin archivos
  externos. Los kilómetros se aceptan de forma opcional porque el esquema no los guarda.
- **`ddl-auto: none`**: el esquema se administra exclusivamente con `schema.sql`.
- **CORS expone `Content-Disposition`, `X-Remito-Numero` y `X-Remito-Reimpresion`** para que el
  frontend pueda descargar el PDF y mostrar el resultado de la reimpresión.

## Pruebas

```bash
cd backend
./mvnw test
```

Cobertura del corte:

- `MotorRetencionTest`: altura 0, S/N, localidad faltante, fiabilidad media/baja, duplicado de
  Empresa, exclusión de Particulares, pedidos de otro día, normalización de datos y acumulación de
  motivos.
- `EntregaFallidaServiceTest`: recargo del 50%, reprogramación al lunes, feriados, fecha programada
  futura, motivo obligatorio y protección contra doble recargo.
- `CalendarioHabilTest`: fines de semana y feriados.
- `TarifaServiceTest`: niveles 1, 2 y 3, ejemplo de 16 km / 21 kg, fracciones, límites exactos y
  rechazo por superar 80 kg o 30 km.
- `PedidoServiceTest`: alta en `PENDIENTE` / `EN_REVISION`, persistencia de revisiones y exclusión de
  pedidos cancelados.
- `RevisionServiceTest`: aprobación con corrección de dirección y rechazo con lógica de entrega
  fallida.
- `RemitoPdfGeneratorTest`: PDF A4 de una página, detalle, kilos, kilómetros, importe, subtotal y
  recargo por entrega fallida, firma, reimpresión marcada con su motivo y kilómetros sin informar.
- `RemitoServiceTest`: primera impresión, motivo obligatorio al reimprimir, registro del evento
  `Reimpresión`, separación de subtotal y recargo (incluido más de un reintento), pedidos en revisión
  o cancelados y operario inexistente.
- `ApiIntegracionTest`: contrato HTTP contra H2 con seguridad desactivada (CORS, listado con el
  contrato del frontend, cambio de estado con recargo, PDF con subtotal y recargo, y `409` de
  reimpresión sin motivo).
- `PersistenciaTest`: mapeo JPA y consultas contra H2 en modo de prueba.

## Estructura

```text
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/mandatodo/logistica/
    │   │   ├── auth/            # Login, sesión y DTOs de autenticación
    │   │   ├── auditoria/       # Registro append-only
    │   │   ├── cliente/         # Entidad, repositorio, servicio y controller de clientes
    │   │   ├── config/          # Clock, conversiones, CORS e interceptor JWT
    │   │   ├── geo/             # Nominatim, rate limit y normalización de direcciones
    │   │   ├── operario/        # Entidad y repositorio de operarios
    │   │   ├── pedido/          # Pedidos, estados, tarifa y entregas fallidas
    │   │   ├── remito/          # Generación de remitos PDF y reimpresiones
    │   │   ├── revision/        # Motor de retención y bandeja
    │   │   ├── security/        # JWT, BCrypt e interceptor
    │   │   └── shared/          # Calendario hábil, errores, texto y geo
    │   └── resources/application.yml
    └── test/                    # Pruebas unitarias, de persistencia e integración HTTP
```
