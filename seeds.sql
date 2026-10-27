-- Datos de prueba para MandáTodo Logística
-- Ejecutar después de schema.sql, con las tablas vacías.
-- Credenciales de prueba (BCrypt):
--   Admin:             laura.gomez@mandatodo.com / Admin123!
--   Operario activo:   martin.perez@mandatodo.com / Operario123!
--   Operario inactivo: sofia.ramirez@mandatodo.com / Logistica123!

USE mandatodo_db;

SET NAMES utf8mb4;

START TRANSACTION;

-- ============================================================
-- 1. Operarios
-- ============================================================
INSERT INTO operarios (id_operario, nombre, email, password_hash, rol, estado) VALUES
  (1, 'Laura Gómez', 'laura.gomez@mandatodo.com', '$2a$10$gF/QrIJ3IRSkMpJjvEwafu.leWcLhHEqG8vSfUtCiO.CVXNyQS6IK', 'Admin', 'Activo'),
  (2, 'Martín Pérez', 'martin.perez@mandatodo.com', '$2a$10$uRthLFumD7c26HRs.mUVaO2IAYcsx8l7kp26LVwDSNJxGbTwp6Eeu', 'Operario', 'Activo'),
  (3, 'Sofía Ramírez', 'sofia.ramirez@mandatodo.com', '$2a$10$fznvBPXqbssRbNgrI8HfXu598VI5j1DJ/OBTWjlUadNoQ9IRvwXdO', 'Operario', 'Inactivo');

-- ============================================================
-- 2. Clientes
-- Incluye fecha_creacion para datos determinísticos.
-- ============================================================
INSERT INTO clientes (
  id_cliente,
  tipo_documento,
  numero_documento,
  nombre_razon_social,
  telefono,
  email,
  tipo_cliente,
  es_frecuente,
  fecha_creacion
) VALUES
  (1, 'CUIT', '30-71234567-8', 'Distribuidora del Sur S.A.', '11 4567-8901', 'contacto@delsur.com.ar', 'Empresa', TRUE, '2026-10-01 09:00:00'),
  (2, 'CUIT', '30-70987654-3', 'Almacén Central S.R.L.', '11 4789-2345', 'ventas@almacencentral.com.ar', 'Empresa', TRUE, '2026-10-01 09:05:00'),
  (3, 'DNI', '28456789', 'Juan Carlos González', '11 3345-6789', 'juan.gonzalez@gmail.com', 'Particular', TRUE, '2026-10-02 10:00:00'),
  (4, 'DNI', '31987654', 'María Elena Rodríguez', '11 5678-1234', 'maria.rodriguez@hotmail.com', 'Particular', FALSE, '2026-10-03 11:00:00'),
  (5, 'DNI', '26765432', 'Carlos Alberto López', '11 2233-4455', 'carlos.lopez@yahoo.com.ar', 'Particular', FALSE, '2026-10-04 12:00:00'),
  (6, 'CUIT', '33-69876543-9', 'Logística Norte S.A.', '11 4321-8765', 'operaciones@logisticanorte.com.ar', 'Empresa', TRUE, '2026-10-05 13:00:00');

