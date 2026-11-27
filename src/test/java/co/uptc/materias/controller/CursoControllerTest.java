package co.uptc.materias.controller;

import co.uptc.materias.dto.response.CursoResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.dto.response.ErrorResponse;
import co.uptc.materias.exception.CupoAgotadoException;
import co.uptc.materias.exception.GlobalExceptionHandler;
import co.uptc.materias.exception.ReglaNegocioException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.service.CursoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CursoController.class)
@DisplayName("CursoController")
class CursoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CursoService cursoService;

    @Nested
    @DisplayName("POST /api/materias/cursos")
    class CreateCurso {

        @Test
        @DisplayName("debe retornar 201 con datos válidos")
        void shouldReturn201() throws Exception {
            CursoResponse response = new CursoResponse(1L, 1L, "Sistemas Distribuidos",
                    1L, "Lunes 14:00-16:00", "2026-2", 30, 30, "Lab 204", "PRESENCIAL", "ACTIVO");
            when(cursoService.create(any())).thenReturn(response);

            mockMvc.perform(post("/api/materias/cursos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "materiaId": 1,
                                      "docenteId": 1,
                                      "horario": "Lunes 14:00-16:00",
                                      "periodo": "2026-2",
                                      "cupo": 30,
                                      "aula": "Lab 204",
                                      "modalidad": "PRESENCIAL",
                                      "estado": "ACTIVO"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.materiaNombre").value("Sistemas Distribuidos"))
                    .andExpect(jsonPath("$.cupoDisponible").value(30));
        }

        @Test
        @DisplayName("debe retornar 400 con cupo negativo")
        void shouldReturn400WhenCupoNegative() throws Exception {
            mockMvc.perform(post("/api/materias/cursos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "materiaId": 1,
                                      "docenteId": 1,
                                      "horario": "Lunes 14:00",
                                      "periodo": "2026-2",
                                      "cupo": -5,
                                      "aula": "Aula",
                                      "modalidad": "PRESENCIAL",
                                      "estado": "ACTIVO"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("debe retornar 400 con modalidad inválida")
        void shouldReturn400WhenInvalidModalidad() throws Exception {
            mockMvc.perform(post("/api/materias/cursos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "materiaId": 1,
                                      "docenteId": 1,
                                      "horario": "Lunes 14:00",
                                      "periodo": "2026-2",
                                      "cupo": 30,
                                      "aula": "Aula",
                                      "modalidad": "INVALIDA",
                                      "estado": "ACTIVO"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("debe retornar 404 cuando materia no existe")
        void shouldReturn404WhenMateriaNotFound() throws Exception {
            when(cursoService.create(any()))
                    .thenThrow(new ResourceNotFoundException("Materia no encontrada con id: 99"));

            mockMvc.perform(post("/api/materias/cursos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "materiaId": 99,
                                      "docenteId": 1,
                                      "horario": "Lunes 14:00",
                                      "periodo": "2026-2",
                                      "cupo": 30,
                                      "aula": "Aula",
                                      "modalidad": "PRESENCIAL",
                                      "estado": "ACTIVO"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Materia no encontrada con id: 99"));
        }
    }

    @Nested
    @DisplayName("GET /api/materias/cursos/{id}")
    class FindById {

        @Test
        @DisplayName("debe retornar 200 con materiaNombre incluido")
        void shouldReturn200WithMateriaNombre() throws Exception {
            CursoResponse response = new CursoResponse(1L, 3L, "Sistemas Distribuidos",
                    12L, "Lunes y Miércoles 14:00-16:00", "2026-2", 30, 18,
                    "Laboratorio 204", "PRESENCIAL", "ACTIVO");
            when(cursoService.findById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/materias/cursos/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.materiaNombre").value("Sistemas Distribuidos"))
                    .andExpect(jsonPath("$.materiaId").value(3))
                    .andExpect(jsonPath("$.docenteId").value(12))
                    .andExpect(jsonPath("$.modalidad").value("PRESENCIAL"))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"))
                    .andExpect(jsonPath("$.cupo").value(30))
                    .andExpect(jsonPath("$.cupoDisponible").value(18));
        }

        @Test
        @DisplayName("404 de curso conserva los campos de siempre y agrega errorType")
        void shouldReturn404WithErrorType() throws Exception {
            when(cursoService.findById(99L)).thenThrow(ResourceNotFoundException.curso(99L));

            mockMvc.perform(get("/api/materias/cursos/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Curso no encontrado con id: 99"))
                    .andExpect(jsonPath("$.path").value("/api/materias/cursos/99"))
                    .andExpect(jsonPath("$.errorType").value("CursoNoExiste"))
                    .andExpect(request().attribute("errorType", "CursoNoExiste"));
        }
    }

    @Nested
    @DisplayName("GET /api/materias/cursos")
    class ListCursos {

        @Test
        @DisplayName("debe retornar 200 con filtros por periodo y estado")
        void shouldReturn200WithFilters() throws Exception {
            CursoResponse curso = new CursoResponse(1L, 1L, "Sistemas", 1L,
                    "Lunes 14:00", "2026-2", 30, 30, "Lab", "PRESENCIAL", "ACTIVO");
            PageResponse<CursoResponse> page = new PageResponse<>(List.of(curso), 0, 10, 1, 1);
            when(cursoService.findAll(anyInt(), anyInt(), anyString(), anyString(),
                    any(), any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/materias/cursos")
                            .param("periodo", "2026-2")
                            .param("estado", "ACTIVO")
                            .param("sortBy", "periodo")
                            .param("sortDirection", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].periodo").value("2026-2"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/materias/cursos/{id}")
    class DeleteCurso {

        @Test
        @DisplayName("debe retornar 204 e inactivar el curso (no borrarlo)")
        void shouldReturn204() throws Exception {
            mockMvc.perform(delete("/api/materias/cursos/1"))
                    .andExpect(status().isNoContent());

            verify(cursoService).delete(1L);
        }

        @Test
        @DisplayName("debe retornar 404 CursoNoExiste si el curso no existe")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(ResourceNotFoundException.curso(99L)).when(cursoService).delete(99L);

            mockMvc.perform(delete("/api/materias/cursos/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType").value("CursoNoExiste"));
        }
    }

    @Nested
    @DisplayName("PUT /api/materias/cursos/{id}")
    class UpdateCurso {

        private static final String BODY_CUPO_10 = """
                {
                  "materiaId": 1,
                  "docenteId": 1,
                  "horario": "Lunes 14:00",
                  "periodo": "2026-2",
                  "cupo": 10,
                  "aula": "Aula",
                  "modalidad": "PRESENCIAL",
                  "estado": "ACTIVO"
                }
                """;

        @Test
        @DisplayName("debe retornar 200 con cupo y cupoDisponible ajustados")
        void shouldReturn200WithAdjustedCupo() throws Exception {
            when(cursoService.update(eq(1L), any())).thenReturn(
                    cursoResponse(10, 4, "ACTIVO"));

            mockMvc.perform(put("/api/materias/cursos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY_CUPO_10))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cupo").value(10))
                    .andExpect(jsonPath("$.cupoDisponible").value(4));
        }

        @Test
        @DisplayName("debe retornar 422 CupoMenorQueInscritos si el cupo no alcanza para los reservados")
        void shouldReturn422WhenCupoMenorQueInscritos() throws Exception {
            when(cursoService.update(eq(1L), any()))
                    .thenThrow(ReglaNegocioException.cupoMenorQueInscritos(1L, 10));

            mockMvc.perform(put("/api/materias/cursos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY_CUPO_10))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("UNPROCESSABLE_ENTITY"))
                    .andExpect(jsonPath("$.errorType").value("CupoMenorQueInscritos"))
                    .andExpect(request().attribute("errorType", "CupoMenorQueInscritos"));
        }
    }

    @Nested
    @DisplayName("POST /api/materias/cursos/{id}/reservar-cupo")
    class ReservarCupo {

        @Test
        @DisplayName("con cupo: 200 con el curso actualizado")
        void shouldReturn200() throws Exception {
            when(cursoService.reservarCupo(1L)).thenReturn(cursoResponse(30, 29, "ACTIVO"));

            mockMvc.perform(post("/api/materias/cursos/1/reservar-cupo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.cupoDisponible").value(29))
                    .andExpect(request().attribute("errorType", nullValue()));
        }

        @Test
        @DisplayName("sin cupo: 409 CupoAgotado")
        void shouldReturn409WhenCupoAgotado() throws Exception {
            when(cursoService.reservarCupo(1L)).thenThrow(CupoAgotadoException.agotado(1L));

            mockMvc.perform(post("/api/materias/cursos/1/reservar-cupo"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("CONFLICT"))
                    .andExpect(jsonPath("$.errorType").value("CupoAgotado"))
                    .andExpect(jsonPath("$.path").value("/api/materias/cursos/1/reservar-cupo"))
                    .andExpect(request().attribute("errorType", "CupoAgotado"));
        }

        @Test
        @DisplayName("curso inactivo: 422 CursoInactivo")
        void shouldReturn422WhenCursoInactivo() throws Exception {
            when(cursoService.reservarCupo(1L))
                    .thenThrow(ReglaNegocioException.cursoInactivo(1L, "INACTIVO"));

            mockMvc.perform(post("/api/materias/cursos/1/reservar-cupo"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errorType").value("CursoInactivo"))
                    .andExpect(request().attribute("errorType", "CursoInactivo"));
        }

        @Test
        @DisplayName("no existe: 404 CursoNoExiste")
        void shouldReturn404WhenNotFound() throws Exception {
            when(cursoService.reservarCupo(99L)).thenThrow(ResourceNotFoundException.curso(99L));

            mockMvc.perform(post("/api/materias/cursos/99/reservar-cupo"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType").value("CursoNoExiste"))
                    .andExpect(request().attribute("errorType", "CursoNoExiste"));
        }
    }

    @Nested
    @DisplayName("POST /api/materias/cursos/{id}/liberar-cupo")
    class LiberarCupo {

        @Test
        @DisplayName("con cupo ocupado: 200 con el curso actualizado")
        void shouldReturn200() throws Exception {
            when(cursoService.liberarCupo(1L)).thenReturn(cursoResponse(30, 30, "ACTIVO"));

            mockMvc.perform(post("/api/materias/cursos/1/liberar-cupo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cupoDisponible").value(30));
        }

        @Test
        @DisplayName("cupo ya completo: 409 CupoYaCompleto")
        void shouldReturn409WhenCupoYaCompleto() throws Exception {
            when(cursoService.liberarCupo(1L)).thenThrow(CupoAgotadoException.yaCompleto(1L));

            mockMvc.perform(post("/api/materias/cursos/1/liberar-cupo"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorType").value("CupoYaCompleto"))
                    .andExpect(request().attribute("errorType", "CupoYaCompleto"));
        }

        @Test
        @DisplayName("no existe: 404 CursoNoExiste")
        void shouldReturn404WhenNotFound() throws Exception {
            when(cursoService.liberarCupo(99L)).thenThrow(ResourceNotFoundException.curso(99L));

            mockMvc.perform(post("/api/materias/cursos/99/liberar-cupo"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType").value("CursoNoExiste"));
        }
    }

    @Test
    @DisplayName("errores sin errorType no incluyen el campo (contrato anterior intacto)")
    void errorsWithoutErrorTypeOmitField() throws Exception {
        when(cursoService.create(any()))
                .thenThrow(new ResourceNotFoundException("Materia no encontrada con id: 99"));

        mockMvc.perform(post("/api/materias/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"materiaId": 99, "docenteId": 1, "horario": "Lunes", "periodo": "2026-2",
                                 "cupo": 30, "aula": "Aula", "modalidad": "PRESENCIAL", "estado": "ACTIVO"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorType").doesNotExist())
                .andExpect(request().attribute("errorType", nullValue()));
    }

    @Nested
    @DisplayName("Ruta inexistente")
    class RutaInexistente {

        @Test
        @DisplayName("GET a una ruta sin endpoint: 404 RutaNoExiste con el ErrorResponse habitual")
        void shouldReturn404RutaNoExiste() throws Exception {
            mockMvc.perform(get("/api/materias/cursos/1/no-existe"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("No existe la ruta GET /api/materias/cursos/1/no-existe"))
                    .andExpect(jsonPath("$.path").value("/api/materias/cursos/1/no-existe"))
                    .andExpect(jsonPath("$.errorType").value("RutaNoExiste"))
                    .andExpect(request().attribute("errorType", "RutaNoExiste"));
        }

        @Test
        @DisplayName("POST a una ruta mal escrita (reservar-cupos): 404 RutaNoExiste, no 500")
        void shouldReturn404ForTypoInNewEndpoint() throws Exception {
            mockMvc.perform(post("/api/materias/cursos/1/reservar-cupos"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType").value("RutaNoExiste"));
        }

        @Test
        @DisplayName("NoHandlerFoundException también responde 404 RutaNoExiste")
        void shouldHandleNoHandlerFoundException() {
            MockHttpServletRequest req = new MockHttpServletRequest("DELETE", "/api/materias/otra");

            ResponseEntity<ErrorResponse> resp = new GlobalExceptionHandler().handleRutaNoExiste(
                    new NoHandlerFoundException("DELETE", "/api/materias/otra", new HttpHeaders()), req);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(resp.getBody().errorType()).isEqualTo("RutaNoExiste");
            assertThat(resp.getBody().path()).isEqualTo("/api/materias/otra");
            assertThat(req.getAttribute("errorType")).isEqualTo("RutaNoExiste");
        }
    }

    private static CursoResponse cursoResponse(int cupo, int cupoDisponible, String estado) {
        return new CursoResponse(1L, 1L, "Sistemas Distribuidos", 1L, "Lunes 14:00-16:00",
                "2026-2", cupo, cupoDisponible, "Lab 204", "PRESENCIAL", estado);
    }
}
