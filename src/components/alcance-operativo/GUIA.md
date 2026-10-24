# Guía de lógica operativa

Esta carpeta reúne los requisitos funcionales que deben tenerse en cuenta al trabajar en las pantallas operativas de `src/components/`. No reemplaza la especificación del proyecto ni describe funciones futuras como si ya estuvieran implementadas.

## Pantallas relacionadas

| Archivo | Responsabilidad |
| --- | --- |
| `../GestionPedidos.tsx` | Tabla de pedidos, búsqueda, filtros por estado, cambios de estado, reprogramación, recargos y remitos. |
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
- Generar o descargar un PDF real requiere el servicio backend; una vista imprimible o una descarga simulada del frontend no equivale a esa capacidad.

## Estado actual y brechas conocidas

El proyecto actual es un prototipo frontend-only: React mantiene datos simulados en memoria y no se encontró implementación Java/Spring Boot. `schema.sql` define tablas y restricciones, pero no constituye por sí mismo la lógica de negocio del backend.

- `GestionPedidos.tsx` ofrece filtros y vistas de remito. La acción de recargo es manual; no implementa por sí sola el cálculo automático ni la fecha del día siguiente.
- `NuevaCarga.tsx` detecta posibles duplicados de Empresa comparando pedidos activos, pero la comprobación actual no limita explícitamente la búsqueda al mismo día.
- `BandejaRevision.tsx` permite aprobar o rechazar. La edición de dirección y pin es visual y no se persiste actualmente en el store.
- Las vistas de remito son imprimibles/simuladas. El motivo de reimpresión se muestra en la vista previa, pero no se guarda de forma persistente ni queda asociado a un evento real de auditoría.
- El contrato de Spring Boot, los endpoints, la auditoría y la generación de PDF están definidos en la especificación y documentación; no deben presentarse como backend ya conectado.
- La especificación menciona React + Bootstrap, pero la interfaz existente usa clases de Tailwind. No cambiar el sistema visual al implementar estas reglas sin que el alcance lo solicite.

## Criterio al implementar cambios

1. Mantener estados, alertas y motivos tipados desde `src/types.ts`, evitando catálogos duplicados.
2. Aplicar las reglas de negocio en una fuente coherente. Mientras no exista backend, distinguir explícitamente la simulación frontend de la regla que deberá garantizar el servidor.
3. Actualizar en conjunto las pantallas afectadas y el estado compartido para que tabla, bandeja, fechas, recargos y documentos no discrepen.
4. Validar especialmente los límites de fecha (mismo día para duplicados y día siguiente para reprogramar), los motivos obligatorios y la prevención de recargos repetidos.
5. Mantener el motivo de reimpresión y el historial de auditoría al persistirlos; no dar por registrada una acción solo porque aparezca en la interfaz.