-- ============================================================
-- 3. Pedidos
-- Se completan las columnas históricas del Módulo 1:
-- codigo_pedido, descripcion_paquete, kilometros, precio_base,
-- recargo, precio_total, es_reintento, fecha_programada_entrega,
-- id_operario_ultima_modificacion y fecha_ultima_modificacion.
-- ============================================================
INSERT INTO pedidos (
  id_pedido,
  codigo_pedido,
  id_cliente,
  id_pedido_origen,
  descripcion,
  descripcion_paquete,
  peso_kg,
  kilometros,
  precio_base,
  recargo,
  precio_total,
  monto_envio,
  estado,
  motivo_falla,
  motivo_cancelacion,
  calle,
  altura,
  piso_depto,
  localidad,
  latitud,
  longitud,
  es_reintento,
  fecha_creacion,
  fecha_programada,
  fecha_programada_entrega,
  id_operario_ultima_modificacion,
  fecha_ultima_modificacion
) VALUES
  (1, 'MT-20261020-000001', 3, NULL, 'Encomienda de documentación', 'Encomienda de documentación', 0.50, 12.50, 4500.00, 0.00, 4500.00, 4500.00, 'PENDIENTE', NULL, NULL, 'Av. Corrientes', 1234, '4º B', 'Ciudad Autónoma de Buenos Aires', -34.6037000, -58.3816000, FALSE, '2026-10-20 09:15:00', '2026-10-20', '2026-10-20', 2, '2026-10-20 09:15:00'),
  (2, 'MT-20261019-000002', 2, NULL, 'Reposición de mercadería', 'Reposición de mercadería', 12.50, 15.00, 18500.00, 0.00, 18500.00, 18500.00, 'EN_TRANSITO', NULL, NULL, 'Av. Santa Fe', 2450, '2º A', 'Ciudad Autónoma de Buenos Aires', -34.5955000, -58.3932000, FALSE, '2026-10-19 08:30:00', '2026-10-20', '2026-10-20', 2, '2026-10-19 08:30:00'),
  -- Pedido #3: base 6000.00 + 50% de recargo = 9000.00
  (3, 'MT-20261018-000003', 4, NULL, 'Entrega de medicamentos', 'Entrega de medicamentos', 1.20, 8.00, 6000.00, 3000.00, 9000.00, 9000.00, 'FALLIDO', 'Ausente', NULL, 'Calle 50', 1234, NULL, 'La Plata', -34.9214000, -57.9545000, TRUE, '2026-10-18 14:10:00', '2026-10-21', '2026-10-21', 1, '2026-10-20 18:20:00'),
  -- Pedido #4: base 12000.00 + 50% de recargo = 18000.00
  (4, 'MT-20261018-000004', 1, NULL, 'Devolución de insumos', 'Devolución de insumos', 8.00, 22.00, 12000.00, 6000.00, 18000.00, 18000.00, 'FALLIDO', 'Dirección Inexistente', NULL, 'Ruta 8', 45, NULL, 'San Miguel', -34.5411000, -58.7115000, TRUE, '2026-10-18 11:45:00', '2026-10-22', '2026-10-22', 1, '2026-10-21 17:10:00'),
  (5, 'MT-20261017-000005', 5, NULL, 'Paquete de indumentaria', 'Paquete de indumentaria', 2.30, 10.00, 7500.00, 0.00, 7500.00, 7500.00, 'ENTREGADO', NULL, NULL, 'Av. Mitre', 567, NULL, 'Avellaneda', -34.6625000, -58.3653000, FALSE, '2026-10-17 10:00:00', '2026-10-18', '2026-10-18', 1, '2026-10-18 15:40:00'),
  (6, 'MT-20261016-000006', 6, NULL, 'Documentación comercial', 'Documentación comercial', 0.30, 9.50, 3200.00, 0.00, 3200.00, 3200.00, 'ENTREGADO', NULL, NULL, 'Av. Belgrano', 890, '6º', 'Ciudad Autónoma de Buenos Aires', -34.6131000, -58.3859000, FALSE, '2026-10-16 16:20:00', '2026-10-17', '2026-10-17', 2, '2026-10-17 12:30:00'),
  (7, 'MT-20261020-000007', 1, NULL, 'Carga paletizada', 'Carga paletizada', 25.00, 18.00, 25000.00, 0.00, 25000.00, 25000.00, 'EN_REVISION', NULL, NULL, 'S/N', 0, NULL, 'San Miguel', NULL, NULL, FALSE, '2026-10-20 08:05:00', '2026-10-21', '2026-10-21', 1, '2026-10-20 08:05:00'),
  (8, 'MT-20261019-000008', 2, NULL, 'Reposición de mercadería', 'Reposición de mercadería', 12.50, 15.00, 18500.00, 0.00, 18500.00, 18500.00, 'EN_REVISION', NULL, NULL, 'Av. Santa Fe', 2450, '2º A', 'Ciudad Autónoma de Buenos Aires', -34.5955000, -58.3932000, FALSE, '2026-10-19 09:05:00', '2026-10-20', '2026-10-20', 2, '2026-10-19 09:05:00'),
  (9, 'MT-20261019-000009', 6, NULL, 'Insumos para evento', 'Insumos para evento', 15.00, 25.00, 22000.00, 0.00, 22000.00, 22000.00, 'CANCELADO_EN_TRANSITO', NULL, 'El cliente desistió de la entrega antes de la llegada', 'Av. del Libertador', 5678, NULL, 'Vicente López', -34.5250000, -58.4800000, FALSE, '2026-10-19 13:00:00', '2026-10-20', '2026-10-20', 1, '2026-10-19 15:30:00');

