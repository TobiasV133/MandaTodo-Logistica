# Módulo 2: Carga Masiva y Gestión de Clientes Frecuentes

Estas instrucciones aplican al código de esta función y complementan el `AGENTS.md` raíz.

## Alcance

- Importación de archivos Excel y CSV para clientes recurrentes.
- Validación de datos y nivel de confianza antes de confirmar la carga.
- Impedir la importación de clientes que no sean frecuentes.
- Impedir la confirmación de registros que deban quedar En Revisión.

## Reglas específicas

- Validar el archivo y sus registros antes de modificar el estado compartido; los registros inválidos no deben producir altas parciales silenciosas.
- Para clientes frecuentes, contemplar un nivel intermedio de confianza entre aprobación directa y En Revisión. Mostrar los casos intermedios y pedir validación explícita antes de confirmar la carga.
- Reutilizar la libreta y las reglas de identidad del Módulo 1 para evitar duplicar clientes por CUIT o DNI.
- Informar errores por fila de manera clara, en español rioplatense, y conservar el estado de carga, vacío y confirmación.
- La importación permanece simulada en memoria mientras no exista backend conectado.
