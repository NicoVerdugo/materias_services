package co.uptc.materias.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO estándar de respuesta de error.
 * <p>
 * Estructura consistente para todos los errores HTTP devueltos por la API.
 * No expone información interna del sistema (stack traces, SQL, rutas internas).
 */
@Schema(description = "Respuesta de error")
public record ErrorResponse(

        @Schema(description = "Fecha y hora del error", example = "2026-09-21T20:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,

        @Schema(description = "Código de estado HTTP", example = "404")
        int status,

        @Schema(description = "Tipo de error", example = "NOT_FOUND")
        String error,

        @Schema(description = "Mensaje descriptivo del error", example = "Materia no encontrada con id: 99")
        String message,

        @Schema(description = "Ruta del recurso solicitado", example = "/api/materias/99")
        String path
) {
}
