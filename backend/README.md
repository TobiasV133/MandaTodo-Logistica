# MandáTodo Logística — Backend

Backend Spring Boot del corte funcional **Motor de Retención "En Revisión" + Entregas Fallidas +
Remitos PDF**.
Se apoya en el esquema MySQL existente (`schema.sql` / `seeds.sql`) y en la especificación viva
(`.openspec/specs/manda-todo/manda-todo.spec.md`).

## Alcance implementado

| Endpoint | Descripción |
| --- | --- |
| `POST /api/pedidos` | Crea un pedido y aplica el motor de retención. Queda `PENDIENTE` o `EN_REVISION`. |
| `GET /api/pedidos` | Lista pedidos; acepta `?estado=...`. |
| `GET /api/pedidos/{id}` | Detalle de un pedido con sus revisiones. |
| `GET /api/pedidos/en-revision` | Bandeja de pedidos retenidos, agrupados por pedido. |
| `PUT /api/pedidos/{id}/revision` | Resuelve la revisión con `APROBAR` o `RECHAZAR`. |
| `PATCH /api/pedidos/{id}/estado` | Cambia el estado. Si el destino es `FALLIDO`, aplica la lógica de entrega fallida. |
| `POST /api/remitos/{idPedido}/imprimir` | Genera o reimprime el remito en PDF; en reimpresiones exige y registra el motivo. |

Los demás endpoints del contrato (login, geocodificación, clientes, usuarios y consulta de
auditoría) quedan fuera de este corte. La auditoría se **escribe** automáticamente en cada operación,
pero todavía no se expone un endpoint de consulta.

## Requisitos

- JDK 21 o superior.
- Maven 3.9 o superior.
- MySQL 8.x con la base `mandatodo_db` creada a partir de `schema.sql`.
- Opcional: cargar `seeds.sql` para tener operarios y pedidos de prueba.

## Puesta en marcha

```bash
# Desde la raíz del repositorio
mysql -u root -p < schema.sql
mysql -u root -p < seeds.sql

cd backend
mvn spring-boot:run
```

Variables de entorno útiles:

| Variable | Valor por defecto |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/mandatodo_db?...` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | vacío |
| `PORT` | `8080` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |

La aplicación escucha en `http://localhost:8080`.

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
Content-Type: application/json

{
  "operarioId": 1,
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
  "operarioId": 1,
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
Content-Type: application/json

{
  "operarioId": 1,
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
Content-Type: application/json

{
  "accion": "APROBAR",
  "operarioId": 1,
  "calle": "Av. Corrientes",
  "altura": 1234,
  "pisoDepto": "4 B",
  "localidad": "Ciudad Autónoma de Buenos Aires"
}
```

### Marcar una entrega como fallida

```http
PATCH /api/pedidos/10/estado
Content-Type: application/json

{
  "estado": "FALLIDO",
  "motivoFalla": "Ausente",
  "notaFalla": "El receptor no se encontraba en el domicilio",
  "operarioId": 1
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
  interfaz pueda mostrarlo.
- **No se aplica la exención de clientes frecuentes del prototipo** porque la especificación no la
  define como regla de backend. El motor evalúa todas las direcciones y todos los duplicados de
  Empresa.
- **`operarioId` es obligatorio** en las operaciones que escriben auditoría. Todavía no hay login ni
  autorización real: el identificador se envía en el body.
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

## Pruebas

```bash
cd backend
mvn test
```

Cobertura del corte:

- `MotorRetencionTest`: altura 0, S/N, localidad faltante, fiabilidad media/baja, duplicado de
  Empresa, exclusión de Particulares, pedidos de otro día, normalización de datos y acumulación de
  motivos.
- `EntregaFallidaServiceTest`: recargo del 50%, reprogramación al lunes, feriados, fecha programada
  futura, motivo obligatorio y protección contra doble recargo.
- `CalendarioHabilTest`: fines de semana y feriados.
- `PedidoServiceTest`: alta en `PENDIENTE` / `EN_REVISION`, persistencia de revisiones y exclusión de
  pedidos cancelados.
- `RevisionServiceTest`: aprobación con corrección de dirección y rechazo con lógica de entrega
  fallida.
- `RemitoPdfGeneratorTest`: PDF A4 de una página, detalle, kilos, kilómetros, importe, firma,
  reimpresión marcada con su motivo y kilómetros sin informar.
- `RemitoServiceTest`: primera impresión, motivo obligatorio al reimprimir, registro del evento
  `Reimpresión`, pedidos en revisión o cancelados y operario inexistente.
- `PersistenciaTest`: mapeo JPA y consultas contra H2 en modo de prueba.

## Estructura

```text
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/mandatodo/logistica/
    │   │   ├── auditoria/       # Registro append-only
    │   │   ├── cliente/         # Entidad y repositorio de clientes
    │   │   ├── config/          # Clock y CORS
    │   │   ├── operario/        # Entidad y repositorio de operarios
    │   │   ├── pedido/          # Pedidos, estados y entregas fallidas
    │   │   ├── remito/          # Generación de remitos PDF y reimpresiones
    │   │   ├── revision/        # Motor de retención y bandeja
    │   │   └── shared/          # Calendario hábil, errores y normalización
    │   └── resources/application.yml
    └── test/                    # Pruebas unitarias y de persistencia
```
