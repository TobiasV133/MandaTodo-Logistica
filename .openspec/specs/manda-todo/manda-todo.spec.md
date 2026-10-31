# Specification: MandáTodo Logística

## Purpose

Definir el stack oficial, el modelo de datos, las reglas de negocio y el contrato de API de MandáTodo Logística como fuente de verdad única del proyecto.

## Requirements

### Requirement: Stack Tecnológico Oficial

El sistema SHALL implementarse con el stack definido en la sección 1.

#### Scenario: Cumplimiento del stack

- **WHEN** se implemente cualquier módulo del sistema
- **THEN** se debe utilizar el stack tecnológico oficial

### Requirement: Modelo de Datos Canónico

El sistema SHALL persistir las entidades con la nomenclatura y los tipos definidos en la sección 2.

#### Scenario: Persistencia del modelo

- **WHEN** se cree o consulte una entidad
- **THEN** se deben respetar los nombres y tipos del modelo de datos

### Requirement: Reglas de Negocio Estrictas

El sistema SHALL aplicar las reglas de geolocalización, duplicados, reintentos y auditoría definidas en la sección 3.

#### Scenario: Evaluación de un pedido

- **WHEN** se registre o actualice un pedido
- **THEN** se deben evaluar las reglas de negocio correspondientes

### Requirement: Contrato de API REST

El sistema SHALL exponer los 21 endpoints definidos en la sección 4.

#### Scenario: Consumo de la API

- **WHEN** un cliente consuma la API
- **THEN** debe encontrar los endpoints con las rutas y operaciones especificadas

## 1. Stack Tecnológico Oficial
- **Frontend:** React + Bootstrap + React-Leaflet + Lucide Icons
- **Backend:** Java 21 + Spring Boot 4.0.8 + Spring Data JPA / Hibernate (Jakarta Persistence)
- **Validación:** Jakarta Bean Validation (`jakarta.validation`) en los DTOs de entrada
- **Seguridad:** Spring Security Crypto (`BCryptPasswordEncoder`) para el hash de contraseñas y JWT para las sesiones
- **Database:** MySQL 8.x
- **Geocoding API:** OpenStreetMap / Nominatim API

## 2. Nomenclatura del Modelo de Datos (DER MySQL)

export type TipoCliente = 'Particular' | 'Empresa';
export type EstadoPedido = 'PENDIENTE' | 'EN_REVISION' | 'EN_TRANSITO' | 'CANCELADO_EN_TRANSITO' | 'ENTREGADO' | 'FALLIDO';
export type MotivoFalla = 'Ausente' | 'Dirección Inexistente' | 'Dirección Incompleta' | 'Pedido Duplicado';
export type RolOperario = 'Admin' | 'Operario';

export interface Cliente {
  id_cliente: number;
  tipo_documento: string;
  numero_documento: string; // UNIQUE en DB
  nombre_razon_social: string;
  telefono: string;
  email: string;
  tipo_cliente: TipoCliente;
  es_frecuente: boolean;
}

export interface Pedido {
  id_pedido: number;
  id_cliente: number;
  descripcion: string;
  peso_kg: number;
  kilometros?: number; // distancia del recorrido: tarifa y remito
  monto_envio: number;
  estado: EstadoPedido;
  motivo_falla?: MotivoFalla;
  calle: string;
  altura: number;
  piso_depto?: string;
  localidad: string;
  latitud: number;
  longitud: number;
  fecha_creacion: string;
}

export interface Revision {
  id_revision: number;
  id_pedido: number;
  motivo_retencion: 'Dirección ambigua' | 'Posible duplicado de local';
  estado_resolucion: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';
}

export interface Operario {
  id_operario: number;
  username: string; // UNIQUE, login
  nombre: string;
  email: string; // UNIQUE
  rol: RolOperario;
  estado: 'Activo' | 'Inactivo';
}

export interface AuditLog {
  id_auditoria: number;
  id_operario: number;
  id_pedido?: number;
  actividad: string;
  tipo_evento: 'Alta' | 'Edición' | 'Reimpresión' | 'Cambio de Estado';
  fecha_hora: string;
}

export interface TarifaConfig {
  id_tarifa: 1; // fila única
  tarifa_base: number;
  tarifa_media: number;
  recargo_por_km: number;
  recargo_por_kg: number;
  limite_peso_base: number;
  limite_km_base: number;
  limite_peso_medio: number;
  limite_km_medio: number;
  max_peso_kg: number;
  max_kilometros: number;
  id_operario_ultima_modificacion?: number;
  fecha_ultima_modificacion?: string;
}

