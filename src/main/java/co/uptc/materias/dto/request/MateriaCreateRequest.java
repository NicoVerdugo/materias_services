package co.uptc.materias.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO para la creación de una Materia.
 */
@Schema(description = "Datos requeridos para crear una materia")
public record MateriaCreateRequest(

        @Schema(description = "Nombre de la materia", example = "Sistemas Distribuidos")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no debe exceder 150 caracteres")
        String nombre,

        @Schema(description = "Descripción de la materia", example = "Estudio de sistemas de cómputo distribuido y sus paradigmas")
        @Size(max = 1000, message = "La descripción no debe exceder 1000 caracteres")
        String descripcion,

        @Schema(description = "Número de créditos académicos", example = "4")
        @NotNull(message = "Los créditos son obligatorios")
        @Positive(message = "Los créditos deben ser un número positivo")
        Integer creditos,

        @Schema(description = "Programa académico al que pertenece", example = "Ingeniería de Sistemas y Computación")
        @NotBlank(message = "El programa es obligatorio")
        @Size(max = 150, message = "El programa no debe exceder 150 caracteres")
        String programa
) {
}
