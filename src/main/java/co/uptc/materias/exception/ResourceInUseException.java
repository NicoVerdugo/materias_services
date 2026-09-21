package co.uptc.materias.exception;

/**
 * Excepción lanzada cuando se intenta eliminar un recurso que tiene dependencias activas.
 * Ejemplo: eliminar una Materia que tiene Cursos asociados.
 * Genera un HTTP 409 Conflict.
 */
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
