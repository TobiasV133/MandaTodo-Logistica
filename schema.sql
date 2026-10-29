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

CREATE TABLE clientes (
  id_cliente INT UNSIGNED NOT NULL AUTO_INCREMENT,
  tipo_documento VARCHAR(20) NOT NULL,
  numero_documento VARCHAR(30) NOT NULL,
  nombre_razon_social VARCHAR(150) NOT NULL,
  telefono VARCHAR(30) NOT NULL,
  email VARCHAR(150) NOT NULL,
  tipo_cliente ENUM('Particular', 'Empresa') NOT NULL,
  es_frecuente BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id_cliente),
  UNIQUE KEY uq_clientes_numero_documento (numero_documento),
  KEY idx_clientes_tipo_cliente (tipo_cliente),
  KEY idx_clientes_nombre_razon_social (nombre_razon_social)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operarios (
  id_operario INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(150) NOT NULL,
  email VARCHAR(150) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  rol ENUM('Admin', 'Operario') NOT NULL DEFAULT 'Operario',
  estado ENUM('Activo', 'Inactivo') NOT NULL DEFAULT 'Activo',
  PRIMARY KEY (id_operario),
  UNIQUE KEY uq_operarios_email (email),
  KEY idx_operarios_rol (rol),
  KEY idx_operarios_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pedidos (
  id_pedido INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_cliente INT UNSIGNED NOT NULL,
  descripcion TEXT NOT NULL,
  peso_kg DECIMAL(10, 2) NOT NULL,
  kilometros DECIMAL(8, 2) NULL,
  monto_envio DECIMAL(12, 2) NOT NULL,
  estado ENUM('PENDIENTE', 'EN_REVISION', 'EN_TRANSITO', 'CANCELADO_EN_TRANSITO', 'ENTREGADO', 'FALLIDO') NOT NULL DEFAULT 'PENDIENTE',
  motivo_falla ENUM('Ausente', 'Dirección Inexistente', 'Dirección Incompleta') NULL,
  motivo_cancelacion VARCHAR(255) NULL,
  calle VARCHAR(200) NOT NULL,
  altura INT UNSIGNED NOT NULL,
  piso_depto VARCHAR(50) NULL,
  localidad VARCHAR(150) NOT NULL,
  latitud DECIMAL(10, 7) NULL,
  longitud DECIMAL(10, 7) NULL,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_programada DATE NOT NULL DEFAULT (CURRENT_DATE),
  PRIMARY KEY (id_pedido),
  KEY idx_pedidos_id_cliente (id_cliente),
  KEY idx_pedidos_estado (estado),
  KEY idx_pedidos_fecha_creacion (fecha_creacion),
  KEY idx_pedidos_fecha_programada (fecha_programada),
  CONSTRAINT fk_pedidos_cliente
    FOREIGN KEY (id_cliente) REFERENCES clientes (id_cliente)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_pedidos_peso_kg CHECK (peso_kg >= 0),
  CONSTRAINT chk_pedidos_kilometros CHECK (kilometros IS NULL OR kilometros >= 0),
  CONSTRAINT chk_pedidos_monto_envio CHECK (monto_envio >= 0),
  CONSTRAINT chk_pedidos_latitud CHECK (latitud IS NULL OR latitud BETWEEN -90 AND 90),
  CONSTRAINT chk_pedidos_longitud CHECK (longitud IS NULL OR longitud BETWEEN -180 AND 180),
  CONSTRAINT chk_pedidos_motivo_falla CHECK (estado <> 'FALLIDO' OR motivo_falla IS NOT NULL),
  CONSTRAINT chk_pedidos_motivo_cancelacion CHECK (estado <> 'CANCELADO_EN_TRANSITO' OR motivo_cancelacion IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE revisiones (
  id_revision INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_pedido INT UNSIGNED NOT NULL,
  motivo_retencion ENUM('Dirección ambigua', 'Posible duplicado comercial') NOT NULL,
  estado_resolucion ENUM('PENDIENTE', 'APROBADO', 'RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
  PRIMARY KEY (id_revision),
  KEY idx_revisiones_id_pedido (id_pedido),
  KEY idx_revisiones_estado_resolucion (estado_resolucion),
  CONSTRAINT fk_revisiones_pedido
    FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auditoria (
  id_auditoria BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_operario INT UNSIGNED NOT NULL,
  id_pedido INT UNSIGNED NULL,
  actividad VARCHAR(500) NOT NULL,
  tipo_evento ENUM('Alta', 'Edición', 'Reimpresión', 'Cambio de Estado') NOT NULL,
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
