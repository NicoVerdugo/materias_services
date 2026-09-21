package co.uptc.materias.specification;

import co.uptc.materias.entity.Docente;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications para consultas dinámicas de Docente con Criteria API.
 */
public final class DocenteSpecification {

    private DocenteSpecification() {
    }

    /**
     * Construye una Specification combinando todos los filtros proporcionados.
     *
     * @param nombres      filtro parcial por nombres (case-insensitive)
     * @param apellidos    filtro parcial por apellidos (case-insensitive)
     * @param especialidad filtro parcial por especialidad (case-insensitive)
     * @param activo       filtro exacto por estado activo
     * @return Specification combinada
     */
    public static Specification<Docente> withFilters(String nombres, String apellidos,
                                                     String especialidad, Boolean activo) {
        return Specification.where(hasNombres(nombres))
                .and(hasApellidos(apellidos))
                .and(hasEspecialidad(especialidad))
                .and(isActivo(activo));
    }

    private static Specification<Docente> hasNombres(String nombres) {
        return (root, query, cb) -> {
            if (nombres == null || nombres.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("nombres")), "%" + nombres.toLowerCase() + "%");
        };
    }

    private static Specification<Docente> hasApellidos(String apellidos) {
        return (root, query, cb) -> {
            if (apellidos == null || apellidos.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("apellidos")), "%" + apellidos.toLowerCase() + "%");
        };
    }

    private static Specification<Docente> hasEspecialidad(String especialidad) {
        return (root, query, cb) -> {
            if (especialidad == null || especialidad.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("especialidad")), "%" + especialidad.toLowerCase() + "%");
        };
    }

    private static Specification<Docente> isActivo(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
