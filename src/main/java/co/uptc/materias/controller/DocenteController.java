package co.uptc.materias.controller;

import co.uptc.materias.dto.request.DocenteCreateRequest;
import co.uptc.materias.dto.request.DocenteUpdateRequest;
import co.uptc.materias.dto.response.DocenteResponse;
import co.uptc.materias.dto.response.ErrorResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.service.DocenteService;
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
 * Controlador REST para la gestión de Docentes.
 * <p>
 * El endpoint GET /api/docentes/{id} es clave para la futura integración
 * con el API Gateway durante la composición del detalle de un estudiante.
 */
@RestController
@RequestMapping("/api/docentes")
@Tag(name = "Docentes", description = "Gestión de docentes del módulo académico")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @Operation(summary = "Crear docente", description = "Registra un nuevo docente. El correo institucional debe ser único")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Docente creado exitosamente",
                    content = @Content(schema = @Schema(implementation = DocenteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El correo institucional ya está registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DocenteResponse> create(@Valid @RequestBody DocenteCreateRequest request) {
        DocenteResponse response = docenteService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar docentes", description = "Lista docentes con paginación, ordenamiento y filtros combinables")
    @ApiResponse(responseCode = "200", description = "Lista de docentes obtenida exitosamente")
    @GetMapping
    public ResponseEntity<PageResponse<DocenteResponse>> findAll(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Tamaño de página (máx. 100)") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Campo de ordenamiento: id, nombres, apellidos, especialidad") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Dirección: asc o desc") @RequestParam(defaultValue = "asc") String sortDirection,
            @Parameter(description = "Filtro parcial por nombres (case-insensitive)") @RequestParam(required = false) String nombres,
            @Parameter(description = "Filtro parcial por apellidos (case-insensitive)") @RequestParam(required = false) String apellidos,
            @Parameter(description = "Filtro parcial por especialidad (case-insensitive)") @RequestParam(required = false) String especialidad,
            @Parameter(description = "Filtro por estado activo") @RequestParam(required = false) Boolean activo) {

        PageResponse<DocenteResponse> response = docenteService.findAll(
                pageNumber, pageSize, sortBy, sortDirection, nombres, apellidos, especialidad, activo);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener docente por ID",
            description = "Consulta un docente específico. Endpoint clave para la composición del API Gateway")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Docente encontrado",
                    content = @Content(schema = @Schema(implementation = DocenteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Docente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DocenteResponse> findById(
            @Parameter(description = "ID del docente") @PathVariable Long id) {
        DocenteResponse response = docenteService.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualizar docente", description = "Actualiza todos los campos de un docente existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Docente actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = DocenteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Docente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El correo institucional ya está registrado por otro docente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<DocenteResponse> update(
            @Parameter(description = "ID del docente") @PathVariable Long id,
            @Valid @RequestBody DocenteUpdateRequest request) {
        DocenteResponse response = docenteService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar docente", description = "Elimina un docente. Falla si tiene cursos asociados (409 Conflict)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Docente eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Docente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El docente tiene cursos asociados y no puede eliminarse",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del docente") @PathVariable Long id) {
        docenteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
