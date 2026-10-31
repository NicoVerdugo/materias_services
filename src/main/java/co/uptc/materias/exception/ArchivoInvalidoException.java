package co.uptc.materias.exception;

/**
 * Excepción lanzada cuando un archivo subido falta o no tiene el formato permitido.
 * Resulta en HTTP 400 Bad Request.
 */
public class ArchivoInvalidoException extends RuntimeException {

    public ArchivoInvalidoException(String message) {
        super(message);
    }
}
