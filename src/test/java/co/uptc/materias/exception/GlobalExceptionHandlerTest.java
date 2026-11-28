package co.uptc.materias.exception;

import co.uptc.materias.controller.CursoController;
import co.uptc.materias.dto.response.ErrorResponse;
import co.uptc.materias.service.CursoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Errores de la solicitud que antes caían en el handler genérico y respondían 500.
 */
@WebMvcTest(CursoController.class)
@DisplayName("GlobalExceptionHandler: errores de solicitud")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CursoService cursoService;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("método no soportado: 405 MetodoNoPermitido con encabezado Allow")
    void metodoNoPermitido() throws Exception {
        mockMvc.perform(delete("/api/materias/cursos"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("El método DELETE no está permitido en /api/materias/cursos"))
                .andExpect(jsonPath("$.path").value("/api/materias/cursos"))
                .andExpect(jsonPath("$.errorType").value("MetodoNoPermitido"))
                .andExpect(request().attribute("errorType", "MetodoNoPermitido"));
    }

    @Test
    @DisplayName("variable de ruta con tipo inválido (/cursos/abc): 400 ValidacionFallida")
    void tipoInvalido() throws Exception {
        mockMvc.perform(get("/api/materias/cursos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El parámetro 'id' tiene un valor inválido (se esperaba Long)"))
                .andExpect(jsonPath("$.path").value("/api/materias/cursos/abc"))
                .andExpect(jsonPath("$.errorType").value("ValidacionFallida"))
                .andExpect(request().attribute("errorType", "ValidacionFallida"));
    }

    @Test
    @DisplayName("parámetro de query con tipo inválido (pageNumber=x): 400 ValidacionFallida")
    void queryConTipoInvalido() throws Exception {
        mockMvc.perform(get("/api/materias/cursos").param("pageNumber", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("ValidacionFallida"));
    }

    @Test
    @DisplayName("parámetro obligatorio ausente: 400 ValidacionFallida")
    void parametroFaltante() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/materias/cursos");

        ResponseEntity<ErrorResponse> r = handler.handleParametroFaltante(
                new MissingServletRequestParameterException("periodo", "String"), req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().status()).isEqualTo(400);
        assertThat(r.getBody().error()).isEqualTo("BAD_REQUEST");
        assertThat(r.getBody().message()).isEqualTo("Falta el parámetro obligatorio 'periodo'");
        assertThat(r.getBody().path()).isEqualTo("/api/materias/cursos");
        assertThat(r.getBody().errorType()).isEqualTo("ValidacionFallida");
        assertThat(req.getAttribute("errorType")).isEqualTo("ValidacionFallida");
    }

    @Test
    @DisplayName("variable de ruta ausente: 400 ValidacionFallida")
    void variableRutaFaltante() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/materias/cursos/");
        MethodParameter parametro = new MethodParameter(
                CursoController.class.getMethod("findById", Long.class), 0);

        ResponseEntity<ErrorResponse> r = handler.handleVariableRutaFaltante(
                new MissingPathVariableException("id", parametro), req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().message()).isEqualTo("Falta la variable de ruta 'id'");
        assertThat(r.getBody().errorType()).isEqualTo("ValidacionFallida");
        assertThat(req.getAttribute("errorType")).isEqualTo("ValidacionFallida");
    }
}