-- ============================================================
-- 4. Revisiones
-- ============================================================
INSERT INTO revisiones (id_revision, id_pedido, motivo_retencion, estado_resolucion) VALUES
  (1, 7, 'Dirección ambigua', 'PENDIENTE'),
  (2, 8, 'Posible duplicado comercial', 'PENDIENTE');

-- ============================================================
-- 5. Auditoría
-- ============================================================
INSERT INTO auditoria (id_auditoria, id_operario, id_pedido, actividad, tipo_evento, fecha_hora) VALUES
  (1, 1, NULL, 'Alta de operario Martín Pérez', 'Alta', '2026-10-15 09:00:00'),
  (2, 1, NULL, 'Alta de operario Sofía Ramírez', 'Alta', '2026-10-15 09:05:00'),
  (3, 2, 6, 'Alta de pedido #6 para Logística Norte S.A.', 'Alta', '2026-10-16 16:20:00'),
  (4, 3, 5, 'Alta de pedido #5 para Carlos Alberto López', 'Alta', '2026-10-17 10:00:00'),
  (5, 1, 6, 'Pedido #6 entregado', 'Cambio de Estado', '2026-10-17 12:30:00'),
  (6, 1, 4, 'Alta de pedido #4 para Distribuidora del Sur S.A.', 'Alta', '2026-10-18 11:45:00'),
  (7, 2, 3, 'Alta de pedido #3 para María Elena Rodríguez', 'Alta', '2026-10-18 14:10:00'),
  (8, 1, 5, 'Pedido #5 entregado', 'Cambio de Estado', '2026-10-18 15:40:00'),
  (9, 2, 2, 'Alta de pedido #2 para Almacén Central S.R.L.', 'Alta', '2026-10-19 08:30:00'),
  (10, 2, 8, 'Alta de pedido #8 retenido por Posible duplicado comercial', 'Alta', '2026-10-19 09:05:00'),
  (11, 2, 2, 'Pedido #2 puesto en tránsito', 'Cambio de Estado', '2026-10-19 10:00:00'),
  (12, 2, 9, 'Alta de pedido #9 para Logística Norte S.A.', 'Alta', '2026-10-19 13:00:00'),
  (13, 1, 9, 'Pedido #9 cancelado en tránsito a pedido del cliente', 'Cambio de Estado', '2026-10-19 15:30:00'),
  (14, 1, 7, 'Alta de pedido #7 retenido por Dirección ambigua', 'Alta', '2026-10-20 08:05:00'),
  (15, 1, 1, 'Alta de pedido #1 para Juan Carlos González', 'Alta', '2026-10-20 09:15:00'),
  (16, 1, 3, 'Pedido #3 marcado como FALLIDO por Ausente; reprogramado con recargo del 50%', 'Cambio de Estado', '2026-10-20 18:20:00'),
  (17, 2, 4, 'Pedido #4 marcado como FALLIDO por Dirección Inexistente; reprogramado con recargo del 50%', 'Cambio de Estado', '2026-10-21 17:10:00');

COMMIT;
