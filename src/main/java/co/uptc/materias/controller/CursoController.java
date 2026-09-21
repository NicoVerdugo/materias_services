package co.uptc.materias.controller;

import co.uptc.materias.dto.request.CursoCreateRequest;
import co.uptc.materias.dto.request.CursoUpdateRequest;
import co.uptc.materias.dto.response.CursoResponse;
import co.uptc.materias.dto.response.ErrorResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.entity.EstadoCurso;
import co.uptc.materias.entity.Modalidad;
import co.uptc.materias.service.CursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de Cursos (oferta académica).
 * <p>
 * El endpoint GET /api/cursos/{id} es clave para la futura integración
 * con el API Gateway. La respuesta incluye {@code materiaNombre} para
 * facilitar la composición del detalle de un estudiante.
 */
@RestController
@RequestMapping("/api/cursos")
@Tag(name = "Cursos", description = "Gestión de la oferta académica (cursos por periodo)")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @Operation(summary = "Crear curso",
            description = "Crea una nueva oferta de curso. La materia y el docente referenciados deben existir")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Curso creado exitosamente",
                    content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Materia o docente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CursoResponse> create(@Valid @RequestBody CursoCreateRequest request) {
        CursoResponse response = cursoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar cursos",
            description = "Lista cursos con paginación, ordenamiento y filtros combinables")
    @ApiResponse(responseCode = "200", description = "Lista de cursos obtenida exitosamente")
    @GetMapping
    public ResponseEntity<PageResponse<CursoResponse>> findAll(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Tamaño de página (máx. 100)") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Campo de ordenamiento: id, periodo, cupo, estado, modalidad") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Dirección: asc o desc") @RequestParam(defaultValue = "asc") String sortDirection,
            @Parameter(description = "Filtro exacto por periodo académico") @RequestParam(required = false) String periodo,
            @Parameter(description = "Filtro por estado: ACTIVO, INACTIVO, CANCELADO") @RequestParam(required = false) EstadoCurso estado,
            @Parameter(description = "Filtro por modalidad: PRESENCIAL, VIRTUAL, HIBRIDA") @RequestParam(required = false) Modalidad modalidad,
            @Parameter(description = "Filtro por ID de materia") @RequestParam(required = false) Long materiaId,
            @Parameter(description = "Filtro por ID de docente") @RequestParam(required = false) Long docenteId) {

        PageResponse<CursoResponse> response = cursoService.findAll(
                pageNumber, pageSize, sortBy, sortDirection, periodo, estado, modalidad, materiaId, docenteId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener curso por ID",
            description = "Consulta un curso específico. Incluye materiaNombre para composición en el API Gateway")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Curso encontrado",
                    content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Curso no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CursoResponse> findById(
            @Parameter(description = "ID del curso") @PathVariable Long id) {
        CursoResponse response = cursoService.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualizar curso",
            description = "Actualiza todos los campos de un curso. La materia y el docente referenciados deben existir")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Curso actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Curso, materia o docente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<CursoResponse> update(
            @Parameter(description = "ID del curso") @PathVariable Long id,
            @Valid @RequestBody CursoUpdateRequest request) {
        CursoResponse response = cursoService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar curso", description = "Elimina un curso de la oferta académica")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Curso eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Curso no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del curso") @PathVariable Long id) {
        cursoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
