package co.uptc.materias.controller;

import co.uptc.materias.dto.response.CursoResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.service.CursoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
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
                    1L, "Lunes 14:00-16:00", "2026-2", 30, "Lab 204", "PRESENCIAL", "ACTIVO");
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
                    .andExpect(jsonPath("$.materiaNombre").value("Sistemas Distribuidos"));
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
                    12L, "Lunes y Miércoles 14:00-16:00", "2026-2", 30,
                    "Laboratorio 204", "PRESENCIAL", "ACTIVO");
            when(cursoService.findById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/materias/cursos/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.materiaNombre").value("Sistemas Distribuidos"))
                    .andExpect(jsonPath("$.materiaId").value(3))
                    .andExpect(jsonPath("$.docenteId").value(12))
                    .andExpect(jsonPath("$.modalidad").value("PRESENCIAL"))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"));
        }
    }

    @Nested
    @DisplayName("GET /api/materias/cursos")
    class ListCursos {

        @Test
        @DisplayName("debe retornar 200 con filtros por periodo y estado")
        void shouldReturn200WithFilters() throws Exception {
            CursoResponse curso = new CursoResponse(1L, 1L, "Sistemas", 1L,
                    "Lunes 14:00", "2026-2", 30, "Lab", "PRESENCIAL", "ACTIVO");
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
        @DisplayName("debe retornar 204 al eliminar exitosamente")
        void shouldReturn204() throws Exception {
            mockMvc.perform(delete("/api/materias/cursos/1"))
                    .andExpect(status().isNoContent());
        }
    }
}
