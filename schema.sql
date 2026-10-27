SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS mandatodo_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE mandatodo_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS auditoria;
DROP TABLE IF EXISTS revisiones;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS operarios;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. Clientes
-- Alineado a com.mandatodo.backend.model.Cliente
-- ============================================================
CREATE TABLE clientes (
  id_cliente INT NOT NULL AUTO_INCREMENT,
  tipo_documento ENUM('DNI', 'CUIT') NOT NULL,
  numero_documento VARCHAR(20) NOT NULL,
  nombre_razon_social VARCHAR(180) NOT NULL,
  telefono VARCHAR(40) NULL,
  email VARCHAR(180) NOT NULL,
  tipo_cliente ENUM('Particular', 'Empresa') NOT NULL,
  es_frecuente BOOLEAN NOT NULL DEFAULT FALSE,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_cliente),
  UNIQUE KEY uk_clientes_numero_documento (numero_documento),
  UNIQUE KEY uk_clientes_email (email),
  KEY idx_clientes_tipo_cliente (tipo_cliente),
  KEY idx_clientes_nombre_razon_social (nombre_razon_social)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. Operarios
-- Alineado a com.mandatodo.backend.model.Operario
-- ============================================================
CREATE TABLE operarios (
  id_operario INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(150) NOT NULL,
  email VARCHAR(180) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  rol ENUM('Admin', 'Operario') NOT NULL DEFAULT 'Operario',
  estado ENUM('Activo', 'Inactivo') NOT NULL DEFAULT 'Activo',
  PRIMARY KEY (id_operario),
  UNIQUE KEY uq_operarios_email (email),
  KEY idx_operarios_rol (rol),
  KEY idx_operarios_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. Pedidos
-- Alineado a com.mandatodo.backend.model.Pedido
-- Incluye columnas históricas del Módulo 1 y campos canónicos.
-- ============================================================
CREATE TABLE pedidos (
  id_pedido INT NOT NULL AUTO_INCREMENT,
  codigo_pedido VARCHAR(30) NOT NULL,
  id_cliente INT NOT NULL,
  id_pedido_origen INT NULL,
  descripcion TEXT NULL,
  descripcion_paquete VARCHAR(300) NOT NULL,
  peso_kg DECIMAL(10, 3) NOT NULL,
  kilometros DECIMAL(10, 2) NOT NULL,
  precio_base DECIMAL(12, 2) NOT NULL,
  recargo DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
  precio_total DECIMAL(12, 2) NOT NULL,
  monto_envio DECIMAL(12, 2) NULL,
  estado ENUM(
    'EN_REVISION',
    'PENDIENTE',
    'EN_TRANSITO',
    'CANCELADO_EN_TRANSITO',
    'ENTREGADO',
    'FALLIDO'
  ) NOT NULL DEFAULT 'PENDIENTE',
  motivo_falla ENUM(
    'Ausente',
    'Dirección Inexistente',
    'Dirección Incompleta'
  ) NULL,
  motivo_cancelacion VARCHAR(255) NULL,
  calle VARCHAR(150) NOT NULL,
  altura INT NULL,
  piso_depto VARCHAR(40) NULL,
  localidad VARCHAR(120) NOT NULL,
  latitud DECIMAL(10, 7) NULL,
  longitud DECIMAL(10, 7) NULL,
  es_reintento BOOLEAN NOT NULL DEFAULT FALSE,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_programada DATE NULL,
  fecha_programada_entrega DATE NULL,
  id_operario_ultima_modificacion INT NULL,
  fecha_ultima_modificacion DATETIME NULL,
  PRIMARY KEY (id_pedido),
  UNIQUE KEY uk_pedidos_codigo_pedido (codigo_pedido),
  KEY idx_pedidos_id_cliente (id_cliente),
  KEY idx_pedidos_estado (estado),
  KEY idx_pedidos_fecha_creacion (fecha_creacion),
  KEY idx_pedidos_fecha_programada (fecha_programada),
  KEY idx_pedidos_fecha_programada_entrega (fecha_programada_entrega),
  KEY idx_pedidos_id_operario_ultima_modificacion (id_operario_ultima_modificacion),
  CONSTRAINT fk_pedidos_cliente
    FOREIGN KEY (id_cliente) REFERENCES clientes (id_cliente)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_pedidos_peso_kg CHECK (peso_kg >= 0),
  CONSTRAINT chk_pedidos_kilometros CHECK (kilometros >= 0),
  CONSTRAINT chk_pedidos_precio_base CHECK (precio_base >= 0),
  CONSTRAINT chk_pedidos_recargo CHECK (recargo >= 0),
  CONSTRAINT chk_pedidos_precio_total CHECK (precio_total >= 0),
  CONSTRAINT chk_pedidos_monto_envio CHECK (monto_envio IS NULL OR monto_envio >= 0),
  CONSTRAINT chk_pedidos_altura CHECK (altura IS NULL OR altura >= 0),
  CONSTRAINT chk_pedidos_latitud CHECK (latitud IS NULL OR latitud BETWEEN -90 AND 90),
  CONSTRAINT chk_pedidos_longitud CHECK (longitud IS NULL OR longitud BETWEEN -180 AND 180),
  CONSTRAINT chk_pedidos_motivo_falla CHECK (estado <> 'FALLIDO' OR motivo_falla IS NOT NULL),
  CONSTRAINT chk_pedidos_motivo_cancelacion CHECK (estado <> 'CANCELADO_EN_TRANSITO' OR motivo_cancelacion IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. Revisiones
-- Alineado a com.mandatodo.backend.model.Revision
-- ============================================================
CREATE TABLE revisiones (
  id_revision INT NOT NULL AUTO_INCREMENT,
  id_pedido INT NOT NULL,
  motivo_retencion ENUM(
    'Dirección ambigua',
    'Posible duplicado comercial'
  ) NOT NULL,
  estado_resolucion ENUM('PENDIENTE', 'APROBADO', 'RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
  PRIMARY KEY (id_revision),
  KEY idx_revisiones_id_pedido (id_pedido),
  KEY idx_revisiones_estado_resolucion (estado_resolucion),
  CONSTRAINT fk_revisiones_pedido
    FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. Auditoría
-- Alineado a com.mandatodo.backend.model.Auditoria
-- ============================================================
CREATE TABLE auditoria (
  id_auditoria BIGINT NOT NULL AUTO_INCREMENT,
  id_operario INT NOT NULL,
  id_pedido INT NULL,
  actividad VARCHAR(500) NOT NULL,
  tipo_evento ENUM(
    'Alta',
    'Edición',
    'Reimpresión',
    'Cambio de Estado'
  ) NOT NULL,
  fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_auditoria),
  KEY idx_auditoria_id_operario (id_operario),
  KEY idx_auditoria_id_pedido (id_pedido),
  KEY idx_auditoria_fecha_hora (fecha_hora),
  KEY idx_auditoria_fecha_operario (fecha_hora, id_operario),
  CONSTRAINT fk_auditoria_operario
    FOREIGN KEY (id_operario) REFERENCES operarios (id_operario)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_auditoria_pedido
    FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TRIGGER trg_auditoria_no_update
BEFORE UPDATE ON auditoria
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La tabla auditoria es inalterable';

CREATE TRIGGER trg_auditoria_no_delete
BEFORE DELETE ON auditoria
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La tabla auditoria es inalterable';
