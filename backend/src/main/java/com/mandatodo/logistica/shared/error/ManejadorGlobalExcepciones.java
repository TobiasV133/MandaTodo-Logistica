package com.mandatodo.logistica.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    private final Clock clock;

    public ManejadorGlobalExcepciones(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(
        RecursoNoEncontradoException excepcion,
        HttpServletRequest request
    ) {
        return respuesta(HttpStatus.NOT_FOUND, excepcion.getMessage(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaDeNegocio(
        ReglaNegocioException excepcion,
        HttpServletRequest request
    ) {
        return respuesta(HttpStatus.CONFLICT, excepcion.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(
        MethodArgumentNotValidException excepcion,
        HttpServletRequest request
    ) {
        String mensaje = excepcion.getBindingResult().getFieldErrors().stream()
            .map(this::formatearError)
            .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> restriccion(
        ConstraintViolationException excepcion,
        HttpServletRequest request
    ) {
        return respuesta(HttpStatus.BAD_REQUEST, excepcion.getMessage(), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> solicitudInvalida(
        Exception excepcion,
        HttpServletRequest request
    ) {
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida: " + excepcion.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(
        DataIntegrityViolationException excepcion,
        HttpServletRequest request
    ) {
        log.warn("Violación de integridad de datos", excepcion);
        return respuesta(
            HttpStatus.CONFLICT,
            "No se pudo guardar la operación por una restricción de integridad de datos.",
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorGeneral(
        Exception excepcion,
        HttpServletRequest request
    ) {
        log.error("Error no controlado en {}", request.getRequestURI(), excepcion);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.", request);
    }

    private String formatearError(FieldError error) {
        String detalle = error.getDefaultMessage() == null ? "valor inválido" : error.getDefaultMessage();
        return error.getField() + ": " + detalle;
    }

    private ResponseEntity<ErrorResponse> respuesta(
        HttpStatus estado,
        String mensaje,
        HttpServletRequest request
    ) {
        ErrorResponse cuerpo = new ErrorResponse(
            LocalDateTime.now(clock),
            estado.value(),
            estado.getReasonPhrase(),
            mensaje,
            request.getRequestURI()
        );
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
