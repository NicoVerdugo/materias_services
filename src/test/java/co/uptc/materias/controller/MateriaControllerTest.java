package co.uptc.materias.controller;

import co.uptc.materias.dto.response.MateriaResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.exception.GlobalExceptionHandler;
import co.uptc.materias.exception.ResourceInUseException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.service.MateriaService;
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

@WebMvcTest(MateriaController.class)
@DisplayName("MateriaController")
class MateriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MateriaService materiaService;

    @Nested
    @DisplayName("POST /api/materias")
    class CreateMateria {

        @Test
        @DisplayName("debe retornar 201 con datos válidos")
        void shouldReturn201() throws Exception {
            MateriaResponse response = new MateriaResponse(1L, "Sistemas Distribuidos",
                    "Descripción", 4, "Ingeniería de Sistemas y Computación");
            when(materiaService.create(any())).thenReturn(response);

            mockMvc.perform(post("/api/materias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nombre": "Sistemas Distribuidos",
                                      "descripcion": "Descripción",
                                      "creditos": 4,
                                      "programa": "Ingeniería de Sistemas y Computación"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Sistemas Distribuidos"));
        }

        @Test
        @DisplayName("debe retornar 400 con nombre vacío")
        void shouldReturn400WhenNombreBlank() throws Exception {
            mockMvc.perform(post("/api/materias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nombre": "",
                                      "creditos": 4,
                                      "programa": "Programa"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("debe retornar 400 con créditos negativos")
        void shouldReturn400WhenCreditosNegative() throws Exception {
            mockMvc.perform(post("/api/materias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nombre": "Materia",
                                      "creditos": -1,
                                      "programa": "Programa"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/materias")
    class ListMaterias {

        @Test
        @DisplayName("debe retornar 200 con paginación")
        void shouldReturn200WithPagination() throws Exception {
            MateriaResponse materia = new MateriaResponse(1L, "Sistemas", "Desc", 4, "ISC");
            PageResponse<MateriaResponse> page = new PageResponse<>(List.of(materia), 0, 10, 1, 1);
            when(materiaService.findAll(anyInt(), anyInt(), anyString(), anyString(),
                    any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/materias")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.pageNumber").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/materias/{id}")
    class FindById {

        @Test
        @DisplayName("debe retornar 200 cuando existe")
        void shouldReturn200() throws Exception {
            MateriaResponse response = new MateriaResponse(1L, "Sistemas", "Desc", 4, "ISC");
            when(materiaService.findById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/materias/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("debe retornar 404 cuando no existe")
        void shouldReturn404() throws Exception {
            when(materiaService.findById(99L))
                    .thenThrow(new ResourceNotFoundException("Materia no encontrada con id: 99"));

            mockMvc.perform(get("/api/materias/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/materias/{id}")
    class DeleteMateria {

        @Test
        @DisplayName("debe retornar 204 al eliminar exitosamente")
        void shouldReturn204() throws Exception {
            mockMvc.perform(delete("/api/materias/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("debe retornar 409 cuando tiene cursos asociados")
        void shouldReturn409WhenHasCursos() throws Exception {
            doThrow(new ResourceInUseException("No se puede eliminar la materia"))
                    .when(materiaService).delete(1L);

            mockMvc.perform(delete("/api/materias/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }
}
