# Pantallas de MandáTodo — guía para agentes

Estas instrucciones complementan el `AGENTS.md` raíz y aplican a `src/components/`.

## Mapa de pantallas activas

| Archivo | Responsabilidad |
| --- | --- |
| `Login.tsx` | Acceso simulado con `admin` / `admin`, error y estado de carga. |
| `NuevaCarga.tsx` | Wizard Cliente → Dirección → Pedido → Confirmar; geocodificación, mapa, cálculo de tarifa, alertas y remito. |
| `BandejaRevision.tsx` | Pedidos retenidos, detalle de alertas, ajuste visual del pin y aprobación/rechazo. |
| `GestionPedidos.tsx` | Interfaz de gestión conectada al backend: búsqueda, filtros, detalle, cambios de estado y descarga de remitos PDF. Su implementación vive en `gestion-pedidos/`. |
| `Clientes.tsx` | Búsqueda, resumen operativo y edición inline de clientes. |
| `Usuarios.tsx` | CRUD simulado en memoria para roles Administrador/Operario. |
| `Auditoria.tsx` | Historial mock de actividad con filtros. |

`Pedidos.tsx` y `Libreta.tsx` son implementaciones heredadas, oscuras y actualmente no montadas. No usarlas como referencia visual o funcional.

## Flujo de alta de carga

`NuevaCarga.tsx` concentra el flujo operativo más sensible:

1. Busca clientes por DNI/CUIT normalizado y autocompleta los datos.
2. Parsea la dirección libre con `parseAddress`.
3. Consulta Nominatim al normalizar/geolocalizar y permite confirmar/mover el pin con Leaflet.
4. Sugiere tarifa con `1500 + kg × 250 + km × 180`; el monto sigue siendo editable.
5. Detecta alertas con `detectAlertas`, confianza geográfica y duplicados de empresas.
6. Crea cliente/dirección si no existen y luego crea el pedido.
7. Envía el resultado a Pedidos o a En revisión según las alertas.

Al cambiar este flujo, conservar la navegación hacia atrás, las validaciones por paso, el resumen previo y la pantalla de resultado. No bloquear el alta por un fallo de red de geocodificación: hoy se degrada a fiabilidad baja.

## Mapas y direcciones

- Se usa `react-leaflet`/Leaflet.
- Los iconos del marker se cargan desde unpkg.
- El alta usa tiles de Esri y Nominatim para geocodificación.
- La posición por defecto es Buenos Aires (`-34.6037`, `-58.3816`).
- `BandejaRevision` mantiene campos y pin en estado local visual; actualmente aprobar no persiste esas ediciones en el cliente. No afirmar lo contrario.
- Si se implementa persistencia de una dirección corregida, añadir una mutación explícita al store en lugar de modificar objetos por referencia.

## Patrones visuales de componentes

- Contenedor de pantalla: `flex-1` con overflow controlado; contenido principal suele usar `p-6`.
- Ancho de formularios y revisión: `max-w-3xl mx-auto`.
- Ancho de tablas administrativas: `max-w-5xl` o tabla con `min-w-*` y scroll horizontal.
- Panel/tarjeta: blanco, `border-[#E2E8F0]`, `rounded-xl`; sombra solo cuando ayuda a separar overlays o documentos.
- Inputs: fondo blanco, borde `#E2E8F0`, texto `#0F172A`, foco `#2563EB`, radio `rounded-lg`.
- Botón primario: `#2563EB`, hover `#1D4ED8`, texto blanco.
- Acciones destructivas: rojo; aprobación: verde; revisión/precaución: ámbar.
- Títulos de pantalla: aproximadamente `text-base font-semibold`.
- Texto auxiliar: `text-xs text-[#64748B]`.
- Cabeceras técnicas: `text-[10px] font-mono uppercase tracking-wider`.
- IDs, fechas y montos: `font-mono`.

Reutilizar clases constantes locales (`field`, `fieldCls`, `inputCls`) dentro de pantallas complejas antes de repetir cadenas largas. Si un patrón llega a compartirse entre varias pantallas, extraerlo solo cuando la reutilización sea real.

## Estados de pedido y tonos

Mantener estos significados en tablas, badges y mensajes:

- `pendiente`: celeste/informativo.
- `en_revision`: ámbar/advertencia.
- `en_transito`: azul cobalto.
- `cancelado_transito`: gris o azul tenue; significa pausa, no eliminación.
- `entregado`: verde/éxito.
- `fallido`: rojo/peligro.

Todos los filtros y selectores de estado deben considerar la unión completa de `EstadoPedido`, salvo que una acción tenga una transición deliberadamente limitada.

## Modales, drawers y documentos

- Drawers de edición: overlay oscuro y panel blanco anclado a la derecha.
- Modales de confirmación: panel blanco centrado, con cierre/cancelación visibles.
- Gestión de pedidos solicita el PDF real al endpoint de remitos; otras vistas pueden seguir usando vistas imprimibles simuladas. Conservar legibilidad del documento sobre blanco y no mezclar el tema del overlay con el documento.
- La reimpresión debe indicar que es duplicado y conservar el motivo.
- Las acciones simuladas deben comunicar feedback sin prometer una operación backend real. Gestión de pedidos es una excepción: mostrar errores reales de API y no simular éxito si falla una solicitud.

## Límites y consistencia

- `Usuarios.tsx` y `Auditoria.tsx` no consumen `useApp`; sus datos son mocks locales.
- El sidebar muestra un usuario Administrador fijo; todavía no existe autorización por rol.
- No añadir un tercer rol.
- No crear un segundo catálogo de estados, motivos o alertas si puede derivarse de `src/types.ts`.
- Al agregar una pantalla activa, actualizar el tipo `View`, `navItems`, `viewTitle` y el render condicional en `src/App.tsx`.
- Mantener responsive básico: evitar anchos fijos sin scroll y conservar grids adaptables donde ya existen.
