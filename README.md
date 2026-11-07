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

### Cambio de esquema: recordatorios de vacunación (US-13)

US-13 agrega la tabla `reminders`. Con `ddl-auto: update` (`dev`) Hibernate la crea sola, incluida la FK y la restricción única. En `prod` (`ddl-auto: validate`, sin Flyway ni Liquibase) hay que crearla manualmente antes de desplegar:

```sql
CREATE TABLE reminders (
    id                uuid PRIMARY KEY,
    sanitary_event_id uuid NOT NULL REFERENCES sanitary_events (id),
    scheduled_for     timestamp(6) NOT NULL,
    status            varchar(255) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts          integer NOT NULL,
    sent_at           timestamp(6) with time zone,
    created_at        timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_reminders_event_scheduled_for UNIQUE (sanitary_event_id, scheduled_for)
);
```

- `sanitary_event_id` es una FK real hacia `sanitary_events.id` (mismo bounded context). Un evento puede tener varios recordatorios.
- `UNIQUE (sanitary_event_id, scheduled_for)` es la clave de idempotencia: el mismo recordatorio no se crea dos veces.
- `attempts` y `sent_at` son atributos técnicos de entrega; los valores de `status` son una decisión de implementación.

### Flujo de US-13 (recordatorio de vacunación)

1. `POST /api/v1/animals/{animalId}/sanitary-events/scheduled` programa un evento (`SCHEDULED`) de cualquier tipo sanitario. La fecha no puede ser anterior a hoy (lo valida el REST y también el dominio, con el `Clock` de `gethics.sanitary.reminders.zone`).
2. Cada día (`gethics.sanitary.reminders.cron`) el job envía un recordatorio por cada vacuna `SCHEDULED` cuya fecha es hoy + 3 días. Es idempotente: el mismo aviso no se envía dos veces.
3. `POST /api/v1/animals/{animalId}/sanitary-events/{eventId}/complete` registra como aplicada **la misma** vacuna programada (`SCHEDULED` → `COMPLETED`): fija `occurredAt`, conserva `scheduledDate` y no crea otra fila. Desde ese momento el job no genera recordatorio ni reintenta uno `FAILED`. Un evento registrado aparte con el `POST` de US-11 **no** se vincula con el programado.

No hay cambio de esquema: `status` ya admite `COMPLETED` y `occurred_at` ya es nullable.

**Push real bloqueado externamente:** el envío lo hace `LoggingNotificationService`, que solo escribe en el log (simulado). Falta resolver animal → propietario (Livestock no existe), propietario → device token (IAM no lo modela) y un proveedor push (FCM u otro, con credenciales).

### Cambio de esquema: registro de animales (US-05)

US-05 agrega la tabla `animals` (`POST /api/v1/animals`). Con `ddl-auto: update` (`dev`) Hibernate la crea sola. En `prod` (`ddl-auto: validate`, sin Flyway ni Liquibase) hay que crearla manualmente antes de desplegar:

```sql
CREATE TABLE animals (
    id                 uuid PRIMARY KEY,
    farm_id            uuid,
    tag                varchar(50) NOT NULL,
    qr_code            varchar(20) NOT NULL,
    name               varchar(100),
    breed              varchar(60) NOT NULL,
    sex                varchar(255) CHECK (sex IN ('MALE', 'FEMALE')),
    birth_date         date NOT NULL,
    initial_weight_kg  numeric(7, 2),
    photo_url          varchar(500),
    status             varchar(255) NOT NULL CHECK (status IN ('ACTIVE', 'SOLD', 'DECEASED', 'INACTIVE')),
    created_at         timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_animals_tag UNIQUE (tag),
    CONSTRAINT uk_animals_qr_code UNIQUE (qr_code)
);
```

