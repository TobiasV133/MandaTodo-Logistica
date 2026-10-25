package com.mandatodo.backend.config;

import com.mandatodo.backend.model.Operario;
import com.mandatodo.backend.model.Rol;
import com.mandatodo.backend.repository.OperarioRepository;
import com.mandatodo.backend.security.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea los usuarios iniciales únicamente cuando la tabla operarios está vacía.
 * Las credenciales de desarrollo se documentan en el README del backend.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final OperarioRepository operarioRepository;
    private final PasswordService passwordService;

    public DataInitializer(OperarioRepository operarioRepository, PasswordService passwordService) {
        this.operarioRepository = operarioRepository;
        this.passwordService = passwordService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (operarioRepository.count() > 0) {
            return;
        }

        crearOperario(
                "admin",
                "Administrador MandáTodo",
                "admin@mandatodo.local",
                "Admin123!",
                Rol.ADMINISTRADOR
        );
        crearOperario(
                "operario",
                "Operario MandáTodo",
                "operario@mandatodo.local",
                "Operario123!",
                Rol.OPERARIO
        );

        log.warn("Se crearon usuarios iniciales de desarrollo. Cambiá sus contraseñas antes de producción.");
    }

    private void crearOperario(String username, String nombre, String email, String password, Rol rol) {
        Operario operario = new Operario();
        operario.setUsername(username);
        operario.setNombreCompleto(nombre);
        operario.setEmail(email);
        operario.setPasswordHash(passwordService.hash(password));
        operario.setRol(rol);
        operario.setActivo(true);
        operarioRepository.save(operario);
    }
}
