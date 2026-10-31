package co.uptc.materias.exception;

/**
 * Excepción lanzada cuando un archivo subido supera el tamaño máximo permitido.
 * Resulta en HTTP 413 Payload Too Large.
 */
public class ArchivoDemasiadoGrandeException extends RuntimeException {

    public ArchivoDemasiadoGrandeException(String message) {
        super(message);
    }
}
