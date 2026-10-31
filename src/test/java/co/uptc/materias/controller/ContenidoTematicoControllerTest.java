package co.uptc.materias.controller;

import co.uptc.materias.dto.response.ContenidoTematicoResponse;
import co.uptc.materias.exception.ArchivoDemasiadoGrandeException;
import co.uptc.materias.exception.ArchivoInvalidoException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.service.ContenidoTematicoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContenidoTematicoController.class)
@DisplayName("ContenidoTematicoController")
class ContenidoTematicoControllerTest {

    private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContenidoTematicoService contenidoService;

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("archivo", "programa.pdf", "application/pdf", PDF);
    }

    @Nested
    @DisplayName("POST /api/materias/{id}/contenido-tematico")
    class Subir {

        @Test
        @DisplayName("debe retornar 201 con los metadatos")
        void shouldReturn201() throws Exception {
            when(contenidoService.subir(eq(1L), any())).thenReturn(new ContenidoTematicoResponse(
                    1L, 1L, "materias/1/contenido-tematico/abc.pdf", "programa.pdf",
                    "application/pdf", (long) PDF.length, "https://firmada"));

            mockMvc.perform(multipart("/api/materias/1/contenido-tematico").file(pdf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.claveS3").value("materias/1/contenido-tematico/abc.pdf"))
                    .andExpect(jsonPath("$.nombreOriginal").value("programa.pdf"))
                    .andExpect(jsonPath("$.tipoMime").value("application/pdf"))
                    .andExpect(jsonPath("$.tamanoBytes").value(PDF.length))
                    .andExpect(jsonPath("$.url").value("https://firmada"));
        }

        @Test
        @DisplayName("debe retornar 400 si el archivo no es PDF")
        void shouldReturn400WhenNotPdf() throws Exception {
            when(contenidoService.subir(eq(1L), any()))
                    .thenThrow(new ArchivoInvalidoException("Formato no permitido: el contenido temático debe ser un archivo PDF"));

            mockMvc.perform(multipart("/api/materias/1/contenido-tematico").file(pdf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Formato no permitido: el contenido temático debe ser un archivo PDF"));
        }

        @Test
        @DisplayName("debe retornar 413 si el archivo supera 10 MB")
        void shouldReturn413WhenTooLarge() throws Exception {
            when(contenidoService.subir(eq(1L), any()))
                    .thenThrow(new ArchivoDemasiadoGrandeException("El archivo supera el tamaño máximo permitido de 10 MB"));

            mockMvc.perform(multipart("/api/materias/1/contenido-tematico").file(pdf()))
                    .andExpect(status().isPayloadTooLarge())
                    .andExpect(jsonPath("$.error").value("PAYLOAD_TOO_LARGE"));
        }

        @Test
        @DisplayName("debe retornar 400 si falta la parte \"archivo\"")
        void shouldReturn400WhenPartMissing() throws Exception {
            MockMultipartFile otroCampo = new MockMultipartFile("documento", "programa.pdf", "application/pdf", PDF);

            mockMvc.perform(multipart("/api/materias/1/contenido-tematico").file(otroCampo))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(contenidoService);
        }

        @Test
        @DisplayName("debe retornar 415 si no se envía multipart/form-data")
        void shouldReturn415WhenNotMultipart() throws Exception {
            mockMvc.perform(post("/api/materias/1/contenido-tematico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnsupportedMediaType());
            verifyNoInteractions(contenidoService);
        }

        @Test
        @DisplayName("debe retornar 404 si la materia no existe")
        void shouldReturn404() throws Exception {
            when(contenidoService.subir(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Materia no encontrada con id: 99"));

            mockMvc.perform(multipart("/api/materias/99/contenido-tematico").file(pdf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/materias/{id}/contenido-tematico")
    class Listar {

        @Test
        @DisplayName("debe retornar 200 con la lista")
        void shouldReturn200() throws Exception {
            when(contenidoService.findByMateria(1L)).thenReturn(List.of(new ContenidoTematicoResponse(
                    1L, 1L, "materias/1/contenido-tematico/abc.pdf", "programa.pdf",
                    "application/pdf", 10L, "https://firmada")));

            mockMvc.perform(get("/api/materias/1/contenido-tematico"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].claveS3").value("materias/1/contenido-tematico/abc.pdf"));
        }
    }
}
