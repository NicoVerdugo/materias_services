package co.uptc.materias.dto.request;

import co.uptc.materias.entity.EstadoCurso;
import co.uptc.materias.entity.Modalidad;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO para la actualización de un Curso.
 */
@Schema(description = "Datos requeridos para actualizar un curso")
public record CursoUpdateRequest(

        @Schema(description = "ID de la materia asociada", example = "1")
        @NotNull(message = "El ID de la materia es obligatorio")
        Long materiaId,

        @Schema(description = "ID del docente asignado", example = "1")
        @NotNull(message = "El ID del docente es obligatorio")
        Long docenteId,

        @Schema(description = "Horario del curso", example = "Martes y Jueves 10:00-12:00")
        @NotBlank(message = "El horario es obligatorio")
        @Size(max = 200, message = "El horario no debe exceder 200 caracteres")
        String horario,

        @Schema(description = "Periodo académico", example = "2026-2")
        @NotBlank(message = "El periodo es obligatorio")
        @Size(max = 20, message = "El periodo no debe exceder 20 caracteres")
        String periodo,

        @Schema(description = "Cupo máximo del curso", example = "35")
        @NotNull(message = "El cupo es obligatorio")
        @Positive(message = "El cupo debe ser un número positivo")
        Integer cupo,

        @Schema(description = "Aula asignada", example = "Aula 301")
        @NotBlank(message = "El aula es obligatoria")
        @Size(max = 50, message = "El aula no debe exceder 50 caracteres")
        String aula,

        @Schema(description = "Modalidad del curso", example = "PRESENCIAL")
        @NotNull(message = "La modalidad es obligatoria")
        Modalidad modalidad,

        @Schema(description = "Estado del curso", example = "ACTIVO")
        @NotNull(message = "El estado es obligatorio")
        EstadoCurso estado
) {
}
