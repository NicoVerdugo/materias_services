# Materias Service

Microservicio independiente para la gestión de **Materias**, **Docentes** y **Cursos** del módulo académico.  
Laboratorio 3 — Arquitectura Modular con API Gateway · Sistemas Distribuidos · UPTC.

---

## Descripción

Este microservicio administra el catálogo académico completo: materias del plan de estudios, docentes que las imparten y la oferta de cursos por periodo académico. Funciona de forma completamente independiente, con su propia base de datos, y está preparado para ser consumido por un API Gateway.

**No incluye frontend.** La interacción se realiza exclusivamente mediante:
- Swagger UI / OpenAPI
- Peticiones HTTP REST
- Base de datos PostgreSQL

---

## Tecnologías

| Tecnología | Versión |
|------------|---------|
| Java | 21 |
| Spring Boot | 3.3.4 |
| Spring Data JPA | (incluida en Boot) |
| PostgreSQL | 16 |
| Flyway | (incluida en Boot) |
| Jakarta Bean Validation | (incluida en Boot) |
| Springdoc OpenAPI | 2.6.0 |
| Maven | 3.9+ |
| Docker Compose | v2 |
| JUnit 5 + Mockito | (incluidas en Boot) |

---

## Arquitectura

Arquitectura por capas con separación estricta de responsabilidades:

```
Controller → Service → Repository → PostgreSQL
     ↕           ↕
   DTOs       Entities
   (records)   (JPA)
```

| Capa | Responsabilidad |
|------|-----------------|
| **Controller** | HTTP, validación de entrada con `@Valid`, delegación al Service |
| **Service** | Lógica de negocio, reglas de eliminación, validación de relaciones |
| **Repository** | Acceso a datos con Spring Data JPA + `JpaSpecificationExecutor` |
| **Mapper** | Conversión Entity ↔ DTO |
| **Specification** | Filtros dinámicos con Criteria API |
| **Exception** | Manejo global de errores (`@RestControllerAdvice`) |

---

## Modelo de Datos

```
Materia (1) ──── (N) Curso (N) ──── (1) Docente
```

### Materia
| Campo | Tipo | Restricciones |
|-------|------|---------------|
| id | Long | PK, auto-generado |
| nombre | String | NOT NULL, max 150 |
| descripcion | String | TEXT, nullable |
| creditos | Integer | NOT NULL, > 0 |
| programa | String | NOT NULL, max 150 |

### Docente
| Campo | Tipo | Restricciones |
|-------|------|---------------|
| id | Long | PK, auto-generado |
| nombres | String | NOT NULL, max 100 |
| apellidos | String | NOT NULL, max 100 |
| correoInstitucional | String | NOT NULL, UNIQUE, max 150 |
| especialidad | String | NOT NULL, max 150 |
| activo | Boolean | NOT NULL, default TRUE |

### Curso
| Campo | Tipo | Restricciones |
|-------|------|---------------|
| id | Long | PK, auto-generado |
| materiaId | Long | FK → materias, NOT NULL |
| docenteId | Long | FK → docentes, NOT NULL |
| horario | String | NOT NULL, max 200 |
| periodo | String | NOT NULL, max 20 |
| cupo | Integer | NOT NULL, > 0 |
| aula | String | NOT NULL, max 50 |
| modalidad | Enum | PRESENCIAL, VIRTUAL, HIBRIDA |
| estado | Enum | ACTIVO, INACTIVO, CANCELADO |

**Nota:** No existen relaciones hacia otros microservicios (Estudiantes, Inscripciones).

---

## Base de Datos

- **Motor:** PostgreSQL 16
- **Base de datos:** `materias_db`
- **Migraciones:** Flyway (automáticas al iniciar)
  - `V1__create_initial_schema.sql` — Crea tablas y restricciones
  - `V2__insert_sample_data.sql` — Datos de prueba

---

## Variables de Entorno

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `DB_HOST` | Host de PostgreSQL | `localhost` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `materias_db` |
| `DB_USERNAME` | Usuario de PostgreSQL | `materias_user` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `materias_pass` |

Ver `.env.example` para una referencia completa.

---

## Ejecución

### 1. Levantar PostgreSQL

```bash
docker compose up -d
```

### 2. Ejecutar pruebas

```bash
mvn clean test
```

### 3. Iniciar el microservicio

```bash
mvn spring-boot:run
```

El servicio arrancará en `http://localhost:3002`.

---

## Swagger UI

Una vez iniciado el servicio:

