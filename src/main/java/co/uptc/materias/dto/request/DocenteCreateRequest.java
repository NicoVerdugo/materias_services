package co.uptc.materias.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la creación de un Docente.
 */
@Schema(description = "Datos requeridos para crear un docente")
public record DocenteCreateRequest(

        @Schema(description = "Nombres del docente", example = "Carlos Andrés")
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no deben exceder 100 caracteres")
        String nombres,

        @Schema(description = "Apellidos del docente", example = "Ramírez López")
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no deben exceder 100 caracteres")
        String apellidos,

        @Schema(description = "Correo electrónico institucional (debe ser único)", example = "carlos.ramirez@uptc.edu.co")
        @NotBlank(message = "El correo institucional es obligatorio")
        @Email(message = "El correo institucional debe tener un formato válido")
        @Size(max = 150, message = "El correo institucional no debe exceder 150 caracteres")
        String correoInstitucional,

        @Schema(description = "Área de especialidad del docente", example = "Ingeniería de Software")
        @NotBlank(message = "La especialidad es obligatoria")
        @Size(max = 150, message = "La especialidad no debe exceder 150 caracteres")
        String especialidad,

        @Schema(description = "Indica si el docente está activo", example = "true")
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
