package co.uptc.materias.service;

import co.uptc.materias.dto.request.DocenteCreateRequest;
import co.uptc.materias.dto.request.DocenteUpdateRequest;
import co.uptc.materias.dto.response.DocenteResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.entity.Docente;
import co.uptc.materias.exception.DuplicateResourceException;
import co.uptc.materias.exception.ResourceInUseException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.repository.CursoRepository;
import co.uptc.materias.repository.DocenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocenteService")
class DocenteServiceTest {

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private CursoRepository cursoRepository;

    @InjectMocks
    private DocenteService docenteService;

    private Docente docente;

    @BeforeEach
    void setUp() {
        docente = new Docente(1L, "Carlos Andrés", "Ramírez López",
                "carlos.ramirez@uptc.edu.co", "Sistemas Distribuidos", true);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("debe crear docente exitosamente")
        void shouldCreateDocente() {
            DocenteCreateRequest request = new DocenteCreateRequest(
                    "Carlos Andrés", "Ramírez López",
                    "carlos.ramirez@uptc.edu.co", "Sistemas Distribuidos", true);
            when(docenteRepository.existsByCorreoInstitucional("carlos.ramirez@uptc.edu.co")).thenReturn(false);
            when(docenteRepository.save(any(Docente.class))).thenReturn(docente);

            DocenteResponse response = docenteService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.correoInstitucional()).isEqualTo("carlos.ramirez@uptc.edu.co");
        }

        @Test
        @DisplayName("debe lanzar DuplicateResourceException con correo duplicado")
        void shouldThrowWhenDuplicateEmail() {
            DocenteCreateRequest request = new DocenteCreateRequest(
                    "Otro", "Docente", "carlos.ramirez@uptc.edu.co", "Otra", true);
            when(docenteRepository.existsByCorreoInstitucional("carlos.ramirez@uptc.edu.co")).thenReturn(true);

            assertThatThrownBy(() -> docenteService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("carlos.ramirez@uptc.edu.co");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("debe retornar docente cuando existe")
        void shouldReturnDocenteWhenExists() {
            when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));

            DocenteResponse response = docenteService.findById(1L);

            assertThat(response.nombres()).isEqualTo("Carlos Andrés");
        }

        @Test
        @DisplayName("debe lanzar ResourceNotFoundException cuando no existe")
        void shouldThrowWhenNotFound() {
            when(docenteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> docenteService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("debe retornar página de docentes con filtros")
        @SuppressWarnings("unchecked")
        void shouldReturnPagedDocentes() {
            Page<Docente> page = new PageImpl<>(List.of(docente));
            when(docenteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            PageResponse<DocenteResponse> response = docenteService.findAll(
                    0, 10, "id", "asc", "Carlos", null, null, true);

            assertThat(response.content()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("debe actualizar docente exitosamente")
        void shouldUpdateDocente() {
            DocenteUpdateRequest request = new DocenteUpdateRequest(
                    "Carlos Andrés", "Ramírez López",
                    "carlos.ramirez@uptc.edu.co", "Cloud Computing", true);
            Docente updated = new Docente(1L, "Carlos Andrés", "Ramírez López",
                    "carlos.ramirez@uptc.edu.co", "Cloud Computing", true);

            when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
            when(docenteRepository.existsByCorreoInstitucionalAndIdNot("carlos.ramirez@uptc.edu.co", 1L)).thenReturn(false);
            when(docenteRepository.save(any(Docente.class))).thenReturn(updated);

            DocenteResponse response = docenteService.update(1L, request);

            assertThat(response.especialidad()).isEqualTo("Cloud Computing");
        }

        @Test
        @DisplayName("debe lanzar excepción al actualizar con correo duplicado de otro docente")
        void shouldThrowWhenUpdatingWithDuplicateEmail() {
            DocenteUpdateRequest request = new DocenteUpdateRequest(
                    "Carlos", "Ramírez", "otro.correo@uptc.edu.co", "Esp", true);
            when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
            when(docenteRepository.existsByCorreoInstitucionalAndIdNot("otro.correo@uptc.edu.co", 1L)).thenReturn(true);

            assertThatThrownBy(() -> docenteService.update(1L, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("debe eliminar docente sin cursos asociados")
        void shouldDeleteDocenteWithoutCursos() {
            when(docenteRepository.existsById(1L)).thenReturn(true);
            when(cursoRepository.existsByDocenteId(1L)).thenReturn(false);

            docenteService.delete(1L);

            verify(docenteRepository).deleteById(1L);
        }

        @Test
        @DisplayName("debe lanzar ResourceInUseException al eliminar docente con cursos")
        void shouldThrowWhenDocenteHasCursos() {
            when(docenteRepository.existsById(1L)).thenReturn(true);
            when(cursoRepository.existsByDocenteId(1L)).thenReturn(true);

            assertThatThrownBy(() -> docenteService.delete(1L))
                    .isInstanceOf(ResourceInUseException.class)
                    .hasMessageContaining("cursos asociados");
        }

        @Test
        @DisplayName("debe lanzar ResourceNotFoundException al eliminar docente inexistente")
        void shouldThrowWhenDeletingNonExistent() {
            when(docenteRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> docenteService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
