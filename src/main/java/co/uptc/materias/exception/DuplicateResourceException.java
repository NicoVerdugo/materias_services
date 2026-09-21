package co.uptc.materias.exception;

/**
 * Excepción lanzada cuando se intenta crear un recurso que viola una restricción de unicidad.
 * Ejemplo: correo institucional duplicado.
 * Genera un HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
