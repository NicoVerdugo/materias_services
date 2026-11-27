package co.uptc.materias.entity;

import jakarta.persistence.*;

/**
 * Entidad que representa una oferta concreta de una Materia en un periodo académico.
 * <p>
 * Un curso asocia una Materia con un Docente, e incluye información de
 * horario, aula, capacidad, modalidad y estado.
 * Esta entidad es clave para la futura integración con el API Gateway,
 * ya que el endpoint GET /api/materias/cursos/{id} será consultado durante la
 * composición del detalle de un estudiante.
 */
@Entity
@Table(name = "cursos")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @Column(nullable = false, length = 200)
    private String horario;

    @Column(nullable = false, length = 20)
    private String periodo;

    /**
     * Capacidad del curso. {@code updatable = false}: Hibernate no la escribe al
     * guardar la entidad; solo cambia con la sentencia condicional
     * {@code CursoRepository.ajustarCupo}, que mueve a la vez {@code cupoDisponible}.
     */
    @Column(nullable = false, updatable = false)
    private Integer cupo;

    /**
     * Puestos libres. {@code updatable = false}: solo cambia con las sentencias
     * UPDATE atómicas del repositorio (reservar, liberar, ajustar). Así un PUT o un
     * save() con la entidad leída antes nunca pisa un descuento hecho en paralelo.
     */
    @Column(name = "cupo_disponible", nullable = false, updatable = false)
    private Integer cupoDisponible;

    @Column(nullable = false, length = 50)
    private String aula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidad modalidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCurso estado;

    public Curso() {
    }

    /**
     * Crea un curso sin inscritos: {@code cupoDisponible = cupo}.
     */
    public Curso(Long id, Materia materia, Docente docente, String horario, String periodo,
                 Integer cupo, String aula, Modalidad modalidad, EstadoCurso estado) {
        this(id, materia, docente, horario, periodo, cupo, cupo, aula, modalidad, estado);
    }

    public Curso(Long id, Materia materia, Docente docente, String horario, String periodo,
                 Integer cupo, Integer cupoDisponible, String aula, Modalidad modalidad,
                 EstadoCurso estado) {
        this.id = id;
        this.materia = materia;
        this.docente = docente;
        this.horario = horario;
        this.periodo = periodo;
        this.cupo = cupo;
        this.cupoDisponible = cupoDisponible;
        this.aula = aula;
        this.modalidad = modalidad;
        this.estado = estado;
    }

    // --- Getters y Setters ---

    public Integer getCupoDisponible() {
        return cupoDisponible;
    }

    public void setCupoDisponible(Integer cupoDisponible) {
        this.cupoDisponible = cupoDisponible;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Materia getMateria() {
        return materia;
    }

    public void setMateria(Materia materia) {
        this.materia = materia;
    }

    public Docente getDocente() {
        return docente;
    }

    public void setDocente(Docente docente) {
        this.docente = docente;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public Integer getCupo() {
        return cupo;
    }

    public void setCupo(Integer cupo) {
        this.cupo = cupo;
    }

    public String getAula() {
        return aula;
    }

    public void setAula(String aula) {
        this.aula = aula;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public void setModalidad(Modalidad modalidad) {
        this.modalidad = modalidad;
    }

    public EstadoCurso getEstado() {
        return estado;
    }

    public void setEstado(EstadoCurso estado) {
        this.estado = estado;
    }
}
