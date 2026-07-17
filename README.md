 
 
# Innovatech Backend

API REST en Spring Boot para la gestión de productos de Innovatech Chile. Se conecta a una base de datos MySQL (RDS en producción) y se despliega en AWS ECS Fargate detrás de un Application Load Balancer.

Proyecto desarrollado para la Evaluación Final Transversal — ISY1101 (Introducción a Herramientas DevOps), DuocUC.

## Stack

- Java 17 + Spring Boot 3.3.4
- Spring Data JPA + Hibernate
- MySQL 8.0 (producción, vía RDS) / H2 en memoria (tests)
- Maven
- Docker (build multi-stage)

## Estructura del proyecto

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/innovatech/backend/
│   │   │   ├── controller/
│   │   │   │   ├── ProductoController.java
│   │   │   │   └── HealthController.java
│   │   │   ├── model/Producto.java
│   │   │   ├── repository/ProductoRepository.java
│   │   │   └── BackendApplication.java
│   │   └── resources/application.properties      # config de producción (MySQL)
│   └── test/
│       ├── java/com/innovatech/backend/
│       │   ├── BackendApplicationTests.java
│       │   └── ProductoControllerTest.java
│       └── resources/application.properties       # config de test (H2 en memoria)
├── Dockerfile                                       # build multi-stage (maven → jre-alpine)
├── docker-compose.yml                               # entorno local (backend + frontend + mysql)
├── pom.xml
└── .github/workflows/deploy.yml
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/productos` | Lista todos los productos |
| GET | `/api/productos/{id}` | Obtiene un producto por ID |
| POST | `/api/productos` | Crea un producto (nombre, precio > 0, stock > 0) |
| DELETE | `/api/productos/{id}` | Elimina un producto |
| GET | `/api/health` | Health check (usado por el Target Group del ALB) |

## Cómo correr en local

### Opción A: con Docker Compose (recomendado, levanta todo el stack)

Desde la raíz del proyecto (donde está `docker-compose.yml`):

```bash
docker-compose up --build
```

- Backend: http://localhost:8080/api/health
- Frontend: http://localhost:3000

### Opción B: solo el backend, con Maven

Necesitas un MySQL corriendo aparte (local o Docker) con una base `innovatech_db`.

```bash
mvn spring-boot:run
```

Por defecto usa `localhost:3306`, usuario `root`, password `root` (ver variables de entorno abajo).

## Variables de entorno

| Variable | Descripción | Default (si no se define) |
|---|---|---|
| `DB_HOST` | Host de la base de datos | `localhost` |
| `DB_PORT` | Puerto de MySQL | `3306` |
| `DB_NAME` | Nombre de la base de datos | `innovatech_db` |
| `DB_USER` | Usuario de la base de datos | `root` |
| `DB_PASSWORD` | Password de la base de datos | `root` |

**En producción (AWS)**, `DB_HOST/DB_PORT/DB_NAME` se configuran como valores directos en la Task Definition de ECS, mientras que `DB_USER` y `DB_PASSWORD` se inyectan de forma segura desde **AWS Secrets Manager** (`innovatech/rds-credentials`), nunca como texto plano.

## Tests

```bash
mvn test
```

Usa una base de datos **H2 en memoria** (no requiere MySQL corriendo), configurada en `src/test/resources/application.properties`. Incluye:

- Test de carga de contexto de Spring Boot
- Health check responde `UP`
- Creación de producto válido → `201 Created`
- Rechazo de producto con precio inválido → `400 Bad Request`

## Pipeline CI/CD

Cada push a `main` dispara `.github/workflows/deploy.yml`, que ejecuta en orden:

1. **Test** — `mvn test` (JUnit 5 + H2)
2. **Build** — construye la imagen Docker (multi-stage: Maven → JRE Alpine)
3. **Push** — publica la imagen en Amazon ECR (tags: SHA del commit + `latest`)
4. **Deploy** — registra una nueva revisión de la Task Definition y actualiza el Service en Amazon ECS (rolling update, espera confirmación de estabilidad antes de marcar éxito)

### Secretos requeridos (GitHub Actions → Settings → Secrets and variables → Actions)

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN`

> Estas credenciales provienen de AWS Academy Learner Lab y son temporales (expiran cada ~4 horas). Deben actualizarse en los secrets del repo cada vez que se reinicia la sesión del lab.

## Infraestructura en AWS

- **VPC**: `innovatech-vpc` (10.0.0.0/16), 2 subredes públicas + 2 privadas
- **Base de datos**: RDS MySQL 8.0, en subred privada, sin acceso público
- **Orquestación**: ECS Fargate, clúster `innovatech-cluster`, autoscaling por CPU (target 50%, min 1 / max 4 tareas)
- **Balanceo**: Application Load Balancer, enruta `/api/*` hacia este servicio (puerto 8080)
- **Seguridad**: Security Groups en cadena (alb → backend → rds), credenciales de BD vía Secrets Manager
- **Observabilidad**: logs y métricas en CloudWatch (`/ecs/innovatech-backend-task`)

Ver detalle completo de la arquitectura en el informe técnico del proyecto.

## Repositorio relacionado

- Frontend: https://github.com/bluepoint25/innovatech-frontend