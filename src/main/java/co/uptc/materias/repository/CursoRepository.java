package co.uptc.materias.repository;

import co.uptc.materias.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de acceso a datos para la entidad Curso.
 * <p>
 * El cupo se modifica solo con sentencias UPDATE condicionales: la condición y la
 * escritura ocurren en la misma sentencia, con el bloqueo de fila de PostgreSQL, así
 * que dos peticiones simultáneas nunca leen el mismo valor para luego escribirlo.
 * Cada método devuelve el número de filas afectadas (0 o 1).
 * <p>
 * {@code flushAutomatically} envía antes los cambios pendientes de la entidad y
 * {@code clearAutomatically} vacía el contexto de persistencia, para que la lectura
 * posterior traiga los valores recién escritos y no una copia en caché.
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

    /**
     * Descuenta un cupo si el curso existe, está ACTIVO y le queda cupo.
     *
     * @return 1 si descontó, 0 si alguna condición no se cumplió
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE cursos SET cupo_disponible = cupo_disponible - 1
            WHERE id = :id AND estado = 'ACTIVO' AND cupo_disponible > 0
            """, nativeQuery = true)
    int reservarCupo(@Param("id") Long id);

    /**
     * Devuelve un cupo si el curso existe y no está lleno (compensación o cancelación).
     *
     * @return 1 si devolvió, 0 si no existe o cupo_disponible ya es igual a cupo
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE cursos SET cupo_disponible = cupo_disponible + 1
            WHERE id = :id AND cupo_disponible < cupo
            """, nativeQuery = true)
    int liberarCupo(@Param("id") Long id);

    /**
     * Cambia la capacidad y mueve cupo_disponible en la misma diferencia
     * (cupo_nuevo - cupo_viejo). En PostgreSQL las expresiones del SET usan los
     * valores previos de la fila, así que {@code cupo} a la derecha es el cupo viejo.
     *
     * @return 1 si ajustó, 0 si no existe o el cupo disponible quedaría negativo
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE cursos
            SET cupo_disponible = cupo_disponible + (:cupoNuevo - cupo),
                cupo = :cupoNuevo
            WHERE id = :id AND cupo_disponible + (:cupoNuevo - cupo) >= 0
            """, nativeQuery = true)
    int ajustarCupo(@Param("id") Long id, @Param("cupoNuevo") int cupoNuevo);

    /**
     * Baja lógica: el curso pasa a INACTIVO y la fila se conserva, porque otros
     * módulos (Inscripciones) guardan su id sin FK.
     *
     * @return 1 si existía, 0 si no
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE cursos SET estado = 'INACTIVO' WHERE id = :id", nativeQuery = true)
    int inactivar(@Param("id") Long id);
}
