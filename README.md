# Gethics Backend

Backend de **Gethics**, la aplicación móvil de gestión ganadera desarrollada por la startup **JamSell** (UPC, curso de Ingeniería de Software / Aplicaciones Móviles).

Gethics digitaliza y centraliza el control del ganado para pequeños y medianos ganaderos y para veterinarios de campo: registro de animales, historial sanitario, calendario con alertas, control económico, reportes y módulo veterinario.

## Tecnologías

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA
- PostgreSQL 16
- Maven
- Docker Compose (base de datos local)

## Arquitectura

El proyecto sigue **Domain-Driven Design (DDD)**. Cada bounded context se divide en cuatro capas:

```
<contexto>/
├── domain/          # Aggregates, entities, value objects, commands, queries, events, services
├── application/     # Command services, query services, outbound services, event handlers
├── infrastructure/  # Repositorios JPA e integraciones técnicas
└── interfaces/      # REST (resources, transform) y ACL (facades entre contextos)
```

## Bounded Contexts

| Paquete | Bounded Context | Responsabilidad |
|---|---|---|
| `iam` | Identity & Access | Usuarios, roles, sesiones y autenticación |
| `livestock` | Livestock Management | Fincas (Farm) y animales (Animal), código QR |
| `sanitary` | Sanitary Tracking | Historial clínico, eventos sanitarios, calendario y recordatorios |
| `veterinary` | Veterinary Care | Asignación veterinario-cliente y seguimiento clínico |
| `finance` | Financial Management | Ingresos, egresos y pagos de suscripción |
| `analytics` | Analytics & Alerts | Tendencias, reportes y alertas |
| `subscription` | Subscription Management | Planes y suscripciones |
| `shared` | Código compartido | Configuración, base entities y utilidades comunes |

## Cómo correr el proyecto

1. Instalar Java 21, Docker Desktop e IntelliJ IDEA.
2. Clonar el repositorio:
```bash
   git clone https://github.com/UPc-1ACC0238-2620-4939-JamSell/gethics-backend.git
```
3. Levantar la base de datos:
```bash
   docker compose up -d
```
4. Ejecutar `GethicsApplication` desde IntelliJ (o `./mvnw spring-boot:run`).

La API queda disponible en `http://localhost:8080`.

### Cambio de esquema: calendario sanitario (US-12)

`sanitary_events` incorpora `scheduled_date` (fecha de los eventos `SCHEDULED`) y `occurred_at` pasa a ser nullable (los eventos `SCHEDULED` aún no ocurrieron).

El proyecto **no usa Flyway ni Liquibase**: el esquema lo gestiona Hibernate (`ddl-auto: update` en `dev`, `validate` en `prod`). `update` crea la columna nueva, pero no elimina el `NOT NULL` de `occurred_at` en tablas ya existentes. En una base creada antes de US-12 (y en `prod`, antes de desplegar) hay que ejecutar manualmente:

```sql
ALTER TABLE sanitary_events ADD COLUMN IF NOT EXISTS scheduled_date date;
ALTER TABLE sanitary_events ALTER COLUMN occurred_at DROP NOT NULL;
```

Una base nueva no requiere ningún paso.

## Variables de entorno

Copia `.env.example` como `.env` y ajusta los valores si usas otra base de datos (por ejemplo Neon o Supabase). **Nunca subas el archivo `.env` al repositorio.**

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo (`dev` o `prod`) | `dev` |
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/gethics` |
| `DB_USER` | Usuario de la base de datos | `gethics` |
| `DB_PASSWORD` | Contraseña de la base de datos | `gethics` |

## Flujo de trabajo con Git

- `main`: rama estable, no se trabaja directamente en ella.
- `develop`: rama de integración.
- `feature/<contexto>-<tarea>`: una rama por tarea (ejemplo: `feature/livestock-crud-animal`).

Cada cambio se integra mediante **Pull Request hacia `develop`**. Se recomienda que cada integrante trabaje en un bounded context distinto para evitar conflictos.

### Convención de commits

```
feat: nueva funcionalidad
fix: corrección de error
chore: tareas de configuración o estructura
docs: documentación
refactor: mejora de código sin cambiar comportamiento
```

## Equipo JamSell

- Mauricio Sebastian Castillo Yataco (u202113229)
- Juan Jose Meza Huanacune (u202320574)
- Luis Angel Pillaca Vidal (u202315654)
- Abigail Nadhim Raymundo Villarroel (u202318001)
- Mateo Paolo Salazar Miranda (u202315171)