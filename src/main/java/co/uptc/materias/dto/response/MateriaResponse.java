package co.uptc.materias.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para Materia.
 */
@Schema(description = "Información de una materia")
public record MateriaResponse(

        @Schema(description = "Identificador único", example = "1")
        Long id,

        @Schema(description = "Nombre de la materia", example = "Sistemas Distribuidos")
        String nombre,

        @Schema(description = "Descripción de la materia", example = "Estudio de sistemas de cómputo distribuido y sus paradigmas")
        String descripcion,

        @Schema(description = "Número de créditos académicos", example = "4")
        Integer creditos,

        @Schema(description = "Programa académico", example = "Ingeniería de Sistemas y Computación")
        String programa
) {
}
