# Módulo 1: Núcleo Operativo y Normalización Geográfica

Estas instrucciones aplican al código de esta función y complementan el `AGENTS.md` raíz.

## Alcance

- Formulario inteligente que interpreta una dirección y separa calle, altura, piso/departamento y localidad.
- Geocodificación y mapa interactivo para confirmar la ubicación mediante un pin.
- Libreta digital con detección de clientes duplicados por CUIT o DNI.
- La categoría (`Particular` o `Empresa`) y la condición de cliente frecuente son atributos independientes.
- Reglas de seguridad y bandeja En Revisión para retener pedidos que requieren validación antes de asignarlos.
- Flujo de estados con motivo de falla obligatorio, incluidos Ausente, Dirección Inexistente y Dirección Incompleta.
- Remito físico con detalle del paquete, kilos, kilómetros, precio a cobrar y espacio para firma o comprobante.

## Reglas específicas

- Mantener coherentes el texto original de la dirección, sus partes normalizadas y las coordenadas confirmadas.
- No eludir alertas de seguridad ni asignar directamente pedidos que deban pasar por revisión.
- Las correcciones de dirección y pin deben persistirse mediante una mutación explícita del estado compartido, no solo en estado visual local.
- La generación o regeneración del remito en PDF es un requisito evolutivo; comunicar claramente cualquier simulación local.
- No confundir `cancelado_transito` con el futuro estado `cancelado`.
