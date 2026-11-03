package co.uptc.materias.controller;

import co.uptc.materias.dto.response.ContenidoTematicoResponse;
import co.uptc.materias.dto.response.ErrorResponse;
import co.uptc.materias.service.ContenidoTematicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controlador REST para el contenido temático (PDF) de las materias.
 * <p>
 * Los archivos se envían directamente a AWS S3; en PostgreSQL solo se guardan sus metadatos.
 */
@RestController
@RequestMapping("/api/materias/{id}/contenido-tematico")
@Tag(name = "Contenido temático", description = "Archivos PDF de contenido temático de una materia (AWS S3)")
public class ContenidoTematicoController {

    private final ContenidoTematicoService contenidoService;

    public ContenidoTematicoController(ContenidoTematicoService contenidoService) {
        this.contenidoService = contenidoService;
    }

    @Operation(summary = "Subir contenido temático",
            description = "Sube un archivo PDF (máx. 10 MB) asociado a la materia. El archivo va directo a AWS S3.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Archivo subido exitosamente",
                    content = @Content(schema = @Schema(implementation = ContenidoTematicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falta el archivo o no es un PDF",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Materia no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "El archivo supera los 10 MB",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContenidoTematicoResponse> subir(
            @Parameter(description = "ID de la materia") @PathVariable Long id,
            @Parameter(description = "Archivo PDF") @RequestPart("archivo") MultipartFile archivo) {
        ContenidoTematicoResponse response = contenidoService.subir(id, archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar contenido temático",
            description = "Lista los archivos de la materia con una URL firmada temporal para descargarlos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Materia no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<ContenidoTematicoResponse>> findByMateria(
            @Parameter(description = "ID de la materia") @PathVariable Long id) {
        return ResponseEntity.ok(contenidoService.findByMateria(id));
    }
}
