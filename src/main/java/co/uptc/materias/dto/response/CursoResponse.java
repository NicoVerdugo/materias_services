package co.uptc.materias.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para Curso.
 * <p>
 * Incluye {@code materiaNombre} para facilitar la futura composición
 * en el API Gateway sin requerir una segunda consulta a Materias.
 */
@Schema(description = "Información de un curso (oferta académica)")
public record CursoResponse(

        @Schema(description = "Identificador único del curso", example = "1")
        Long id,

        @Schema(description = "ID de la materia asociada", example = "3")
        Long materiaId,

        @Schema(description = "Nombre de la materia asociada", example = "Sistemas Distribuidos")
        String materiaNombre,

        @Schema(description = "ID del docente asignado", example = "2")
        Long docenteId,

        @Schema(description = "Horario del curso", example = "Lunes y Miércoles 14:00-16:00")
        String horario,

        @Schema(description = "Periodo académico", example = "2026-2")
        String periodo,

        @Schema(description = "Cupo máximo", example = "30")
        Integer cupo,

        @Schema(description = "Aula asignada", example = "Laboratorio 204")
        String aula,

        @Schema(description = "Modalidad del curso", example = "PRESENCIAL")
        String modalidad,

        @Schema(description = "Estado del curso", example = "ACTIVO")
        String estado
) {
}