## 3. Reglas de Negocio Estrictas
1. **Geolocalización & Normalización:**
   - La API recibe `calle`, `altura`, `localidad` y consulta Nominatim.
   - Si la altura es 0, la calle es "S/N" o el resultado no es confiable, se genera un registro en `revisiones` con motivo `"Dirección ambigua"` y el pedido pasa a `EN_REVISION`.
2. **Control de Duplicados Comerciales:**
   - Si un cliente tipo `'Empresa'` registra un pedido con la misma fecha programada que otro ya registrado, pasa a `EN_REVISION` con motivo `"Posible duplicado de local"` (búsqueda por el mismo cliente y la misma fecha programada, asumiendo el mismo destino).
   - Si es `'Particular'`, el pedido pasa a `PENDIENTE` directamente (el duplicado se permite).
3. **Reintento de Entregas Fallidas:**
   - Al marcar estado `FALLIDO`, se exige un `motivo_falla`. Se reprograma automáticamente para el día siguiente y se aplica un **50% de recargo** sobre el `monto_envio`.
4. **Auditoría Global Inalterable:**
   - Toda acción de Alta, Edición, Reimpresión o Cambio de Estado registra el operario responsable en la tabla `auditoria`.
5. **Tarifario Configurable:**
   - El tarifario escalonado (importes base, medio, recargos por km/kg, límites de cada nivel y capacidad máxima) se persiste en la fila única de `tarifa_config`.
   - Solo un operario con rol `Admin` puede modificarlo. Los operarios con rol `Operario` reciben `403 Forbidden`.
   - Cada modificación se registra en `auditoria` como evento de `Edición` sin pedido asociado.
   - Los pedidos nuevos se tarifan con los valores vigentes al momento del alta.

## 4. Contrato Completo de API REST (21 Endpoints)

1. Autenticación y Sesión:
   - `POST /api/auth/login` -> Iniciar sesión de operario

2. Carga Inteligente & Normalización Geográfica:
   - `POST /api/geo/normalizar` -> Normalización y geocodificación via Nominatim
   - `POST /api/pedidos` -> Crear nuevo pedido (`PENDIENTE` o `EN_REVISION`)

3. Libreta Digital de Clientes:
   - `GET /api/clientes/buscar?doc=...` -> Autocompletado por DNI/CUIT
   - `GET /api/clientes` -> Listar todos los clientes
   - `PUT /api/clientes/{id}` -> Editar datos de un cliente

4. Gestión de Pedidos y Circuitos de Estados:
   - `GET /api/pedidos` -> Listar pedidos (soporta filtro `?estado=...`)
   - `GET /api/pedidos/{id}` -> Obtener el detalle de un pedido
   - `PATCH /api/pedidos/{id}/estado` -> Actualizar estado (`FALLIDO` reprograma + 50% recargo)
   - `POST /api/pedidos/{id}/cancelar-transito` -> Registrar cancelación en tránsito con motivo

5. Bandeja de Seguridad "En Revisión":
   - `GET /api/pedidos/en-revision` -> Listar pedidos retenidos
   - `PUT /api/pedidos/{id}/revision` -> Resolver pedido retenido (`APROBAR` o `RECHAZAR`)

6. Generación de Remitos PDF:
   - `POST /api/remitos/{idPedido}/imprimir` -> Generar/reimprimir PDF de remito con motivo

7. ABM de Usuarios / Operarios:
   - `GET /api/usuarios` -> Listar todos los operarios
   - `POST /api/usuarios` -> Alta de nuevo operario (`Admin` o `Operario`)
   - `PUT /api/usuarios/{id}` -> Editar datos o estado (`Activo` / `Inactivo`)

8. Sistema de Auditoría Inalterable (Audit Log):
   - `POST /api/auditoria` -> Registrar evento de auditoría
   - `GET /api/auditoria` -> Consultar listado general de logs de auditoría
   - `GET /api/auditoria?fecha=...&operario=...` -> Consultar auditoría con filtros

9. Configuración del Tarifario:
   - `GET /api/tarifas` -> Obtener los valores vigentes (cualquier operario autenticado)
   - `PUT /api/tarifas` -> Actualizar importes y límites (solo rol `Admin`; `403` para operarios)

---
