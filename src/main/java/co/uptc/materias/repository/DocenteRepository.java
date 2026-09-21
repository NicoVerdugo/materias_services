package co.uptc.materias.repository;

import co.uptc.materias.entity.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de acceso a datos para la entidad Docente.
 */
@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long>, JpaSpecificationExecutor<Docente> {

    /**
     * Verifica si existe un docente con el correo institucional especificado.
     *
     * @param correoInstitucional el correo a verificar
     * @return true si ya existe un docente con ese correo
     */
    boolean existsByCorreoInstitucional(String correoInstitucional);

    /**
     * Verifica si existe otro docente (diferente al ID dado) con el correo institucional especificado.
     * Útil para validar unicidad durante actualización.
     *
     * @param correoInstitucional el correo a verificar
     * @param id                  el ID del docente que se está actualizando (se excluye)
     * @return true si existe otro docente con ese correo
     */
    boolean existsByCorreoInstitucionalAndIdNot(String correoInstitucional, Long id);
}
