package co.uptc.materias.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para un archivo de contenido temático.
 */
@Schema(description = "Metadatos de un archivo de contenido temático almacenado en AWS S3")
public record ContenidoTematicoResponse(

        @Schema(description = "Identificador único", example = "1")
        Long id,

        @Schema(description = "ID de la materia asociada", example = "1")
        Long materiaId,

        @Schema(description = "Clave del objeto en el bucket de S3",
                example = "materias/1/contenido-tematico/3f1c9a2e-7b4d-4e8a-9c1f-2d6b8e0a4c7f.pdf")
        String claveS3,

        @Schema(description = "Nombre original del archivo", example = "programa-sistemas-distribuidos.pdf")
        String nombreOriginal,

        @Schema(description = "Tipo MIME", example = "application/pdf")
        String tipoMime,

        @Schema(description = "Tamaño en bytes", example = "524288")
        Long tamanoBytes,

        @Schema(description = "URL firmada temporal (1 hora) para descargar el archivo; no se guarda en la base de datos")
        String url
) {
}
