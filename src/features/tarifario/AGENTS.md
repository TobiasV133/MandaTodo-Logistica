# Módulo 4: Tarifario, Cálculo de Precios y Recargos

Estas instrucciones aplican al código de esta función y complementan el `AGENTS.md` raíz.

## Alcance

- Matriz configurable de precios por zona geográfica (CABA, AMBA y Provincia), kilos y kilómetros.
- Sugerencia automática del precio del envío durante la carga según peso y distancia.
- Recargos por reprogramación, incluido el caso de entrega fallida por Cliente Ausente.

## Reglas específicas

- Mantener visible la diferencia entre precio sugerido y monto final cuando el operador pueda editarlo.
- Una reprogramación de un pedido fallido se agenda para la siguiente jornada y aplica un recargo del 50% del valor del envío.
- Para una reprogramación normal, calcular el recargo según la fecha elegida, con un máximo del 50% del valor del envío; permitir editar el monto calculado.
- Contemplar los estados futuros `cancelado` y `reprogramado`, diferenciando `cancelado` de `cancelado_transito`.
- Evitar aplicar más de una vez el mismo recargo y mostrar claramente su motivo y valor.
- Mantener las reglas de cálculo en una fuente coherente; no duplicar fórmulas entre pantallas.
- Si los datos o tarifas son simulados, no presentarlos como cálculo respaldado por servicios reales.
