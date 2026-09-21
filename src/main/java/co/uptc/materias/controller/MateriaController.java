package co.uptc.materias.controller;

import co.uptc.materias.dto.request.MateriaCreateRequest;
import co.uptc.materias.dto.request.MateriaUpdateRequest;
import co.uptc.materias.dto.response.ErrorResponse;
import co.uptc.materias.dto.response.MateriaResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.service.MateriaService;
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
 * Controlador REST para la gestión de Materias.
 * <p>
 * Expone los endpoints CRUD con paginación, filtros y ordenamiento.
 */
@RestController
@RequestMapping("/api/materias")
@Tag(name = "Materias", description = "Gestión del catálogo de materias académicas")
public class MateriaController {

    private final MateriaService materiaService;

    public MateriaController(MateriaService materiaService) {
        this.materiaService = materiaService;
    }

    @Operation(summary = "Crear materia", description = "Crea una nueva materia en el catálogo académico")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Materia creada exitosamente",
                    content = @Content(schema = @Schema(implementation = MateriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<MateriaResponse> create(@Valid @RequestBody MateriaCreateRequest request) {
        MateriaResponse response = materiaService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar materias", description = "Lista materias con paginación, ordenamiento y filtros combinables")
    @ApiResponse(responseCode = "200", description = "Lista de materias obtenida exitosamente")
    @GetMapping
    public ResponseEntity<PageResponse<MateriaResponse>> findAll(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Tamaño de página (máx. 100)") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Campo de ordenamiento: id, nombre, creditos, programa") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Dirección: asc o desc") @RequestParam(defaultValue = "asc") String sortDirection,
            @Parameter(description = "Filtro parcial por nombre (case-insensitive)") @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtro parcial por programa (case-insensitive)") @RequestParam(required = false) String programa,
            @Parameter(description = "Filtro exacto por número de créditos") @RequestParam(required = false) Integer creditos) {

        PageResponse<MateriaResponse> response = materiaService.findAll(
                pageNumber, pageSize, sortBy, sortDirection, nombre, programa, creditos);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener materia por ID", description = "Consulta una materia específica por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Materia encontrada",
                    content = @Content(schema = @Schema(implementation = MateriaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Materia no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<MateriaResponse> findById(
            @Parameter(description = "ID de la materia") @PathVariable Long id) {
        MateriaResponse response = materiaService.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualizar materia", description = "Actualiza todos los campos de una materia existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Materia actualizada exitosamente",
                    content = @Content(schema = @Schema(implementation = MateriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Materia no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<MateriaResponse> update(
            @Parameter(description = "ID de la materia") @PathVariable Long id,
            @Valid @RequestBody MateriaUpdateRequest request) {
        MateriaResponse response = materiaService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar materia", description = "Elimina una materia. Falla si tiene cursos asociados (409 Conflict)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Materia eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Materia no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La materia tiene cursos asociados y no puede eliminarse",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la materia") @PathVariable Long id) {
        materiaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
