package co.uptc.materias.specification;

import co.uptc.materias.entity.Curso;
import co.uptc.materias.entity.EstadoCurso;
import co.uptc.materias.entity.Modalidad;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications para consultas dinámicas de Curso con Criteria API.
 */
public final class CursoSpecification {

    private CursoSpecification() {
    }

    /**
     * Construye una Specification combinando todos los filtros proporcionados.
     *
     * @param periodo    filtro exacto por periodo académico
     * @param estado     filtro exacto por estado del curso
     * @param modalidad  filtro exacto por modalidad
     * @param materiaId  filtro exacto por ID de materia
     * @param docenteId  filtro exacto por ID de docente
     * @return Specification combinada
     */
    public static Specification<Curso> withFilters(String periodo, EstadoCurso estado,
                                                   Modalidad modalidad, Long materiaId,
                                                   Long docenteId) {
        return Specification.where(hasPeriodo(periodo))
                .and(hasEstado(estado))
                .and(hasModalidad(modalidad))
                .and(hasMateriaId(materiaId))
                .and(hasDocenteId(docenteId));
    }

    private static Specification<Curso> hasPeriodo(String periodo) {
        return (root, query, cb) -> {
            if (periodo == null || periodo.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("periodo"), periodo);
        };
    }

    private static Specification<Curso> hasEstado(EstadoCurso estado) {
        return (root, query, cb) -> {
            if (estado == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("estado"), estado);
        };
    }

    private static Specification<Curso> hasModalidad(Modalidad modalidad) {
        return (root, query, cb) -> {
            if (modalidad == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("modalidad"), modalidad);
        };
    }

    private static Specification<Curso> hasMateriaId(Long materiaId) {
        return (root, query, cb) -> {
            if (materiaId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("materia").get("id"), materiaId);
        };
    }

    private static Specification<Curso> hasDocenteId(Long docenteId) {
        return (root, query, cb) -> {
            if (docenteId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("docente").get("id"), docenteId);
        };
    }
}
