package co.uptc.materias.service;

import co.uptc.materias.dto.request.CursoCreateRequest;
import co.uptc.materias.dto.request.CursoUpdateRequest;
import co.uptc.materias.dto.response.CursoResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.entity.Curso;
import co.uptc.materias.entity.Docente;
import co.uptc.materias.entity.EstadoCurso;
import co.uptc.materias.entity.Materia;
import co.uptc.materias.entity.Modalidad;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.mapper.CursoMapper;
import co.uptc.materias.repository.CursoRepository;
import co.uptc.materias.repository.DocenteRepository;
import co.uptc.materias.repository.MateriaRepository;
import co.uptc.materias.specification.CursoSpecification;
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
 * Servicio con la lógica de negocio para la gestión de Cursos.
 * <p>
 * Un Curso representa una oferta concreta de una Materia en un periodo académico,
 * impartida por un Docente. Es la entidad clave para la futura integración
 * con el API Gateway.
 */
@Service
@Transactional(readOnly = true)
public class CursoService {

    private static final Logger log = LoggerFactory.getLogger(CursoService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "periodo", "cupo", "estado", "modalidad");

    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final DocenteRepository docenteRepository;

    public CursoService(CursoRepository cursoRepository, MateriaRepository materiaRepository,
                        DocenteRepository docenteRepository) {
        this.cursoRepository = cursoRepository;
        this.materiaRepository = materiaRepository;
        this.docenteRepository = docenteRepository;
    }

    /**
     * Crea un nuevo Curso.
     * <p>
     * Valida que tanto la Materia como el Docente referenciados existan.
     *
     * @param request datos del curso a crear
     * @return el curso creado
     * @throws ResourceNotFoundException si la materia o el docente no existen
     */
    @Transactional
    public CursoResponse create(CursoCreateRequest request) {
        Materia materia = materiaRepository.findById(request.materiaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Materia no encontrada con id: " + request.materiaId()));
        Docente docente = docenteRepository.findById(request.docenteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Docente no encontrado con id: " + request.docenteId()));

        Curso curso = new Curso();
        curso.setMateria(materia);
        curso.setDocente(docente);
        curso.setHorario(request.horario());
        curso.setPeriodo(request.periodo());
        curso.setCupo(request.cupo());
        curso.setAula(request.aula());
        curso.setModalidad(request.modalidad());
        curso.setEstado(request.estado());

        Curso saved = cursoRepository.save(curso);
        log.info("Curso creado con id: {}", saved.getId());
        return CursoMapper.toResponse(saved);
    }

    /**
     * Obtiene un Curso por su ID.
     * <p>
     * La respuesta incluye {@code materiaNombre} para facilitar la composición
     * en el API Gateway sin necesidad de una segunda consulta.
     *
     * @param id identificador del curso
     * @return el curso encontrado
     * @throws ResourceNotFoundException si no existe
     */
    public CursoResponse findById(Long id) {
        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + id));
        return CursoMapper.toResponse(curso);
    }

    /**
     * Lista cursos con paginación, ordenamiento y filtros.
     */
    public PageResponse<CursoResponse> findAll(int pageNumber, int pageSize,
                                                String sortBy, String sortDirection,
                                                String periodo, EstadoCurso estado,
                                                Modalidad modalidad, Long materiaId,
                                                Long docenteId) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        String validatedSortField = SORTABLE_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;

        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortField));
        Specification<Curso> spec = CursoSpecification.withFilters(periodo, estado, modalidad, materiaId, docenteId);
        Page<Curso> page = cursoRepository.findAll(spec, pageRequest);

        List<CursoResponse> content = page.getContent().stream()
                .map(CursoMapper::toResponse)
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Actualiza un Curso existente.
     * <p>
     * Valida que la Materia y el Docente referenciados existan.
     *
     * @param id      identificador del curso
     * @param request datos actualizados
     * @return el curso actualizado
     * @throws ResourceNotFoundException si el curso, materia o docente no existen
     */
    @Transactional
    public CursoResponse update(Long id, CursoUpdateRequest request) {
        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + id));

        Materia materia = materiaRepository.findById(request.materiaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Materia no encontrada con id: " + request.materiaId()));
        Docente docente = docenteRepository.findById(request.docenteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Docente no encontrado con id: " + request.docenteId()));

        curso.setMateria(materia);
        curso.setDocente(docente);
        curso.setHorario(request.horario());
        curso.setPeriodo(request.periodo());
        curso.setCupo(request.cupo());
        curso.setAula(request.aula());
        curso.setModalidad(request.modalidad());
        curso.setEstado(request.estado());

        Curso updated = cursoRepository.save(curso);
        log.info("Curso actualizado con id: {}", updated.getId());
        return CursoMapper.toResponse(updated);
    }

    /**
     * Elimina un Curso por su ID.
     *
     * @param id identificador del curso
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional
    public void delete(Long id) {
        if (!cursoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Curso no encontrado con id: " + id);
        }
        cursoRepository.deleteById(id);
        log.info("Curso eliminado con id: {}", id);
    }
}
