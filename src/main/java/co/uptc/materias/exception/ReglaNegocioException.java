package co.uptc.materias.exception;

/**
 * La petición es válida pero viola una regla de negocio.
 * Genera un HTTP 422 Unprocessable Entity con {@code errorType}.
 * <ul>
 *   <li>{@code CursoInactivo}: reservar-cupo sobre un curso cuyo estado no es ACTIVO.</li>
 *   <li>{@code CupoMenorQueInscritos}: PUT que baja el cupo por debajo de los cupos ya reservados.</li>
 * </ul>
 */
public class ReglaNegocioException extends RuntimeException {

    public static final String CURSO_INACTIVO = "CursoInactivo";
    public static final String CUPO_MENOR_QUE_INSCRITOS = "CupoMenorQueInscritos";

    private final String errorType;

    public ReglaNegocioException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public static ReglaNegocioException cursoInactivo(Long cursoId, Object estado) {
        return new ReglaNegocioException(CURSO_INACTIVO,
                "El curso " + cursoId + " no está ACTIVO (estado: " + estado + ")");
    }

    public static ReglaNegocioException cupoMenorQueInscritos(Long cursoId, int cupoNuevo) {
        return new ReglaNegocioException(CUPO_MENOR_QUE_INSCRITOS,
                "El cupo " + cupoNuevo + " es menor que los cupos ya reservados en el curso " + cursoId);
    }

    public String getErrorType() {
        return errorType;
    }
}
