package com.mandatodo.backend.dto;

import com.mandatodo.backend.model.EstadoOperario;
import com.mandatodo.backend.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para actualizar un operario.
 *
 * <p>La contraseña es opcional: si llega {@code null} o vacía se conserva el
 * hash actual. Si llega un valor, debe tener entre 8 y 72 caracteres y se
 * hashea antes de persistirlo.</p>
 */
public record ActualizarUsuarioDTO(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email,

        @Pattern(
                regexp = "^$|^.{8,72}$",
                message = "La contraseña debe tener entre 8 y 72 caracteres"
        )
        String password,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        @NotNull(message = "El estado es obligatorio")
        EstadoOperario estado
) {
}
