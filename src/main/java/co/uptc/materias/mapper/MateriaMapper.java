package co.uptc.materias.mapper;

import co.uptc.materias.dto.request.MateriaCreateRequest;
import co.uptc.materias.dto.request.MateriaUpdateRequest;
import co.uptc.materias.dto.response.MateriaResponse;
import co.uptc.materias.entity.Materia;

/**
 * Mapper para conversión entre entidad Materia y sus DTOs.
 * <p>
 * Métodos estáticos puros, sin dependencias externas.
 */
public final class MateriaMapper {

    private MateriaMapper() {
    }

    /**
     * Convierte un request de creación a entidad Materia.
     */
    public static Materia toEntity(MateriaCreateRequest request) {
        Materia materia = new Materia();
        materia.setNombre(request.nombre());
        materia.setDescripcion(request.descripcion());
        materia.setCreditos(request.creditos());
        materia.setPrograma(request.programa());
        return materia;
    }

    /**
     * Actualiza una entidad Materia existente con los datos del request.
     */
    public static void updateEntity(Materia materia, MateriaUpdateRequest request) {
        materia.setNombre(request.nombre());
        materia.setDescripcion(request.descripcion());
        materia.setCreditos(request.creditos());
        materia.setPrograma(request.programa());
    }

    /**
     * Convierte una entidad Materia a su DTO de respuesta.
     */
    public static MateriaResponse toResponse(Materia materia) {
        return new MateriaResponse(
                materia.getId(),
                materia.getNombre(),
                materia.getDescripcion(),
                materia.getCreditos(),
                materia.getPrograma()
        );
    }
}
