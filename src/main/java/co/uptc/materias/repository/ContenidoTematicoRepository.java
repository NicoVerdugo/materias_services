package co.uptc.materias.repository;

import co.uptc.materias.entity.ContenidoTematico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de acceso a datos para los metadatos del contenido temático.
 */
@Repository
public interface ContenidoTematicoRepository extends JpaRepository<ContenidoTematico, Long> {

    List<ContenidoTematico> findByMateriaIdOrderByIdAsc(Long materiaId);

    boolean existsByMateriaId(Long materiaId);
}
