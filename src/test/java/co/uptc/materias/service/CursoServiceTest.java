package co.uptc.materias.service;

import co.uptc.materias.dto.request.CursoCreateRequest;
import co.uptc.materias.dto.request.CursoUpdateRequest;
import co.uptc.materias.dto.response.CursoResponse;
import co.uptc.materias.dto.response.PageResponse;
import co.uptc.materias.entity.*;
import co.uptc.materias.exception.CupoAgotadoException;
import co.uptc.materias.exception.ReglaNegocioException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.repository.CursoRepository;
import co.uptc.materias.repository.DocenteRepository;
import co.uptc.materias.repository.MateriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
        @DisplayName("debe crear el curso con cupoDisponible igual a cupo")
        void shouldInitCupoDisponibleWithCupo() {
            CursoCreateRequest request = new CursoCreateRequest(
                    1L, 1L, "Lunes 14:00-16:00", "2026-2", 25, "Lab 204",
                    Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
            when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
            when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

            CursoResponse response = cursoService.create(request);

            ArgumentCaptor<Curso> captor = ArgumentCaptor.forClass(Curso.class);
            verify(cursoRepository).save(captor.capture());
            assertThat(captor.getValue().getCupo()).isEqualTo(25);
            assertThat(captor.getValue().getCupoDisponible()).isEqualTo(25);
            assertThat(response.cupoDisponible()).isEqualTo(25);
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
        @DisplayName("sin cambio de cupo: actualiza los demás campos y no ajusta el cupo")
        void shouldUpdateCursoWithoutTouchingCupo() {
            CursoUpdateRequest request = new CursoUpdateRequest(
                    1L, 1L, "Martes 10:00-12:00", "2026-2", 30, "Aula 301",
                    Modalidad.HIBRIDA, EstadoCurso.ACTIVO);

            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
            when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));

            CursoResponse response = cursoService.update(1L, request);

            assertThat(response.horario()).isEqualTo("Martes 10:00-12:00");
            assertThat(response.cupo()).isEqualTo(30);
            assertThat(response.modalidad()).isEqualTo("HIBRIDA");
            verify(cursoRepository).save(curso);
            verify(cursoRepository, never()).ajustarCupo(anyLong(), anyInt());
        }

        @Test
        @DisplayName("con cambio de cupo: ajusta cupo y cupoDisponible con la sentencia condicional")
        void shouldAdjustCupoWhenItChanges() {
            // Curso con cupo 30 y 12 disponibles (18 reservados) que pasa a cupo 35
            Curso actual = new Curso(1L, materia, docente, "Lunes 14:00-16:00", "2026-2",
                    30, 12, "Lab 204", Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            Curso ajustado = new Curso(1L, materia, docente, "Lunes 14:00-16:00", "2026-2",
                    35, 17, "Lab 204", Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            CursoUpdateRequest request = new CursoUpdateRequest(
                    1L, 1L, "Lunes 14:00-16:00", "2026-2", 35, "Lab 204",
                    Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);

            when(cursoRepository.findById(1L)).thenReturn(Optional.of(actual), Optional.of(ajustado));
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
            when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
            when(cursoRepository.ajustarCupo(1L, 35)).thenReturn(1);

            CursoResponse response = cursoService.update(1L, request);

            verify(cursoRepository).ajustarCupo(1L, 35);
            assertThat(response.cupo()).isEqualTo(35);
            assertThat(response.cupoDisponible()).isEqualTo(17);
        }

        @Test
        @DisplayName("bajar el cupo por debajo de los reservados lanza CupoMenorQueInscritos")
        void shouldThrowWhenCupoMenorQueInscritos() {
            // 30 de cupo, 2 disponibles => 28 reservados; bajar a 20 dejaría -8
            Curso actual = new Curso(1L, materia, docente, "Lunes 14:00-16:00", "2026-2",
                    30, 2, "Lab 204", Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            CursoUpdateRequest request = new CursoUpdateRequest(
                    1L, 1L, "Lunes 14:00-16:00", "2026-2", 20, "Lab 204",
                    Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);

            when(cursoRepository.findById(1L)).thenReturn(Optional.of(actual));
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
            when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
            when(cursoRepository.ajustarCupo(1L, 20)).thenReturn(0);

            assertThatThrownBy(() -> cursoService.update(1L, request))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("errorType").isEqualTo("CupoMenorQueInscritos");
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
        @DisplayName("debe pasar el curso a INACTIVO sin borrar la fila")
        void shouldInactivateCurso() {
            when(cursoRepository.inactivar(1L)).thenReturn(1);

            cursoService.delete(1L);

            verify(cursoRepository).inactivar(1L);
            verify(cursoRepository, never()).deleteById(anyLong());
            verify(cursoRepository, never()).delete(any(Curso.class));
        }

        @Test
        @DisplayName("debe lanzar ResourceNotFoundException (CursoNoExiste) al eliminar curso inexistente")
        void shouldThrowWhenDeletingNonExistent() {
            when(cursoRepository.inactivar(99L)).thenReturn(0);

            assertThatThrownBy(() -> cursoService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorType").isEqualTo("CursoNoExiste");
        }
    }

    @Nested
    @DisplayName("reservarCupo")
    class ReservarCupo {

        @Test
        @DisplayName("con cupo: descuenta con la sentencia atómica y devuelve el curso releído")
        void shouldReserve() {
            Curso despues = new Curso(1L, materia, docente, "Lunes 14:00-16:00", "2026-2",
                    30, 29, "Lab 204", Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            when(cursoRepository.reservarCupo(1L)).thenReturn(1);
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(despues));

            CursoResponse response = cursoService.reservarCupo(1L);

            assertThat(response.cupoDisponible()).isEqualTo(29);
            verify(cursoRepository, never()).save(any());
        }

        @Test
        @DisplayName("sin cupo: CupoAgotado")
        void shouldThrowCupoAgotado() {
            Curso lleno = new Curso(1L, materia, docente, "Lunes 14:00-16:00", "2026-2",
                    30, 0, "Lab 204", Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            when(cursoRepository.reservarCupo(1L)).thenReturn(0);
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(lleno));

            assertThatThrownBy(() -> cursoService.reservarCupo(1L))
                    .isInstanceOf(CupoAgotadoException.class)
                    .extracting("errorType").isEqualTo("CupoAgotado");
        }

        @Test
        @DisplayName("curso inactivo (aunque tenga cupo): CursoInactivo")
        void shouldThrowCursoInactivo() {
            Curso inactivo = new Curso(1L, materia, docente, "Lunes 14:00-16:00", "2026-2",
                    30, 10, "Lab 204", Modalidad.PRESENCIAL, EstadoCurso.INACTIVO);
            when(cursoRepository.reservarCupo(1L)).thenReturn(0);
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(inactivo));

            assertThatThrownBy(() -> cursoService.reservarCupo(1L))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("errorType").isEqualTo("CursoInactivo");
        }

        @Test
        @DisplayName("curso cancelado: CursoInactivo")
        void shouldThrowCursoInactivoWhenCancelado() {
            Curso cancelado = new Curso(1L, materia, docente, "Lunes 14:00-16:00", "2026-2",
                    30, 10, "Lab 204", Modalidad.PRESENCIAL, EstadoCurso.CANCELADO);
            when(cursoRepository.reservarCupo(1L)).thenReturn(0);
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(cancelado));

            assertThatThrownBy(() -> cursoService.reservarCupo(1L))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("errorType").isEqualTo("CursoInactivo");
        }

        @Test
        @DisplayName("no existe: CursoNoExiste")
        void shouldThrowCursoNoExiste() {
            when(cursoRepository.reservarCupo(99L)).thenReturn(0);
            when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.reservarCupo(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorType").isEqualTo("CursoNoExiste");
        }
    }

    @Nested
    @DisplayName("liberarCupo")
    class LiberarCupo {

        @Test
        @DisplayName("con cupo ocupado: devuelve el cupo y el curso releído")
        void shouldRelease() {
            Curso despues = new Curso(1L, materia, docente, "Lunes 14:00-16:00", "2026-2",
                    30, 30, "Lab 204", Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
            when(cursoRepository.liberarCupo(1L)).thenReturn(1);
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(despues));

            CursoResponse response = cursoService.liberarCupo(1L);

            assertThat(response.cupoDisponible()).isEqualTo(30);
        }

        @Test
        @DisplayName("cupo ya completo: CupoYaCompleto")
        void shouldThrowCupoYaCompleto() {
            when(cursoRepository.liberarCupo(1L)).thenReturn(0);
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));

            assertThatThrownBy(() -> cursoService.liberarCupo(1L))
                    .isInstanceOf(CupoAgotadoException.class)
                    .extracting("errorType").isEqualTo("CupoYaCompleto");
        }

        @Test
        @DisplayName("no existe: CursoNoExiste")
        void shouldThrowCursoNoExiste() {
            when(cursoRepository.liberarCupo(99L)).thenReturn(0);
            when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.liberarCupo(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorType").isEqualTo("CursoNoExiste");
        }
    }
}
