package co.uptc.materias.mapper;

import co.uptc.materias.dto.response.CursoResponse;
import co.uptc.materias.entity.Curso;

/**
 * Mapper para conversión entre entidad Curso y sus DTOs.
 * <p>
 * La creación de Curso no se mapea aquí porque requiere
 * resolver las entidades Materia y Docente en el Service.
 */
public final class CursoMapper {

    private CursoMapper() {
    }

    /**
     * Convierte una entidad Curso a su DTO de respuesta.
     * <p>
     * Incluye {@code materiaNombre} para facilitar la composición
     * futura en el API Gateway.
     */
    public static CursoResponse toResponse(Curso curso) {
        return new CursoResponse(
                curso.getId(),
                curso.getMateria().getId(),
                curso.getMateria().getNombre(),
                curso.getDocente().getId(),
                curso.getHorario(),
                curso.getPeriodo(),
                curso.getCupo(),
                curso.getAula(),
                curso.getModalidad().name(),
                curso.getEstado().name()
        );
    }
}
