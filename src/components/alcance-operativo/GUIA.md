# Guía de lógica operativa

Esta carpeta reúne los requisitos funcionales que deben tenerse en cuenta al trabajar en las pantallas operativas de `src/components/`. No reemplaza la especificación del proyecto ni describe funciones futuras como si ya estuvieran implementadas.

## Pantallas relacionadas

| Archivo | Responsabilidad |
| --- | --- |
| `../GestionPedidos.tsx` | Punto de entrada de la pantalla conectada. |
| `../gestion-pedidos/GestionPedidos.tsx` | Tabla, filtros, detalle, cambios de estado y descarga real del PDF. |
| `../gestion-pedidos/api.ts` | Tipos y llamadas HTTP para el contrato del backend. |
| `../BandejaRevision.tsx` | Tarjetas de pedidos retenidos, motivos de alerta y acciones de aprobación o rechazo. |
| `../NuevaCarga.tsx` | Alta de pedidos, validación de dirección, detección de duplicados y primera vista del remito. |
| `../../store.tsx` | Estado compartido en memoria, transiciones simuladas y funciones de análisis de direcciones. |
| `../../types.ts` | Tipos de estados, alertas, pedidos, motivos de falla y clientes. |
| `../../../schema.sql` | Esquema MySQL de pedidos, revisiones y auditoría. |
| `../../../.openspec/specs/manda-todo/manda-todo.spec.md` | Reglas de negocio y contrato previsto de la API. |

## Requisitos funcionales

### Gestión de pedidos

- Mantener una tabla filtrable por estado y consistente con todos los valores de `EstadoPedido`.
- Los estados operativos principales incluyen Pendiente, En tránsito, Entregado y Fallido; también deben contemplarse En Revisión y Cancelado en tránsito.
- Comunicar claramente el estado, la fecha de entrega, el motivo de falla y cualquier recargo asociado.

### Bandeja «En Revisión»

- Mostrar los pedidos retenidos como tarjetas con sus alertas y datos relevantes para decidir.
- Retener pedidos por dirección ambigua, como altura `0` o `S/N`, y por posible duplicado comercial.
- Un pedido idéntico de un cliente Empresa registrado el mismo día debe pasar a revisión; no aplicar esa regla a clientes Particulares.
- Permitir aprobar o rechazar el pedido y reflejar el resultado en el estado compartido.
- Si se corrigen datos de dirección o el pin, persistir los cambios mediante una mutación explícita del estado compartido; no dejarlos únicamente en estado visual local.

### Entregas fallidas y reprogramación

- Exigir un motivo de falla al registrar una entrega fallida.
- La regla de negocio prevista es reprogramar automáticamente para el día siguiente y aplicar una sola vez un recargo del 50 % sobre el costo base del envío.
- Mostrar fecha reprogramada, motivo y monto del recargo de forma coherente en las pantallas.
- No confundir el recargo porcentual con el monto del envío ni permitir que una misma reprogramación lo aplique varias veces.

### Remitos y reimpresiones

- El remito debe incluir el detalle del pedido, peso en kilos, kilómetros, costo y espacio para firma y aclaración del receptor.
- La reimpresión debe identificarse como duplicado, requerir un motivo y conservarlo en el registro de auditoría.
- La pantalla Gestión de pedidos descarga el PDF emitido por el servicio backend; una vista imprimible o una descarga simulada no equivale a esa capacidad.

## Integración actual y límites

El backend Spring Boot está implementado para gestión de pedidos y remitos. `gestion-pedidos/api.ts` consume `GET /api/pedidos`, `PATCH /api/pedidos/{id}/estado` y `POST /api/remitos/{idPedido}/imprimir`. Las respuestas exitosas actualizan la interfaz; los errores HTTP y de conexión se muestran sin sustituirlos por datos falsos.

- Configurar `VITE_API_BASE_URL` (por defecto `http://localhost:8080`) y `VITE_OPERARIO_ID` en el entorno Vite. El segundo valor debe ser un operario activo existente; en los datos de prueba el Admin tiene ID `1`. No inferir la identidad del operario a partir del usuario de demostración de la interfaz.
- Al marcar una entrega como fallida, el backend exige motivo y devuelve importe del recargo y fecha reprogramada al siguiente día hábil. Mostrar esos datos que devuelve el servidor; no volver a calcularlos en el frontend.
- El backend no expone un endpoint para determinar desde la tabla si ya se imprimió el remito. La pantalla intenta la emisión y, ante el `409` específico por motivo de reimpresión faltante, solicita el motivo y reintenta.
- Los kilómetros no se guardan en la entidad pedido. Se solicitan opcionalmente para cada emisión de remito.
- `NuevaCarga.tsx` y `BandejaRevision.tsx` aún usan flujos locales; la conexión de Gestión de pedidos no los convierte en persistentes.
- No se encontraron exports ni recursos de Figma en el repositorio durante esta implementación. La pantalla nueva sigue los tokens y patrones activos del proyecto; para una réplica visual fiel, agregar capturas o assets de diseño al workspace.
- La especificación oficial menciona React + Bootstrap, pero la interfaz existente usa clases de Tailwind. Esta pantalla conserva el sistema visual activo.

## Criterio al implementar cambios

1. Mantener estados, alertas y motivos tipados desde `src/types.ts`, evitando catálogos duplicados.
2. Dejar que el backend sea la fuente de verdad para las transiciones de estado, el recargo, la reprogramación y la generación/auditoría del remito. La interfaz debe presentar lo que devuelve el servidor.
3. Al extender la integración a otras pantallas, distinguir claramente los flujos que aún son simulados y no reemplazar la persistencia real con cambios solo locales.
4. Validar especialmente los límites de fecha (mismo día para duplicados y siguiente día hábil para reprogramar), los motivos obligatorios y la prevención de recargos repetidos.
5. Mostrar el resultado de auditoría que garantiza el backend; no dar por registrada una acción solo porque aparezca en la interfaz.
