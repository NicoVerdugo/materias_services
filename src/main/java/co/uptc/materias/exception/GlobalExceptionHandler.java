package co.uptc.materias.exception;

import co.uptc.materias.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import software.amazon.awssdk.core.exception.SdkException;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la API.
 * <p>
 * Garantiza respuestas de error consistentes sin exponer
 * información interna del sistema (stack traces, SQL, rutas del servidor).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Atributo de la petición donde se deja el {@code errorType}; lo lee el
     * middleware de logs del datalake.
     */
    public static final String ERROR_TYPE_ATTRIBUTE = "errorType";

    public static final String RUTA_NO_EXISTE = "RutaNoExiste";
    public static final String METODO_NO_PERMITIDO = "MetodoNoPermitido";
    public static final String VALIDACION_FALLIDA = "ValidacionFallida";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), ex.getErrorType(), request);
    }

    /**
     * Conflictos de cupo: CupoAgotado (reservar) y CupoYaCompleto (liberar).
     */
    @ExceptionHandler(CupoAgotadoException.class)
    public ResponseEntity<ErrorResponse> handleCupoAgotado(
            CupoAgotadoException ex, HttpServletRequest request) {
        log.warn("Conflicto de cupo [{}]: {}", ex.getErrorType(), ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), ex.getErrorType(), request);
    }

    /**
     * Reglas de negocio: CursoInactivo y CupoMenorQueInscritos.
     */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaNegocio(
            ReglaNegocioException ex, HttpServletRequest request) {
        log.warn("Regla de negocio [{}]: {}", ex.getErrorType(), ex.getMessage());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", ex.getMessage(),
                ex.getErrorType(), request);
    }

    /**
     * Ruta sin endpoint. Desde Spring 6.1 una ruta no mapeada lanza
     * {@link NoResourceFoundException}; sin este handler caía en el genérico y respondía 500.
     * {@link NoHandlerFoundException} aplica solo si se desactiva el manejo de recursos
     * estáticos (spring.web.resources.add-mappings=false); se cubre igual.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleRutaNoExiste(
            Exception ex, HttpServletRequest request) {
        log.warn("Ruta no encontrada: {} {}", request.getMethod(), request.getRequestURI());
        return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "No existe la ruta " + request.getMethod() + " " + request.getRequestURI(),
                RUTA_NO_EXISTE, request);
    }

    /**
     * Método HTTP no soportado por la ruta (antes caía en el genérico: 500).
     * Responde 405 con el encabezado Allow con los métodos que sí acepta.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodoNoPermitido(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Método no permitido: {} {}", request.getMethod(), request.getRequestURI());
        ResponseEntity<ErrorResponse> error = buildError(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "El método " + request.getMethod() + " no está permitido en " + request.getRequestURI(),
                METODO_NO_PERMITIDO, request);
        Set<HttpMethod> permitidos = ex.getSupportedHttpMethods();
        if (permitidos == null || permitidos.isEmpty()) {
            return error;
        }
        return ResponseEntity.status(error.getStatusCode())
                .headers(h -> h.setAllow(permitidos))
                .body(error.getBody());
    }

    /**
     * Falta un parámetro de query obligatorio (antes: 500).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleParametroFaltante(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Parámetro faltante: {}", ex.getParameterName());
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'",
                VALIDACION_FALLIDA, request);
    }

    /**
     * Un parámetro o variable de ruta no se puede convertir a su tipo, p. ej.
     * /api/materias/cursos/abc (antes: 500). No se repite el valor recibido.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Tipo inválido en el parámetro '{}'", ex.getName());
        String esperado = ex.getRequiredType() == null ? "" : " (se esperaba " + ex.getRequiredType().getSimpleName() + ")";
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "El parámetro '" + ex.getName() + "' tiene un valor inválido" + esperado,
                VALIDACION_FALLIDA, request);
    }

    /**
     * Falta una variable de ruta (antes: 500).
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleVariableRutaFaltante(
            MissingPathVariableException ex, HttpServletRequest request) {
        log.warn("Variable de ruta faltante: {}", ex.getVariableName());
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Falta la variable de ruta '" + ex.getVariableName() + "'",
                VALIDACION_FALLIDA, request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Recurso duplicado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<ErrorResponse> handleResourceInUse(
            ResourceInUseException ex, HttpServletRequest request) {
        log.warn("Recurso en uso: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Maneja errores de validación de Jakarta Bean Validation (@Valid).
     * Recopila todos los mensajes de error de campo en un solo mensaje.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        log.warn("Error de validación: {}", message);
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja errores de deserialización JSON (enums inválidos, JSON malformado, etc.).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Error de lectura del mensaje HTTP: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "El cuerpo de la solicitud no es válido. Verifique el formato JSON y los valores de los campos.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Archivo ausente o con formato no permitido (validado en el servicio).
     */
    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleArchivoInvalido(
            ArchivoInvalidoException ex, HttpServletRequest request) {
        log.warn("Archivo inválido: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    /**
     * Archivo que supera el tamaño máximo, ya sea detectado por el servicio o por
     * el límite de multipart de Spring (spring.servlet.multipart.max-file-size).
     */
    @ExceptionHandler({ArchivoDemasiadoGrandeException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ErrorResponse> handleArchivoDemasiadoGrande(
            Exception ex, HttpServletRequest request) {
        log.warn("Archivo demasiado grande: {}", ex.getMessage());
        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                "El archivo supera el tamaño máximo permitido de 10 MB", request);
    }

    /**
     * Petición multipart sin la parte esperada o mal formada.
     */
    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<ErrorResponse> handleMultipart(
            Exception ex, HttpServletRequest request) {
        log.warn("Petición multipart inválida: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Debe enviar un archivo PDF en el campo \"archivo\" (multipart/form-data)", request);
    }

    /**
     * Content-Type no aceptado por el endpoint (p. ej. JSON en lugar de multipart/form-data).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Content-Type no soportado: {}", ex.getMessage());
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type no soportado para este endpoint", request);
    }

    /**
     * Fallo al comunicarse con AWS S3: no se exponen detalles del bucket ni de las credenciales.
     */
    @ExceptionHandler(SdkException.class)
    public ResponseEntity<ErrorResponse> handleS3(
            SdkException ex, HttpServletRequest request) {
        log.error("Error de AWS S3 en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.BAD_GATEWAY, "BAD_GATEWAY",
                "No se pudo completar la operación con el almacenamiento de archivos", request);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String error, String message,
                                                     HttpServletRequest request) {
        return buildError(status, error, message, null, request);
    }

    /**
     * Arma el error y, si trae {@code errorType}, lo deja también en el atributo
     * {@value #ERROR_TYPE_ATTRIBUTE} de la petición para el middleware de logs.
     */
    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String error, String message,
                                                     String errorType, HttpServletRequest request) {
        if (errorType != null) {
            request.setAttribute(ERROR_TYPE_ATTRIBUTE, errorType);
        }
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(), status.value(), error, message, request.getRequestURI(), errorType);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Captura cualquier excepción no manejada.
     * Registra el error internamente pero no expone detalles al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Error interno no manejado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "Ha ocurrido un error interno en el servidor",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
