package co.uptc.materias.mapper;

import co.uptc.materias.dto.request.DocenteCreateRequest;
import co.uptc.materias.dto.request.DocenteUpdateRequest;
import co.uptc.materias.dto.response.DocenteResponse;
import co.uptc.materias.entity.Docente;

/**
 * Mapper para conversión entre entidad Docente y sus DTOs.
 */
public final class DocenteMapper {

    private DocenteMapper() {
    }

    /**
     * Convierte un request de creación a entidad Docente.
     */
    public static Docente toEntity(DocenteCreateRequest request) {
        Docente docente = new Docente();
        docente.setNombres(request.nombres());
        docente.setApellidos(request.apellidos());
        docente.setCorreoInstitucional(request.correoInstitucional());
        docente.setEspecialidad(request.especialidad());
        docente.setActivo(request.activo());
        return docente;
    }

    /**
     * Actualiza una entidad Docente existente con los datos del request.
     */
    public static void updateEntity(Docente docente, DocenteUpdateRequest request) {
        docente.setNombres(request.nombres());
        docente.setApellidos(request.apellidos());
        docente.setCorreoInstitucional(request.correoInstitucional());
        docente.setEspecialidad(request.especialidad());
        docente.setActivo(request.activo());
    }

    /**
     * Convierte una entidad Docente a su DTO de respuesta.
     */
    public static DocenteResponse toResponse(Docente docente) {
        return new DocenteResponse(
                docente.getId(),
                docente.getNombres(),
                docente.getApellidos(),
                docente.getCorreoInstitucional(),
                docente.getEspecialidad(),
                docente.getActivo()
        );
    }
}
