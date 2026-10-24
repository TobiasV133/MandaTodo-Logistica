# MandáTodo

MandáTodo es un sistema interno de logística para operaciones de distribución, diseñado en español rioplatense. El proyecto se encuentra actualmente en el Módulo 2. El orden de presentación acordado para el alcance es: Módulo 1, Módulo 2, Módulo 4, Módulo 5 y Módulo 3.

La mayoría de las pantallas del prototipo utiliza datos simulados en memoria y el acceso sigue siendo de demostración. La pantalla Gestión de pedidos consume los endpoints existentes del backend para listar pedidos, cambiar estados y descargar remitos PDF. La geocodificación y los mapas usan servicios externos.

## Tecnologías

- JavaScript con React para la interfaz y sus componentes.
- React Leaflet para mapas interactivos.
- SQL para la gestión de datos relacionales.
- Java con Spring Boot para el backend.

## Módulos

El alcance funcional y las reglas específicas de cada módulo están separados en instrucciones por carpeta bajo `src/features/`. El Módulo 3 se presenta al final según el orden acordado, sin cambiar su numeración.

Las guías por módulo documentan el alcance y las decisiones futuras; no implican que esas funciones ya estén implementadas. Salvo la integración de Gestión de pedidos descrita arriba, el prototipo conserva datos en memoria, acceso de demostración y acciones simuladas. Las pantallas existentes están concentradas en `src/components/` y se rigen por su propio `AGENTS.md`; las guías de `src/features/` aplican al código que se ubique en esas carpetas.

## Dominio

El modelo representa clientes particulares y empresas, sus direcciones y pedidos de carga. Se define mediante tipos e interfaces:

- `TipoCliente`: `Particular` o `Empresa`.
- `TipoDocumento`: `DNI` o `CUIT`.
- `EstadoPedido`: `en_revision`, `pendiente`, `en_transito`, `cancelado_transito`, `entregado` o `fallido`.
- `MotivoFalla`: `Ausente`, `Dirección Inexistente`, `Dirección Incompleta`, `Rehúsa la entrega`, `Fuera de horario`, `Acceso restringido` u `Otro`.
- `AlertaRevision`: `geo_ambigua`, `altura_cero`, `duplicado_empresa`, `sin_numero`, `fiabilidad_baja` o `fiabilidad_media`.

### Entidades y atributos

- `Direccion`: `id` identifica el registro; `raw` conserva el texto original; `calle`, `altura`, `piso`, `depto`, `localidad` y `provincia` describen el domicilio; `lat` y `lng` guardan coordenadas opcionales; `confirmada` indica si la ubicación fue confirmada.
- `Cliente`: `id`, `nombre`, `documento`, `tipoDocumento` y `tipoCliente` identifican y clasifican al cliente; `esFrecuente` señala su condición de cliente habitual; `telefono` y `email` contienen sus datos de contacto; `direcciones` reúne sus domicilios; `creadoEn` registra su fecha de alta.
- `Pedido`: `id`, `clienteId` y `direccionId` identifican el pedido y sus relaciones; `descripcion`, `peso`, `monto` y `kilometros` describen la carga y su tarifa; `fechaEntrega` y `estado` indican planificación y situación; `alertas` contiene observaciones para revisión; `motivoFalla` y `notaFalla` son opcionales y describen una entrega fallida; `hojaRutaGenerada`, `remitoGenerado` y `recargoReprogramacion` son datos opcionales asociados a la operación; `creadoEn` y `actualizadoEn` registran fechas.

Los roles disponibles en el prototipo son Administrador y Operario. Los datos, los permisos y las acciones de usuarios son simulaciones de la interfaz, no controles de acceso reales.

## Interfaz

La experiencia está orientada a tareas operativas de logística: alta y seguimiento de cargas, revisión de incidencias, gestión de clientes y consulta de actividad. El lenguaje visible es español rioplatense.

La dirección visual es clara, compacta y corporativa. Usa fondo gris muy claro (`#F8F9FA`), superficies blancas (`#FFFFFF`), bordes suaves (`#E2E8F0` y `#CBD5E1`), texto principal azul noche (`#0F172A`) y texto secundario (`#64748B`). El azul cobalto (`#2563EB`, hover `#1D4ED8`) es el color de acción; verde (`#15803D`), ámbar (`#92400E`), rojo (`#B91C1C`) y celeste (`#0369A1`) comunican estados. La navegación lateral usa azul noche (`#0F172A`) con superficies elevadas (`#1E293B`).

La tipografía de interfaz es Inter y la tipografía monoespaciada es DM Mono, usada para cifras, fechas e identificadores. La composición prioriza información operativa legible, superficies blancas, bordes sutiles, radios moderados, sombras discretas y etiquetas de estado suaves.

## Convenciones del proyecto

- Mantener en español rioplatense los textos visibles, mensajes, fechas y terminología de la interfaz.
- Usar componentes funcionales de React, hooks y tipos explícitos, respetando el modelo de dominio descrito arriba.
- Mantener coherentes los datos compartidos y evitar duplicar clientes o pedidos en estados independientes.
- Conservar la dirección visual y los patrones de interacción existentes, incluidos estados vacíos, confirmaciones, indicadores de carga y feedback de acciones.
- Priorizar controles accesibles: etiquetas claras, foco visible, navegación por teclado y elementos nativos cuando sean adecuados.
- Evitar dependencias nuevas cuando la funcionalidad ya esté cubierta por las tecnologías existentes.
- Tratar el acceso, los permisos, los datos, la persistencia y las acciones operativas como simulaciones; no presentarlos como capacidades reales de backend.
