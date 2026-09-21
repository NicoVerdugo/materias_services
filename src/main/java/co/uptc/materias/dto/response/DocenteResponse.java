package co.uptc.materias.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para Docente.
 */
@Schema(description = "Información de un docente")
public record DocenteResponse(

        @Schema(description = "Identificador único", example = "1")
        Long id,

        @Schema(description = "Nombres del docente", example = "Carlos Andrés")
        String nombres,

        @Schema(description = "Apellidos del docente", example = "Ramírez López")
        String apellidos,

        @Schema(description = "Correo electrónico institucional", example = "carlos.ramirez@uptc.edu.co")
        String correoInstitucional,

        @Schema(description = "Área de especialidad", example = "Ingeniería de Software")
        String especialidad,

        @Schema(description = "Indica si el docente está activo", example = "true")
        Boolean activo
) {
}
