# AGENTS.md - Sistema MandáTodo

## Visión del Sistema
MandáTodo es una plataforma logística orientada a la gestión de envíos y paquetes en tiempo real. 

## Reglas de Negocio Estrictas
1. **Modelos de Envíos:** Ningún paquete o guía de transporte puede ser registrado sin una dirección validada/normalizada.
2. **Restricciones de Identidad:** En base de datos (MySQL), los campos DNI, CUIT, Email y Número de Seguimiento (Tracking) DEBEN contar con restricciones `UNIQUE` explícitas para prevenir duplicados.
3. **Costo de Infraestructura:** El costo operativo de consumo de APIs de geolocalización/mapas debe ser $0 USD. Se utiliza únicamente OpenStreetMap / Nominatim. Está prohibido el uso de claves o SDKs pagos (Google Maps API, Mapbox).
4. **Respeto de Rate Limit:** La API de Nominatim exige una cabecera `User-Agent` identificable y un límite de requests máximo de 1 petición por segundo desde el backend.

## Arquitectura de Software y Estándares

### Backend (Spring Boot / Java 21)
- **Tipado Estricto:** Evitar el uso de `Object` o genéricos ambiguos. Todas las entidades, DTOs y Servicios deben estar fuertemente tipados.
- **Arquitectura en Capas:** Controladores (REST) -> Servicios (Reglas) -> Repositorios (JPA) -> Base de Datos.
- **Inmutabilidad & Lombok:** Uso preferencial de DTOs con `@Value` o Java Records para respuestas API.

### Frontend (React + Vite)
- **Componentes Funcionales:** Uso exclusivo de hooks (`useState`, `useEffect`, `useCallback`, `useMemo`).
- **Respeto al Virtual DOM:** Queda prohibido manipular el DOM directamente (`document.getElementById`). Toda actualización del mapa se realiza mediante props y estado de React.
- **Rendimiento de Leaflet:** Utilizar los hooks provistos por `react-leaflet` (`useMap`, `useMapEvents`) para sincronizar marcadores.