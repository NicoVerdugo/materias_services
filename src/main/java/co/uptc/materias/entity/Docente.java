package co.uptc.materias.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un docente del módulo Materias.
 * <p>
 * Un docente puede dictar múltiples cursos.
 * El correo institucional es único dentro del sistema.
 */
@Entity
@Table(name = "docentes")
public class Docente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(name = "correo_institucional", nullable = false, unique = true, length = 150)
    private String correoInstitucional;

    @Column(nullable = false, length = 150)
    private String especialidad;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "docente", fetch = FetchType.LAZY)
    private List<Curso> cursos = new ArrayList<>();

    public Docente() {
    }

    public Docente(Long id, String nombres, String apellidos, String correoInstitucional,
                   String especialidad, Boolean activo) {
        this.id = id;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correoInstitucional = correoInstitucional;
        this.especialidad = especialidad;
        this.activo = activo;
    }

    // --- Getters y Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreoInstitucional() {
        return correoInstitucional;
    }

    public void setCorreoInstitucional(String correoInstitucional) {
        this.correoInstitucional = correoInstitucional;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public List<Curso> getCursos() {
        return cursos;
    }

    public void setCursos(List<Curso> cursos) {
        this.cursos = cursos;
    }
}
