package com.mandatodo.logistica.remito;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos del emisor que se imprimen en el encabezado del remito. Se configuran en
 * {@code mandatodo.remito.empresa} para no fijar datos comerciales en el código.
 */
@ConfigurationProperties(prefix = "mandatodo.remito.empresa")
public class EmpresaRemitoProperties {

    private String nombre = "MandáTodo";
    private String subtitulo = "Logística y distribución";
    private String direccion = "";
    private String telefono = "";
    private String cuit = "";

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSubtitulo() {
        return subtitulo;
    }

    public void setSubtitulo(String subtitulo) {
        this.subtitulo = subtitulo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }
}
