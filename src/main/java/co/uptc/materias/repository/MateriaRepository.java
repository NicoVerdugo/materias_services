package co.uptc.materias.repository;

import co.uptc.materias.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de acceso a datos para la entidad Materia.
 * <p>
 * Extiende {@link JpaSpecificationExecutor} para soportar
 * consultas dinámicas con filtros combinables.
 */
@Repository
public interface MateriaRepository extends JpaRepository<Materia, Long>, JpaSpecificationExecutor<Materia> {
}
