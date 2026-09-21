package co.uptc.materias.service;

import co.uptc.materias.dto.request.MateriaCreateRequest;
import co.uptc.materias.dto.request.MateriaUpdateRequest;
import co.uptc.materias.dto.response.MateriaResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.entity.Materia;
import co.uptc.materias.exception.ResourceInUseException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.mapper.MateriaMapper;
import co.uptc.materias.repository.CursoRepository;
import co.uptc.materias.repository.MateriaRepository;
import co.uptc.materias.specification.MateriaSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Servicio con la lógica de negocio para la gestión de Materias.
 */
@Service
@Transactional(readOnly = true)
public class MateriaService {

    private static final Logger log = LoggerFactory.getLogger(MateriaService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "nombre", "creditos", "programa");

    private final MateriaRepository materiaRepository;
    private final CursoRepository cursoRepository;

    public MateriaService(MateriaRepository materiaRepository, CursoRepository cursoRepository) {
        this.materiaRepository = materiaRepository;
        this.cursoRepository = cursoRepository;
    }

    /**
     * Crea una nueva Materia.
     *
     * @param request datos de la materia a crear
     * @return la materia creada
     */
    @Transactional
    public MateriaResponse create(MateriaCreateRequest request) {
        Materia materia = MateriaMapper.toEntity(request);
        Materia saved = materiaRepository.save(materia);
        log.info("Materia creada con id: {}", saved.getId());
        return MateriaMapper.toResponse(saved);
    }

    /**
     * Obtiene una Materia por su ID.
     *
     * @param id identificador de la materia
     * @return la materia encontrada
     * @throws ResourceNotFoundException si no existe
     */
    public MateriaResponse findById(Long id) {
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada con id: " + id));
        return MateriaMapper.toResponse(materia);
    }

    /**
     * Lista materias con paginación, ordenamiento y filtros.
     *
     * @param pageNumber    número de página (base 0)
     * @param pageSize      tamaño de página (máx. 100)
     * @param sortBy        campo de ordenamiento (whitelist validada)
     * @param sortDirection dirección del ordenamiento (asc/desc)
     * @param nombre        filtro parcial por nombre
     * @param programa      filtro parcial por programa
     * @param creditos      filtro exacto por créditos
     * @return página de materias
     */
    public PageResponse<MateriaResponse> findAll(int pageNumber, int pageSize,
                                                  String sortBy, String sortDirection,
                                                  String nombre, String programa, Integer creditos) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        String validatedSortField = SORTABLE_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;

        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortField));
        Specification<Materia> spec = MateriaSpecification.withFilters(nombre, programa, creditos);
        Page<Materia> page = materiaRepository.findAll(spec, pageRequest);

        List<MateriaResponse> content = page.getContent().stream()
                .map(MateriaMapper::toResponse)
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Actualiza una Materia existente.
     *
     * @param id      identificador de la materia
     * @param request datos actualizados
     * @return la materia actualizada
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional
    public MateriaResponse update(Long id, MateriaUpdateRequest request) {
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada con id: " + id));
        MateriaMapper.updateEntity(materia, request);
        Materia updated = materiaRepository.save(materia);
        log.info("Materia actualizada con id: {}", updated.getId());
        return MateriaMapper.toResponse(updated);
    }

    /**
     * Elimina una Materia por su ID.
     * <p>
     * No permite la eliminación si la materia tiene cursos asociados,
     * para prevenir inconsistencias en los datos.
     *
     * @param id identificador de la materia
     * @throws ResourceNotFoundException si no existe
     * @throws ResourceInUseException    si tiene cursos asociados
     */
    @Transactional
    public void delete(Long id) {
        if (!materiaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Materia no encontrada con id: " + id);
        }
        if (cursoRepository.existsByMateriaId(id)) {
            throw new ResourceInUseException(
                    "No se puede eliminar la materia con id: " + id + " porque tiene cursos asociados");
        }
        materiaRepository.deleteById(id);
        log.info("Materia eliminada con id: {}", id);
    }
}
