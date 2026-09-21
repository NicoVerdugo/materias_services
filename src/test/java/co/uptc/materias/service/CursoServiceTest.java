package co.uptc.materias.service;

import co.uptc.materias.dto.request.CursoCreateRequest;
import co.uptc.materias.dto.request.CursoUpdateRequest;
import co.uptc.materias.dto.response.CursoResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.entity.*;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.repository.CursoRepository;
import co.uptc.materias.repository.DocenteRepository;
import co.uptc.materias.repository.MateriaRepository;
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
@DisplayName("CursoService")
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @InjectMocks
    private CursoService cursoService;

    private Materia materia;
    private Docente docente;
    private Curso curso;

    @BeforeEach
    void setUp() {
        materia = new Materia(1L, "Sistemas Distribuidos", "Descripción", 4, "Ingeniería de Sistemas y Computación");
        docente = new Docente(1L, "Carlos", "Ramírez", "carlos@uptc.edu.co", "Distribuidos", true);
        curso = new Curso(1L, materia, docente, "Lunes 14:00-16:00", "2026-2",
                30, "Lab 204", Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("debe crear curso exitosamente cuando materia y docente existen")
        void shouldCreateCurso() {
            CursoCreateRequest request = new CursoCreateRequest(
                    1L, 1L, "Lunes 14:00-16:00", "2026-2", 30, "Lab 204",
                    Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
            when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
            when(cursoRepository.save(any(Curso.class))).thenReturn(curso);

            CursoResponse response = cursoService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.materiaNombre()).isEqualTo("Sistemas Distribuidos");
            assertThat(response.docenteId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("debe lanzar excepción cuando la materia no existe")
        void shouldThrowWhenMateriaNotFound() {
            CursoCreateRequest request = new CursoCreateRequest(
                    99L, 1L, "Horario", "2026-2", 30, "Aula",
                    Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Materia");
        }

        @Test
        @DisplayName("debe lanzar excepción cuando el docente no existe")
        void shouldThrowWhenDocenteNotFound() {
            CursoCreateRequest request = new CursoCreateRequest(
                    1L, 99L, "Horario", "2026-2", 30, "Aula",
                    Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
            when(docenteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Docente");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("debe retornar curso con materiaNombre cuando existe")
        void shouldReturnCursoWithMateriaNombre() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));

            CursoResponse response = cursoService.findById(1L);

            assertThat(response.materiaNombre()).isEqualTo("Sistemas Distribuidos");
            assertThat(response.materiaId()).isEqualTo(1L);
            assertThat(response.docenteId()).isEqualTo(1L);
            assertThat(response.modalidad()).isEqualTo("PRESENCIAL");
            assertThat(response.estado()).isEqualTo("ACTIVO");
        }

        @Test
        @DisplayName("debe lanzar ResourceNotFoundException cuando no existe")
        void shouldThrowWhenNotFound() {
            when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("debe retornar página de cursos con filtros")
        @SuppressWarnings("unchecked")
        void shouldReturnPagedCursos() {
            Page<Curso> page = new PageImpl<>(List.of(curso));
            when(cursoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            PageResponse<CursoResponse> response = cursoService.findAll(
                    0, 10, "id", "asc", "2026-2", EstadoCurso.ACTIVO, null, null, null);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().getFirst().materiaNombre()).isEqualTo("Sistemas Distribuidos");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("debe actualizar curso exitosamente")
        void shouldUpdateCurso() {
            CursoUpdateRequest request = new CursoUpdateRequest(
                    1L, 1L, "Martes 10:00-12:00", "2026-2", 35, "Aula 301",
                    Modalidad.HIBRIDA, EstadoCurso.ACTIVO);
            Curso updated = new Curso(1L, materia, docente, "Martes 10:00-12:00", "2026-2",
                    35, "Aula 301", Modalidad.HIBRIDA, EstadoCurso.ACTIVO);

            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
            when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
            when(cursoRepository.save(any(Curso.class))).thenReturn(updated);

            CursoResponse response = cursoService.update(1L, request);

            assertThat(response.horario()).isEqualTo("Martes 10:00-12:00");
            assertThat(response.cupo()).isEqualTo(35);
            assertThat(response.modalidad()).isEqualTo("HIBRIDA");
        }

        @Test
        @DisplayName("debe lanzar excepción al actualizar curso inexistente")
        void shouldThrowWhenCursoNotFound() {
            CursoUpdateRequest request = new CursoUpdateRequest(
                    1L, 1L, "Horario", "2026-2", 30, "Aula",
                    Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.update(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("debe eliminar curso exitosamente")
        void shouldDeleteCurso() {
            when(cursoRepository.existsById(1L)).thenReturn(true);

            cursoService.delete(1L);

            verify(cursoRepository).deleteById(1L);
        }

        @Test
        @DisplayName("debe lanzar ResourceNotFoundException al eliminar curso inexistente")
        void shouldThrowWhenDeletingNonExistent() {
            when(cursoRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> cursoService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
