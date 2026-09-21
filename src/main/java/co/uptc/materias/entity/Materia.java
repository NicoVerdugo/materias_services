package co.uptc.materias.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad principal del catálogo académico.
 * <p>
 * Representa una materia del plan de estudios de un programa académico.
 * Una materia puede tener múltiples cursos (ofertas concretas por periodo).
 */
@Entity
@Table(name = "materias")
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Integer creditos;

    @Column(nullable = false, length = 150)
    private String programa;

    @OneToMany(mappedBy = "materia", fetch = FetchType.LAZY)
    private List<Curso> cursos = new ArrayList<>();

    public Materia() {
    }

    public Materia(Long id, String nombre, String descripcion, Integer creditos, String programa) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creditos = creditos;
        this.programa = programa;
    }

    // --- Getters y Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCreditos() {
        return creditos;
    }

    public void setCreditos(Integer creditos) {
        this.creditos = creditos;
    }

    public String getPrograma() {
        return programa;
    }

    public void setPrograma(String programa) {
        this.programa = programa;
    }

    public List<Curso> getCursos() {
        return cursos;
    }

    public void setCursos(List<Curso> cursos) {
        this.cursos = cursos;
    }
}
