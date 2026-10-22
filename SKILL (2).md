---
name: mandatodo-design
description: Sistema de diseño de MandáTodo (logística interna, modo claro corporativo) construido solo con React y react-leaflet. Usar al crear o modificar pantallas, badges, tablas, formularios o mapas del proyecto.
---

# Diseño de MandáTodo con React + react-leaflet

Prototipo frontend-only, en español rioplatense. No hay librerías de UI, de iconos, de formularios ni de mapas adicionales: todo se resuelve con componentes funcionales de React, hooks, estado del `AppProvider` y `react-leaflet` sobre Leaflet. El estilo se escribe con clases de Tailwind v4 directamente en JSX (único complemento de estilos; sin `tailwind.config.*`).

## 1. Principios

- Compacto y operativo: mucha información por pantalla, poco adorno.
- Modo claro corporativo: fondo gris casi blanco, tarjetas blancas, bordes finos, sombras mínimas.
- Color con significado: el color solo comunica estado (info, advertencia, éxito, peligro, acento).
- Datos técnicos en monoespaciada: IDs, fechas, montos, coordenadas y microetiquetas.
- Nunca fingir backend: login, PDF, impresión y persistencia son simulaciones locales.

## 2. Tokens (`src/index.css`, bloque `@theme`)

| Uso | Valor |
| --- | --- |
| Fondo app | `#F8F9FA` |
| Superficie / tarjeta | `#FFFFFF` (secundaria `#F1F5F9`) |
| Borde / borde fuerte | `#E2E8F0` / `#CBD5E1` |
| Texto / secundario | `#0F172A` / `#64748B` |
| Sidebar / elevado | `#0F172A` / `#1E293B` |
| Acento / hover | `#2563EB` / `#1D4ED8` |
| Éxito · Advertencia · Peligro · Info | `#15803D` · `#92400E` · `#B91C1C` · `#0369A1` |

Tipografía: **Inter** para la UI y **DM Mono** para códigos, fechas, cifras y cabeceras técnicas. El orden de `@import` en `index.css` es: Google Fonts, `leaflet/dist/leaflet.css`, `tailwindcss`.

## 3. Patrones de componente

- **Tarjeta**: `bg-white border border-[#E2E8F0] rounded-xl`.
- **Input**: `bg-white border border-[#E2E8F0] rounded-lg px-3 py-2 text-sm`, foco con borde `#2563EB`. Se guarda en una constante local (`field`, `fieldCls`, `inputCls`) para no repetir la cadena.
- **Botón primario**: `#2563EB`, hover `#1D4ED8`, texto blanco. Destructivo en rojo, aprobar en verde, precaución en ámbar.
- **Cabecera de tabla**: `bg-[#F1F5F9] text-[10px] font-mono uppercase tracking-wider text-[#64748B]`.
- **Filtros**: segmentos en una barra blanca; el activo va en azul sólido y muestra un contador en mono.
- **Texto**: título de pantalla `text-base font-semibold`; auxiliar `text-xs text-[#64748B]`.
- **Anchos**: formularios y revisión `max-w-3xl mx-auto`; tablas `max-w-5xl` o `min-w-*` con scroll horizontal.
- **Overlays**: fondo oscuro, panel siempre claro. Drawer a la derecha para edición; modal centrado para confirmar. El remito simula una hoja A4 blanca.
- **Iconos**: SVG inline, coherentes con el camión del logo. Sin emojis nuevos.

## 4. Badges pastel por estado

Fondo pastel + texto saturado del mismo tono (definidos en `GestionPedidos.tsx`):

| Estado | Fondo | Texto |
| --- | --- | --- |
| `pendiente` | `#E0F2FE` | `#0369A1` |
| `en_revision` | `#FEF3C7` | `#92400E` |
| `en_transito` | `#DBEAFE` | `#1D4ED8` |
| `cancelado_transito` (pausa) | `#F1F5F9` | `#475569` |
| `entregado` | `#DCFCE7` | `#15803D` |
| `fallido` | `#FEE2E2` | `#B91C1C` |

