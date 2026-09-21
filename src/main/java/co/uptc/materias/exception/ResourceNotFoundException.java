package co.uptc.materias.exception;

/**
 * Excepción lanzada cuando un recurso no se encuentra en la base de datos.
 * Genera un HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
