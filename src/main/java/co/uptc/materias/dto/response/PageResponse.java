package co.uptc.materias.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO genérico de respuesta paginada.
 * <p>
 * Encapsula los resultados de una consulta paginada con metadatos
 * de la página actual, tamaño, total de elementos y total de páginas.
 *
 * @param <T> tipo de los elementos en la página
 */
@Schema(description = "Respuesta paginada")
public record PageResponse<T>(

        @Schema(description = "Elementos de la página actual")
        List<T> content,

        @Schema(description = "Número de página (base 0)", example = "0")
        int pageNumber,

        @Schema(description = "Tamaño de la página", example = "10")
        int pageSize,

        @Schema(description = "Total de elementos", example = "25")
        long totalElements,

        @Schema(description = "Total de páginas", example = "3")
        int totalPages
) {
}
