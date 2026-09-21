package co.uptc.materias.service;

import co.uptc.materias.dto.request.DocenteCreateRequest;
import co.uptc.materias.dto.request.DocenteUpdateRequest;
import co.uptc.materias.dto.response.DocenteResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.entity.Docente;
import co.uptc.materias.exception.DuplicateResourceException;
import co.uptc.materias.exception.ResourceInUseException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.mapper.DocenteMapper;
import co.uptc.materias.repository.CursoRepository;
import co.uptc.materias.repository.DocenteRepository;
import co.uptc.materias.specification.DocenteSpecification;
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
 * Servicio con la lógica de negocio para la gestión de Docentes.
 */
@Service
@Transactional(readOnly = true)
public class DocenteService {

    private static final Logger log = LoggerFactory.getLogger(DocenteService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "nombres", "apellidos", "especialidad");

    private final DocenteRepository docenteRepository;
    private final CursoRepository cursoRepository;

    public DocenteService(DocenteRepository docenteRepository, CursoRepository cursoRepository) {
        this.docenteRepository = docenteRepository;
        this.cursoRepository = cursoRepository;
    }

    /**
     * Crea un nuevo Docente.
     * <p>
     * Valida que el correo institucional no esté ya registrado.
     *
     * @param request datos del docente a crear
     * @return el docente creado
     * @throws DuplicateResourceException si el correo ya existe
     */
    @Transactional
    public DocenteResponse create(DocenteCreateRequest request) {
        if (docenteRepository.existsByCorreoInstitucional(request.correoInstitucional())) {
            throw new DuplicateResourceException(
                    "Ya existe un docente con el correo institucional: " + request.correoInstitucional());
        }
        Docente docente = DocenteMapper.toEntity(request);
        Docente saved = docenteRepository.save(docente);
        log.info("Docente creado con id: {}", saved.getId());
        return DocenteMapper.toResponse(saved);
    }

    /**
     * Obtiene un Docente por su ID.
     *
     * @param id identificador del docente
     * @return el docente encontrado
     * @throws ResourceNotFoundException si no existe
     */
    public DocenteResponse findById(Long id) {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con id: " + id));
        return DocenteMapper.toResponse(docente);
    }

    /**
     * Lista docentes con paginación, ordenamiento y filtros.
     */
    public PageResponse<DocenteResponse> findAll(int pageNumber, int pageSize,
                                                  String sortBy, String sortDirection,
                                                  String nombres, String apellidos,
                                                  String especialidad, Boolean activo) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        String validatedSortField = SORTABLE_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;

        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortField));
        Specification<Docente> spec = DocenteSpecification.withFilters(nombres, apellidos, especialidad, activo);
        Page<Docente> page = docenteRepository.findAll(spec, pageRequest);

        List<DocenteResponse> content = page.getContent().stream()
                .map(DocenteMapper::toResponse)
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Actualiza un Docente existente.
     * <p>
     * Valida que el correo institucional no esté ya registrado por otro docente.
     *
     * @param id      identificador del docente
     * @param request datos actualizados
     * @return el docente actualizado
     * @throws ResourceNotFoundException  si no existe
     * @throws DuplicateResourceException si el correo ya existe en otro docente
     */
    @Transactional
    public DocenteResponse update(Long id, DocenteUpdateRequest request) {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con id: " + id));

        if (docenteRepository.existsByCorreoInstitucionalAndIdNot(request.correoInstitucional(), id)) {
            throw new DuplicateResourceException(
                    "Ya existe otro docente con el correo institucional: " + request.correoInstitucional());
        }

        DocenteMapper.updateEntity(docente, request);
        Docente updated = docenteRepository.save(docente);
        log.info("Docente actualizado con id: {}", updated.getId());
        return DocenteMapper.toResponse(updated);
    }

    /**
     * Elimina un Docente por su ID.
     * <p>
     * No permite la eliminación si el docente tiene cursos asociados.
     *
     * @param id identificador del docente
     * @throws ResourceNotFoundException si no existe
     * @throws ResourceInUseException    si tiene cursos asociados
     */
    @Transactional
    public void delete(Long id) {
        if (!docenteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Docente no encontrado con id: " + id);
        }
        if (cursoRepository.existsByDocenteId(id)) {
            throw new ResourceInUseException(
                    "No se puede eliminar el docente con id: " + id + " porque tiene cursos asociados");
        }
        docenteRepository.deleteById(id);
        log.info("Docente eliminado con id: {}", id);
    }
}
