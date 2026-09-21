package co.uptc.materias.controller;

import co.uptc.materias.dto.response.DocenteResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.exception.DuplicateResourceException;
import co.uptc.materias.exception.ResourceInUseException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.service.DocenteService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocenteController.class)
@DisplayName("DocenteController")
class DocenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocenteService docenteService;

    @Nested
    @DisplayName("POST /api/docentes")
    class CreateDocente {

        @Test
        @DisplayName("debe retornar 201 con datos válidos")
        void shouldReturn201() throws Exception {
            DocenteResponse response = new DocenteResponse(1L, "Carlos", "Ramírez",
                    "carlos@uptc.edu.co", "Distribuidos", true);
            when(docenteService.create(any())).thenReturn(response);

            mockMvc.perform(post("/api/docentes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nombres": "Carlos",
                                      "apellidos": "Ramírez",
                                      "correoInstitucional": "carlos@uptc.edu.co",
                                      "especialidad": "Distribuidos",
                                      "activo": true
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("debe retornar 400 con correo inválido")
        void shouldReturn400WhenInvalidEmail() throws Exception {
            mockMvc.perform(post("/api/docentes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nombres": "Carlos",
                                      "apellidos": "Ramírez",
                                      "correoInstitucional": "correo-invalido",
                                      "especialidad": "Distribuidos",
                                      "activo": true
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("debe retornar 409 con correo duplicado")
        void shouldReturn409WhenDuplicate() throws Exception {
            when(docenteService.create(any()))
                    .thenThrow(new DuplicateResourceException("Correo ya existe"));

            mockMvc.perform(post("/api/docentes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nombres": "Carlos",
                                      "apellidos": "Ramírez",
                                      "correoInstitucional": "carlos@uptc.edu.co",
                                      "especialidad": "Distribuidos",
                                      "activo": true
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CONFLICT"));
        }

        @Test
        @DisplayName("debe retornar 400 con campos vacíos")
        void shouldReturn400WhenFieldsEmpty() throws Exception {
            mockMvc.perform(post("/api/docentes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nombres": "",
                                      "apellidos": "",
                                      "correoInstitucional": "",
                                      "especialidad": "",
                                      "activo": null
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/docentes/{id}")
    class FindById {

        @Test
        @DisplayName("debe retornar 200 cuando existe")
        void shouldReturn200() throws Exception {
            DocenteResponse response = new DocenteResponse(1L, "Carlos", "Ramírez",
                    "carlos@uptc.edu.co", "Distribuidos", true);
            when(docenteService.findById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/docentes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correoInstitucional").value("carlos@uptc.edu.co"));
        }

        @Test
        @DisplayName("debe retornar 404 cuando no existe")
        void shouldReturn404() throws Exception {
            when(docenteService.findById(99L))
                    .thenThrow(new ResourceNotFoundException("Docente no encontrado"));

            mockMvc.perform(get("/api/docentes/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/docentes")
    class ListDocentes {

        @Test
        @DisplayName("debe retornar 200 con paginación y filtros")
        void shouldReturn200() throws Exception {
            DocenteResponse docente = new DocenteResponse(1L, "Carlos", "Ramírez",
                    "carlos@uptc.edu.co", "Distribuidos", true);
            PageResponse<DocenteResponse> page = new PageResponse<>(List.of(docente), 0, 10, 1, 1);
            when(docenteService.findAll(anyInt(), anyInt(), anyString(), anyString(),
                    any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/docentes")
                            .param("activo", "true")
                            .param("especialidad", "Distribuidos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("DELETE /api/docentes/{id}")
    class DeleteDocente {

        @Test
        @DisplayName("debe retornar 409 cuando tiene cursos asociados")
        void shouldReturn409() throws Exception {
            doThrow(new ResourceInUseException("Docente tiene cursos"))
                    .when(docenteService).delete(1L);

            mockMvc.perform(delete("/api/docentes/1"))
                    .andExpect(status().isConflict());
        }
    }
}
