package co.uptc.materias.specification;

import co.uptc.materias.entity.Materia;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications para consultas dinámicas de Materia con Criteria API.
 * <p>
 * Los filtros se combinan con AND. Un parámetro null o vacío
 * no añade predicado (se ignora), permitiendo combinación libre.
 */
public final class MateriaSpecification {

    private MateriaSpecification() {
    }

    /**
     * Construye una Specification combinando todos los filtros proporcionados.
     *
     * @param nombre   filtro parcial por nombre (case-insensitive)
     * @param programa filtro parcial por programa (case-insensitive)
     * @param creditos filtro exacto por número de créditos
     * @return Specification combinada
     */
    public static Specification<Materia> withFilters(String nombre, String programa, Integer creditos) {
        return Specification.where(hasNombre(nombre))
                .and(hasPrograma(programa))
                .and(hasCreditos(creditos));
    }

    private static Specification<Materia> hasNombre(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
        };
    }

    private static Specification<Materia> hasPrograma(String programa) {
        return (root, query, cb) -> {
            if (programa == null || programa.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("programa")), "%" + programa.toLowerCase() + "%");
        };
    }

    private static Specification<Materia> hasCreditos(Integer creditos) {
        return (root, query, cb) -> {
            if (creditos == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("creditos"), creditos);
        };
    }
}
