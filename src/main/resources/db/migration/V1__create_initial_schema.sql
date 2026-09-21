-- =============================================
-- V1: Esquema inicial del microservicio Materias
-- =============================================

-- Tabla de materias (catálogo académico)
CREATE TABLE materias (
    id          BIGSERIAL       PRIMARY KEY,
    nombre      VARCHAR(150)    NOT NULL,
    descripcion TEXT,
    creditos    INTEGER         NOT NULL CHECK (creditos > 0),
    programa    VARCHAR(150)    NOT NULL
);

-- Tabla de docentes
CREATE TABLE docentes (
    id                      BIGSERIAL       PRIMARY KEY,
    nombres                 VARCHAR(100)    NOT NULL,
    apellidos               VARCHAR(100)    NOT NULL,
    correo_institucional    VARCHAR(150)    NOT NULL UNIQUE,
    especialidad            VARCHAR(150)    NOT NULL,
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE
);

-- Tabla de cursos (oferta académica)
-- Relaciones internas: materia_id → materias, docente_id → docentes
-- NO contiene foreign keys hacia otros microservicios
CREATE TABLE cursos (
    id          BIGSERIAL       PRIMARY KEY,
    materia_id  BIGINT          NOT NULL REFERENCES materias(id),
    docente_id  BIGINT          NOT NULL REFERENCES docentes(id),
    horario     VARCHAR(200)    NOT NULL,
    periodo     VARCHAR(20)     NOT NULL,
    cupo        INTEGER         NOT NULL CHECK (cupo > 0),
    aula        VARCHAR(50)     NOT NULL,
    modalidad   VARCHAR(20)     NOT NULL CHECK (modalidad IN ('PRESENCIAL', 'VIRTUAL', 'HIBRIDA')),
    estado      VARCHAR(20)     NOT NULL CHECK (estado IN ('ACTIVO', 'INACTIVO', 'CANCELADO'))
);

-- Índices para mejorar rendimiento de filtros frecuentes
CREATE INDEX idx_cursos_materia_id ON cursos(materia_id);
CREATE INDEX idx_cursos_docente_id ON cursos(docente_id);
CREATE INDEX idx_cursos_periodo ON cursos(periodo);
CREATE INDEX idx_cursos_estado ON cursos(estado);
