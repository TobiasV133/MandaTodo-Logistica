# Módulo 5: Dashboard de Métricas e Historial

Estas instrucciones aplican al panel del dueño y complementan el `AGENTS.md` raíz.

## Alcance

- Resumen con selector de período semanal o mensual.
- Reportes por rango de fechas de facturación total, margen de ganancias y volumen de envíos por zona geográfica.
- El margen se calcula como tarifa cobrada menos costo directo del flete por kilómetro recorrido.
- Historial de actividad y modificaciones de pedidos.
- Gestión del ciclo de vida de usuarios con rol Operario.

## Reglas específicas

- Aplicar el rango elegido de manera coherente a todas las métricas y aclarar el período mostrado.
- Desglosar el volumen por zona geográfica y mantener la definición del margen visible y consistente.
- Registrar quién y cuándo modifica cada pedido para poder mostrar una referencia como «Última modificación por Tobias Varela».
- La baja de un Operario no debe borrar su historial de actividad.
- Las modificaciones de Operarios deben requerir una clave única y cambiante; no depender de una clave fija compartida.
- Los datos del prototipo son simulados; no describirlos como informes financieros o auditoría persistente de backend.
- Preservar estados de carga, error y ausencia de datos, y comunicar filtros sin resultados.