- `qr_code` lo genera el sistema al registrar el animal (`GTH-` + 12 caracteres hexadecimales) y no se envía en el `POST`. Es la identificación única del animal para el escaneo QR del informe; el contenido del QR es un identificador, no hay imagen.
- `status` nace siempre `ACTIVE` (las bajas lógicas de US-08 cambiarán este valor). `sex`, `name` y `farm_id` son opcionales por ahora: el formulario móvil no los pide y las fincas (US-09/10) aún no existen; `farm_id` se guarda sin FK ni validación.
- `tag` (arete) se guarda sin espacios y en mayúsculas, por lo que `mx-1` y `MX-1` son el mismo arete. Es único en todo el hato (el informe lo plantea por finca; se ajustará cuando exista Farm). Duplicado → `409`.
- `birth_date` no puede ser posterior a hoy (mismo `Clock` que sanitary) → `400`. `initial_weight_kg` es opcional y, si viene, mayor a 0.
- `breed` es texto libre (≤ 60): el negocio aún no define un catálogo de razas. `photo_url` es solo una URL; no hay subida de archivos.
- `sanitary_events` / `clinical_histories` siguen referenciando al animal por `animalId` sin FK física entre bounded contexts.

### Cambio de esquema: registro de granjas (US-09)

US-09 agrega la tabla `farms` (`POST /api/v1/farms` y `GET /api/v1/farms?ownerId=`). Con `ddl-auto: update` (`dev`) Hibernate la crea sola. En `prod` (`ddl-auto: validate`, sin Flyway ni Liquibase) hay que crearla manualmente antes de desplegar:

```sql
CREATE TABLE farms (
    id               uuid PRIMARY KEY,
    owner_id         uuid NOT NULL,
    name             varchar(100) NOT NULL,
    normalized_name  varchar(100) NOT NULL,
    location         varchar(200) NOT NULL,
    size_hectares    numeric(9, 2),
    status           varchar(255) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at       timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_farms_owner_name UNIQUE (owner_id, normalized_name)
);
```

- El nombre es único **por dueño** sin distinguir mayúsculas ni espacios sobrantes (`Fundo Sur` y ` fundo sur` chocan; dos dueños distintos pueden usar el mismo nombre). Cada granja conserva el nombre como se escribió; `normalized_name` solo sirve para la restricción. Duplicado → `409`.
- `size_hectares` es opcional y, si viene, mayor a 0. La historia pide "tamaño" sin unidad y el modelo del informe no lo incluye: se tomó hectáreas.
- `status` nace `ACTIVE`; aún no hay forma de desactivar una granja.
- `owner_id` referencia al usuario de IAM por id, sin FK física. **IAM aún no está integrado**, así que el dueño llega como `ownerId` en el `POST` y como parámetro obligatorio del `GET`, y no se valida que el usuario exista ni que sea quien llama. Cuando IAM esté disponible, el dueño saldrá del usuario autenticado y el parámetro se eliminará.
- `animals.farm_id` no tiene FK física (mismo contexto, pero se valida en la aplicación); asociar animales a una granja es US-10.

### Cambio de esquema: asociar animales a una granja (US-10)

US-10 agrega la tabla `animal_farm_assignments` (historial de cambios de granja) y los endpoints `PUT /api/v1/animals/{animalId}/farm`, `GET /api/v1/animals/{animalId}/farm-history` y `GET /api/v1/farms/{farmId}/animals`. No cambia `animals` ni `farms`. Con `ddl-auto: update` (`dev`) Hibernate crea la tabla sola. En `prod` (`ddl-auto: validate`, sin Flyway ni Liquibase) hay que crearla manualmente antes de desplegar:

```sql
CREATE TABLE animal_farm_assignments (
    id            uuid PRIMARY KEY,
    animal_id     uuid NOT NULL,
    from_farm_id  uuid,
    to_farm_id    uuid NOT NULL,
    assigned_at   timestamp(6) with time zone NOT NULL
);
CREATE INDEX ix_animal_farm_assignments_animal ON animal_farm_assignments (animal_id);
```

- Cada asignación o cambio agrega **una fila** y nunca se modifica ni se borra (escenario 2 de la historia: se conserva el historial). `from_farm_id` es `NULL` en la primera asignación. Repetir la misma granja no agrega fila (`PUT` idempotente).
- Según el informe cada animal pertenece a una finca: se puede **asignar y cambiar** de granja, pero no dejar al animal sin ella (`farmId` nulo → `400`).
- `PUT` valida que existan el animal y la granja (`404` en otro caso). `POST /api/v1/animals` con `farmId` ahora también valida la granja (`404`) y registra la primera asignación en el historial.
- `GET /farms/{farmId}/animals` lista por arete y, como el inventario, solo los `ACTIVE` salvo que se pida `?status=`. `404` si la granja no existe.
- Limitación: el animal no tiene dueño (IAM aún no está integrado), así que no se valida que la granja sea del mismo ganadero que el animal.

