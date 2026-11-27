package co.uptc.materias.exception;

/**
 * Conflicto con el cupo de un curso. Genera un HTTP 409 Conflict con {@code errorType}.
 * <ul>
 *   <li>{@code CupoAgotado}: reservar-cupo sobre un curso con cupo_disponible = 0.</li>
 *   <li>{@code CupoYaCompleto}: liberar-cupo sobre un curso con cupo_disponible = cupo.</li>
 * </ul>
 */
public class CupoAgotadoException extends RuntimeException {

    public static final String CUPO_AGOTADO = "CupoAgotado";
    public static final String CUPO_YA_COMPLETO = "CupoYaCompleto";

    private final String errorType;

    public CupoAgotadoException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public static CupoAgotadoException agotado(Long cursoId) {
        return new CupoAgotadoException(CUPO_AGOTADO,
                "El curso " + cursoId + " no tiene cupo disponible");
    }

    public static CupoAgotadoException yaCompleto(Long cursoId) {
        return new CupoAgotadoException(CUPO_YA_COMPLETO,
                "El curso " + cursoId + " ya tiene todo su cupo disponible; no hay cupo que liberar");
    }

    public String getErrorType() {
        return errorType;
    }
}
