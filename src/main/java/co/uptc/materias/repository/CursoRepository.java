package co.uptc.materias.repository;

import co.uptc.materias.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de acceso a datos para la entidad Curso.
 */
@Repository
public interface CursoRepository extends JpaRepository<Curso, Long>, JpaSpecificationExecutor<Curso> {

    /**
     * Verifica si existen cursos asociados a una materia.
     * Se usa antes de eliminar una materia para prevenir inconsistencias.
     */
    boolean existsByMateriaId(Long materiaId);

    /**
     * Verifica si existen cursos asociados a un docente.
     * Se usa antes de eliminar un docente para prevenir inconsistencias.
     */
    boolean existsByDocenteId(Long docenteId);
}
