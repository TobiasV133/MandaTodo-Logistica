package com.mandatodo.logistica.operario.dto;

import com.mandatodo.logistica.operario.EstadoOperario;
import com.mandatodo.logistica.operario.RolOperario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioDTO(
    @NotBlank(message = "El username es obligatorio")
    @Size(min = 4, max = 30, message = "El username debe tener entre 4 y 30 caracteres")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "El username solo admite letras, números, puntos, guiones y guiones bajos"
    )
    String username,

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
    RolOperario rol,

    @NotNull(message = "El estado es obligatorio")
    EstadoOperario estado
) {
}