| Recurso | URL |
|---------|-----|
| **Swagger UI** | http://localhost:3002/swagger-ui/index.html |
| **OpenAPI JSON** | http://localhost:3002/v3/api-docs |
| **Health Check** | http://localhost:3002/actuator/health |

---

## Endpoints

### Materias

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/materias` | Crear materia |
| `GET` | `/api/materias` | Listar con paginación, filtros y ordenamiento |
| `GET` | `/api/materias/{id}` | Obtener por ID |
| `PUT` | `/api/materias/{id}` | Actualizar |
| `DELETE` | `/api/materias/{id}` | Eliminar (409 si tiene cursos) |

### Docentes

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/materias/docentes` | Crear docente (correo único) |
| `GET` | `/api/materias/docentes` | Listar con paginación, filtros y ordenamiento |
| `GET` | `/api/materias/docentes/{id}` | Obtener por ID |
| `PUT` | `/api/materias/docentes/{id}` | Actualizar |
| `DELETE` | `/api/materias/docentes/{id}` | Eliminar (409 si tiene cursos) |

### Cursos

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/materias/cursos` | Crear curso (valida materia y docente) |
| `GET` | `/api/materias/cursos` | Listar con paginación, filtros y ordenamiento |
| `GET` | `/api/materias/cursos/{id}` | Obtener por ID (incluye `materiaNombre`) |
| `PUT` | `/api/materias/cursos/{id}` | Actualizar |
| `DELETE` | `/api/materias/cursos/{id}` | Eliminar |

---

## Paginación

Todos los endpoints de listado soportan paginación:

```
GET /api/materias?pageNumber=0&pageSize=10
```

Respuesta:
```json
{
  "content": [...],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 25,
  "totalPages": 3
}
```

El `pageSize` máximo permitido es **100**.

---

## Filtros

Los filtros se combinan con AND y son opcionales:

### Materias
```
GET /api/materias?nombre=sistemas&programa=ingenieria&creditos=4
```

### Docentes
```
GET /api/materias/docentes?nombres=carlos&especialidad=distribuidos&activo=true
```

### Cursos
```
GET /api/materias/cursos?periodo=2026-2&estado=ACTIVO&modalidad=PRESENCIAL&materiaId=1
```

---

## Ordenamiento

```
GET /api/materias?sortBy=nombre&sortDirection=desc
```

Campos permitidos por entidad:

| Entidad | Campos |
|---------|--------|
| Materia | `id`, `nombre`, `creditos`, `programa` |
| Docente | `id`, `nombres`, `apellidos`, `especialidad` |
| Curso | `id`, `periodo`, `cupo`, `estado`, `modalidad` |

Si se envía un campo no válido, se usa `id` por defecto.

---

## Códigos HTTP

| Código | Significado |
|--------|-------------|
| `200` | Operación exitosa |
| `201` | Recurso creado |
| `204` | Recurso eliminado |
| `400` | Datos de entrada inválidos |
| `404` | Recurso no encontrado |
| `409` | Conflicto (recurso duplicado o en uso) |
| `500` | Error interno del servidor |

Formato de error:
```json
{
  "timestamp": "2026-09-21T20:00:00",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Materia no encontrada con id: 99",
  "path": "/api/materias/99"
}
```

---

## Integración Futura con API Gateway

Este microservicio está preparado para ser consumido por un API Gateway:

- **Prefijo de rutas:** todo bajo `/api/materias/**` (`/api/materias`, `/api/materias/docentes`, `/api/materias/cursos`)
- **`GET /api/materias/cursos/{id}`** devuelve `materiaNombre` para que el Gateway no necesite una segunda consulta
- **`GET /api/materias/docentes/{id}`** listo para la composición del detalle de estudiante
- **CORS** configurado con orígenes explícitos (editar en `CorsConfig.java`)
- **Health Check** disponible en `/actuator/health`
- **Independencia total:** funciona sin depender de otros microservicios

---

## Estructura del Proyecto

```
src/main/java/co/uptc/materias/
├── config/          # CORS, OpenAPI
├── controller/      # REST Controllers
├── service/         # Lógica de negocio
├── repository/      # Acceso a datos (JPA)
├── entity/          # Entidades JPA + Enums
├── dto/
│   ├── request/     # DTOs de entrada
│   └── response/    # DTOs de salida
├── mapper/          # Entity ↔ DTO
├── specification/   # Filtros dinámicos
├── exception/       # Manejo global de errores
└── MateriasApplication.java
```
