package co.uptc.materias.exception;

/**
 * Excepción lanzada cuando un recurso no se encuentra en la base de datos.
 * Genera un HTTP 404 Not Found.
 * <p>
 * {@code errorType} es opcional; para cursos se usa {@link #CURSO_NO_EXISTE}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public static final String CURSO_NO_EXISTE = "CursoNoExiste";

    private final String errorType;

    public ResourceNotFoundException(String message) {
        this(message, null);
    }

    public ResourceNotFoundException(String message, String errorType) {
        super(message);
        this.errorType = errorType;
    }

    public static ResourceNotFoundException curso(Long id) {
        return new ResourceNotFoundException("Curso no encontrado con id: " + id, CURSO_NO_EXISTE);
    }

    public String getErrorType() {
        return errorType;
    }
}
