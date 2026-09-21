package co.uptc.materias.service;

import co.uptc.materias.dto.request.MateriaCreateRequest;
import co.uptc.materias.dto.request.MateriaUpdateRequest;
import co.uptc.materias.dto.response.MateriaResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.entity.Materia;
import co.uptc.materias.exception.ResourceInUseException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.repository.CursoRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MateriaService")
class MateriaServiceTest {

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private CursoRepository cursoRepository;

    @InjectMocks
    private MateriaService materiaService;

    private Materia materia;

    @BeforeEach
    void setUp() {
        materia = new Materia(1L, "Sistemas Distribuidos",
                "Estudio de sistemas distribuidos", 4, "Ingeniería de Sistemas y Computación");
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("debe crear una materia exitosamente")
        void shouldCreateMateria() {
            MateriaCreateRequest request = new MateriaCreateRequest(
                    "Sistemas Distribuidos", "Estudio de sistemas distribuidos",
                    4, "Ingeniería de Sistemas y Computación");
            when(materiaRepository.save(any(Materia.class))).thenReturn(materia);

            MateriaResponse response = materiaService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.nombre()).isEqualTo("Sistemas Distribuidos");
            assertThat(response.creditos()).isEqualTo(4);
            verify(materiaRepository).save(any(Materia.class));
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("debe retornar materia cuando existe")
        void shouldReturnMateriaWhenExists() {
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));

            MateriaResponse response = materiaService.findById(1L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.nombre()).isEqualTo("Sistemas Distribuidos");
        }

        @Test
        @DisplayName("debe lanzar ResourceNotFoundException cuando no existe")
        void shouldThrowWhenNotFound() {
            when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> materiaService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("debe retornar página de materias")
        @SuppressWarnings("unchecked")
        void shouldReturnPagedMaterias() {
            Page<Materia> page = new PageImpl<>(List.of(materia));
            when(materiaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            PageResponse<MateriaResponse> response = materiaService.findAll(
                    0, 10, "id", "asc", null, null, null);

            assertThat(response).isNotNull();
            assertThat(response.content()).hasSize(1);
            assertThat(response.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("debe limitar pageSize a 100")
        @SuppressWarnings("unchecked")
        void shouldLimitPageSize() {
            Page<Materia> page = new PageImpl<>(List.of());
            when(materiaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            materiaService.findAll(0, 500, "id", "asc", null, null, null);

            verify(materiaRepository).findAll(any(Specification.class), argThat((Pageable p) -> p.getPageSize() == 100));
        }

        @Test
        @DisplayName("debe usar campo default cuando sortBy es inválido")
        @SuppressWarnings("unchecked")
        void shouldUseDefaultSortField() {
            Page<Materia> page = new PageImpl<>(List.of());
            when(materiaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            materiaService.findAll(0, 10, "campoInvalido", "asc", null, null, null);

            verify(materiaRepository).findAll(any(Specification.class),
                    argThat((Pageable p) -> p.getSort().getOrderFor("id") != null));
        }

        @Test
        @DisplayName("debe aplicar filtros correctamente")
        @SuppressWarnings("unchecked")
        void shouldApplyFilters() {
            Page<Materia> page = new PageImpl<>(List.of(materia));
            when(materiaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            PageResponse<MateriaResponse> response = materiaService.findAll(
                    0, 10, "nombre", "desc", "Sistemas", "Ingeniería", 4);

            assertThat(response.content()).hasSize(1);
            verify(materiaRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("debe actualizar materia exitosamente")
        void shouldUpdateMateria() {
            MateriaUpdateRequest request = new MateriaUpdateRequest(
                    "Sistemas Distribuidos Avanzados", "Descripción actualizada",
                    4, "Ingeniería de Sistemas y Computación");
            Materia updated = new Materia(1L, "Sistemas Distribuidos Avanzados",
                    "Descripción actualizada", 4, "Ingeniería de Sistemas y Computación");

            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
            when(materiaRepository.save(any(Materia.class))).thenReturn(updated);

            MateriaResponse response = materiaService.update(1L, request);

            assertThat(response.nombre()).isEqualTo("Sistemas Distribuidos Avanzados");
            verify(materiaRepository).save(any(Materia.class));
        }

        @Test
        @DisplayName("debe lanzar excepción al actualizar materia inexistente")
        void shouldThrowWhenUpdatingNonExistent() {
            MateriaUpdateRequest request = new MateriaUpdateRequest(
                    "Nombre", "Desc", 3, "Programa");
            when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> materiaService.update(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("debe eliminar materia sin cursos asociados")
        void shouldDeleteMateriaWithoutCursos() {
            when(materiaRepository.existsById(1L)).thenReturn(true);
            when(cursoRepository.existsByMateriaId(1L)).thenReturn(false);

            materiaService.delete(1L);

            verify(materiaRepository).deleteById(1L);
        }

        @Test
        @DisplayName("debe lanzar ResourceNotFoundException al eliminar materia inexistente")
        void shouldThrowWhenDeletingNonExistent() {
            when(materiaRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> materiaService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("debe lanzar ResourceInUseException al eliminar materia con cursos")
        void shouldThrowWhenMateriaHasCursos() {
            when(materiaRepository.existsById(1L)).thenReturn(true);
            when(cursoRepository.existsByMateriaId(1L)).thenReturn(true);

            assertThatThrownBy(() -> materiaService.delete(1L))
                    .isInstanceOf(ResourceInUseException.class)
                    .hasMessageContaining("cursos asociados");
        }
    }
}
