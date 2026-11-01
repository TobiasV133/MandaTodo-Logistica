# MandáTodo Logística — Frontend

Interfaz React + Vite del sistema de gestión de envíos. Consume la API Spring Boot de
`backend/` y usa OpenStreetMap/Nominatim para validar direcciones sin claves pagas.

## Requisitos

- Node.js 20 o superior y npm.
- Backend en `http://localhost:8080`.
- MySQL 8.x con `mandatodo_db` creada desde `schema.sql` y, opcionalmente, `seeds.sql`.

## Puesta en marcha

```powershell
cd frontend
npm install
npm run dev
```

Abrí `http://localhost:5173`. Vite redirige `/api` a `http://localhost:8080` mediante el proxy
configurado en `vite.config.js`.

> **VS Code:** si usás **Run and Debug**, elegí la configuración **Frontend: Vite** (o
> **Full stack: backend + frontend**). El puerto `8080` es la API Spring Boot, no la aplicación:
> abrir `http://localhost:8080` en el navegador muestra una respuesta JSON del backend, no la
> interfaz. El backend usa por defecto `root` sin contraseña; si tu MySQL tiene clave, definí
> `DB_PASSWORD` antes de arrancarlo. **Frontend: Vite** abre automáticamente `http://localhost:5173` en el
> navegador.

Usuarios de prueba de `seeds.sql`:

| Email | Contraseña | Rol |
| --- | --- | --- |
| `laura.gomez@mandatodo.com` | `Admin123!` | Admin |
| `martin.perez@mandatodo.com` | `Operario123!` | Operario |
| `sofia.ramirez@mandatodo.com` | `Logistica123!` | Operario inactivo (no puede ingresar) |

## Variables de entorno

El frontend usa `VITE_API_URL` como base de Axios. Si no se define, usa `/api` y funciona con el
proxy de Vite.

```dotenv
# frontend/.env
VITE_API_URL=http://localhost:8080/api
```

Solo hace falta definirla si se consume el backend de forma directa. En ese caso, agregá el origen
del frontend a `CORS_ALLOWED_ORIGINS` en el backend. El JWT se guarda en `localStorage`; no se usa
`VITE_OPERARIO_ID` ni se envían identificadores de operario en el body.

## Funcionalidad integrada

- Login por usuario (o email) contra `POST /api/auth/login`; el token se adjunta automáticamente a cada request.
- Dashboard con `GET /api/pedidos/resumen`.
- Alta de pedidos en cuatro pasos, búsqueda de cliente por DNI/CUIT, normalización de dirección y
  predicción de retención antes de confirmar.
- Si la dirección queda ambigua, el pin del mapa se puede arrastrar o reubicar con un clic para
  enviar coordenadas corregidas; el pedido igualmente queda en revisión y el ajuste se audita.
- Bandeja de pedidos con filtros, detalle y acciones según el estado.
- Resolución de revisiones (`APROBAR` / `RECHAZAR`) y cambio de estado, incluido `FALLIDO` con
  reprogramación y recargo.
- Descarga del remito PDF y reimpresión con motivo, mostrando el número de remito y el resultado.
- Descarga del PDF como Blob y lectura del mensaje JSON cuando el backend rechaza la operación.

## Scripts

```powershell
npm run lint      # ESLint
npm run build     # Build de producción en dist/
npm run preview   # Sirve el build local
```

## Estructura

```text
frontend/
├── src/
│   ├── components/   # UI, mapa, wizard y acciones de pedidos
│   ├── context/      # Autenticación y resumen
│   ├── pages/        # Login, panel, nueva carga, pedidos y revisión
│   ├── services/     # Axios y llamadas a la API
│   └── utils/        # Formatters, documentos, tarifas y sesión
├── vite.config.js    # Proxy /api -> backend
└── package.json
```