### Cambio de esquema: Analytics & Alerts (US-21)

Se agregan las tablas `analytics`, `livestock_trends` y `alerts`. Con `ddl-auto: update` (`dev`) Hibernate las crea solo, incluidas las FK y la restricción única. En `prod` (`ddl-auto: validate`, sin Flyway ni Liquibase) hay que crearlas manualmente antes de desplegar:

```sql
CREATE TABLE analytics (
    id               uuid PRIMARY KEY,
    owner_id         uuid NOT NULL UNIQUE,
    last_analysis_at timestamp(6) with time zone NOT NULL,
    risk_level       varchar(255) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    created_at       timestamp(6) with time zone NOT NULL
);

CREATE TABLE livestock_trends (
    id           uuid PRIMARY KEY,
    analytics_id uuid NOT NULL REFERENCES analytics (id),
    type         varchar(255) NOT NULL CHECK (type IN ('SANITARY', 'FINANCIAL', 'COMBINED')),
    description  text NOT NULL,
    detected_at  timestamp(6) with time zone NOT NULL,
    risk_level   varchar(255) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE TABLE alerts (
    id         uuid PRIMARY KEY,
    owner_id   uuid NOT NULL,
    trend_id   uuid NOT NULL REFERENCES livestock_trends (id),
    message    text NOT NULL,
    status     varchar(255) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'READ')),
    created_at timestamp(6) with time zone NOT NULL
);
```

- `analytics.owner_id` es único (un `Analytics` por propietario) y, igual que `alerts.owner_id`, **no** tiene FK: referencia a IAM por id entre bounded contexts.
- `livestock_trends.analytics_id` y `alerts.trend_id` son FK reales. No hay `UNIQUE(trend_id)`: una tendencia puede tener varias alertas.
- `livestock_trends.description` y `alerts.message` son `text NOT NULL`, sin longitud máxima (el informe no define ninguna).
- Las marcas de tiempo son `timestamp with time zone` (`Instant` en Java), la convención técnica del backend; el informe solo dice DATETIME.
- La generación automática de alertas queda **inactiva** hasta que el equipo defina qué niveles de riesgo alertan (`gethics.analytics.alert-risk-levels`; ver comentarios de `application.yaml`).

### Flujo de US-21 (alertas automáticas por tendencias)

```
TrendAnalysisJob (cron gethics.analytics.analysis.cron; deshabilitado por defecto)
  -> TrendAnalysisCommandService
       -> cada TrendDetector registrado (hoy: ninguno)
            -> LivestockTrendCommandService: persiste la tendencia
                 -> RiskAlertCommandService + AlertRiskPolicy: crea la Alert (PENDING) si el nivel es alertable
       -> AlertDispatchCommandService: envía las Alert PENDING por PushNotificationService (SENT si funciona)
```

- **No existe ningún algoritmo de detección.** `TrendDetector` es un puerto sin implementación: Trello no define indicadores, ventana temporal, valores normales, fórmula de anomalía ni umbral. Sin detectores el análisis se ejecuta, lo registra en el log, no crea tendencias ni alertas y despacha igualmente las alertas PENDING de ejecuciones anteriores.
- `gethics.analytics.alert-risk-levels` decide qué niveles de riesgo **ya clasificados** generan alerta; **no** es el "umbral definido" de Trello.
- El cron no tiene valor por defecto (la periodicidad no está definida): sin configurarlo el job no se programa (`Scheduled.CRON_DISABLED`).
- Cada tendencia que devuelve un detector se registra como nueva: no hay deduplicación entre tendencias porque "el mismo patrón" no está definido; es responsabilidad del detector no repetir una anomalía ya informada.
- Si el push falla, la alerta queda PENDING y se reintenta en cada ejecución, sin límite de reintentos (no está definido).
- **Bloqueos:** definición del detector y del umbral (negocio); el push es simulado (`LoggingPushNotificationAdapter`) porque faltan Livestock (animal → propietario), device tokens en IAM y un proveedor push (FCM u otro).
- No hay cambio de esquema.

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