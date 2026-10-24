package com.mandatodo.logistica.remito;

import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.pedido.Pedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.util.StringUtils;

/**
 * Fotografía inmutable de todo lo que se imprime en el remito. Separar estos datos de las entidades
 * permite generar el PDF sin sesión de JPA abierta y sin acoplar el documento al modelo de
 * persistencia.
 */
public record DatosRemito(
    String numero,
    LocalDateTime fechaEmision,
    LocalDate fechaProgramada,
    String clienteNombre,
    String clienteDocumento,
    String clienteTelefono,
    String clienteEmail,
    String direccion,
    String localidad,
    String estado,
    String descripcion,
    BigDecimal pesoKg,
    BigDecimal kilometros,
    BigDecimal montoEnvio,
    boolean reimpresion,
    String motivoReimpresion,
    String operario,
    DatosEmpresa empresa
) {

    public static DatosRemito desde(
        Pedido pedido,
        Operario operario,
        BigDecimal kilometros,
        boolean reimpresion,
        String motivoReimpresion,
        LocalDateTime fechaEmision,
        DatosEmpresa empresa
    ) {
        return new DatosRemito(
            numeroDe(pedido.getId()),
            fechaEmision,
            pedido.getFechaProgramada(),
            pedido.getCliente().getNombreRazonSocial(),
            pedido.getCliente().getTipoDocumento() + " " + pedido.getCliente().getNumeroDocumento(),
            pedido.getCliente().getTelefono(),
            pedido.getCliente().getEmail(),
            direccionCompleta(pedido),
            pedido.getLocalidad(),
            pedido.getEstado().name(),
            pedido.getDescripcion(),
            pedido.getPesoKg(),
            kilometros,
            pedido.getMontoEnvio(),
            reimpresion,
            motivoReimpresion,
            operario.getNombre(),
            empresa
        );
    }

    public static String numeroDe(Long idPedido) {
        return "R-%08d".formatted(idPedido);
    }

    private static String direccionCompleta(Pedido pedido) {
        StringBuilder direccion = new StringBuilder(pedido.getCalle());
        if (pedido.getAltura() != null && pedido.getAltura() > 0) {
            direccion.append(' ').append(pedido.getAltura());
        }
        if (StringUtils.hasText(pedido.getPisoDepto())) {
            direccion.append(", ").append(pedido.getPisoDepto().trim());
        }
        return direccion.toString();
    }

    public record DatosEmpresa(
        String nombre,
        String subtitulo,
        String direccion,
        String telefono,
        String cuit
    ) {
        public static DatosEmpresa desde(EmpresaRemitoProperties propiedades) {
            return new DatosEmpresa(
                propiedades.getNombre(),
                propiedades.getSubtitulo(),
                propiedades.getDireccion(),
                propiedades.getTelefono(),
                propiedades.getCuit()
            );
        }
    }
}