Las alertas de revisión siguen la misma lógica: ámbar para dudas (altura cero, sin número, fiabilidad media), rojo para riesgo (duplicado de empresa, fiabilidad baja).

## 5. Arquitectura React

- `App.tsx`: login, shell (sidebar navy + topbar) y selección de vista por estado; no hay router.
- `store.tsx`: `AppProvider` + `useApp()` con clientes, pedidos y mutaciones. No duplicar datos en estado local.
- `types.ts`: fuente de verdad del dominio (roles, estados, alertas, `MOTIVOS_FALLA`).
- `components/`: una pantalla por archivo. Activas: `Login`, `NuevaCarga`, `BandejaRevision`, `GestionPedidos`, `Clientes`, `Usuarios`, `Auditoria`. `Pedidos.tsx` y `Libreta.tsx` son heredadas y oscuras: no usar como referencia.
- Hooks nativos (`useState`, `useEffect`) y controles HTML nativos con label y foco visible.

## 6. Mapas con react-leaflet

Se usan en `NuevaCarga.tsx` (confirmar la dirección) y `BandejaRevision.tsx` (corregir el pin). Patrón común:

```tsx
<div className="rounded-lg overflow-hidden border border-[#E2E8F0]" style={{ height: 220 }}>
  <MapContainer center={[lat, lng]} zoom={14} zoomControl={false} style={{ height: '100%', width: '100%' }}>
    <TileLayer
      url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}"
      attribution="&copy; Esri"
    />
    <Marker position={[lat, lng]} icon={markerIcon} />
    <PinMover onMove={(la, ln) => setPin({ lat: la, lng: ln, moved: true })} />
    <MapFlyTo lat={lat} lng={lng} />
  </MapContainer>
</div>
```

- **Tiles**: Esri World Street Map (claros, combinan con el modo claro). Mantener la atribución.
- **Marcador**: `L.icon` con las imágenes oficiales de Leaflet desde unpkg (`iconSize [25, 41]`, `iconAnchor [12, 41]`), porque el bundler rompe las rutas por defecto.
- **Componentes sin UI** dentro de `MapContainer`, que acceden a la instancia con hooks:
  - `PinMover`: `useMapEvents({ click })` mueve el pin al hacer clic.
  - `MapFlyTo`: `useMap()` + `useEffect` llama a `map.flyTo([lat, lng], 15, { duration })` cuando cambian las coordenadas.
- El contenedor tiene altura explícita y marco igual al resto de las tarjetas. `zoomControl` va desactivado para mantener el mapa limpio.
- Estado del pin: `{ lat, lng, moved }`. Cuando `moved` es `true` se muestra la coordenada en mono verde (`✓ -34.60370, -58.38160`).
- Posición por defecto: Buenos Aires (`-34.6037`, `-58.3816`).
- Geocodificación con Nominatim. Si falla, el alta no se bloquea: la fiabilidad pasa a baja y el pedido va a revisión.
- En `BandejaRevision` los cambios de calle, altura y pin son solo visuales: aprobar no los persiste. Si se quiere persistir, agregar una mutación explícita en `store.tsx`.

## 7. Reglas al extender

1. Textos y terminología en español.
2. Usar los tokens de arriba; no reintroducir el tema oscuro ni el violeta `#8B5CF6`.
3. Cambios de dominio: primero `types.ts`, luego `store.tsx`, luego las pantallas.
4. Conservar estados vacíos, disabled, confirmaciones y feedback de acciones.
5. No agregar dependencias para lo que ya cubren React, Tailwind o react-leaflet.
6. Pantalla nueva: actualizar `View`, `navItems`, `viewTitle` y el render condicional en `App.tsx`.
7. Verificar con `pnpm exec tsc --noEmit`; usar `pnpm build` si el cambio es amplio.
