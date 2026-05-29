# Syncro — Microservicio de Pedidos

> **DSY1106 Desarrollo Fullstack III — Evaluación Parcial N°3**
> Integración de arquitectura de microservicios

---

## Tabla de contenidos

1. [Descripción general del proyecto](#1-descripción-general-del-proyecto)
2. [Arquitectura de microservicios](#2-arquitectura-de-microservicios)
3. [Tecnologías y lenguajes utilizados](#3-tecnologías-y-lenguajes-utilizados)
4. [Estructura del proyecto](#4-estructura-del-proyecto)
5. [Requisitos previos](#5-requisitos-previos)
6. [Instalación y configuración](#6-instalación-y-configuración)
7. [Ejecución del microservicio](#7-ejecución-del-microservicio)
8. [Endpoints REST (API)](#8-endpoints-rest-api)
9. [Persistencia de datos (JPA)](#9-persistencia-de-datos-jpa)
10. [Comunicación asíncrona con RabbitMQ](#10-comunicación-asíncrona-con-rabbitmq)
11. [Seguridad — JWT](#11-seguridad--jwt)
12. [Pruebas unitarias](#12-pruebas-unitarias)
13. [Patrones de diseño aplicados](#13-patrones-de-diseño-aplicados)

---

## 1. Descripción general del proyecto

**Syncro** es una plataforma de gestión logística de inventario diseñada para integrarse en cualquier PYME que requiera sincronización en tiempo real de sus pedidos, inventario y envíos.

El sistema resuelve la fragmentación de datos propia de los sistemas monolíticos permitiendo que múltiples módulos operen de manera **desacoplada y simultánea** mediante una arquitectura orientada a eventos (EDA).

Este repositorio contiene el **Microservicio de Pedidos (MS-Pedidos)**, el componente central responsable del ciclo de vida completo de un pedido: desde su creación hasta su entrega, incluyendo la publicación de eventos hacia los demás microservicios.

### Objetivos del microservicio

- Gestionar el ciclo de vida completo de un pedido (PENDIENTE → CONFIRMADO → EN_PREPARACION → DESPACHADO → EN_RUTA → ENTREGADO / CANCELADO).
- Publicar eventos de dominio a RabbitMQ cuando un pedido es confirmado, para que MS-Inventario descuente el stock y MS-Envíos genere el despacho.
- Garantizar **resiliencia** mediante el patrón **Outbox**: si RabbitMQ no está disponible, el evento queda encolado en BD y se reintenta automáticamente.
- Proveer una API REST documentada con Swagger para consumo desde el frontend y el BFF.

---

## 2. Arquitectura de microservicios

### Diagrama de arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENTE (Browser)                          │
│                       React + Vite (Frontend)                       │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTP / REST
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    BFF — API Gateway (:8080)                        │
│              Enruta peticiones a los microservicios                 │
└──────┬────────────────────────┬────────────────────────┬────────────┘
       │ REST                   │ REST                   │ REST
       ▼                        ▼                        ▼
┌─────────────┐        ┌─────────────────┐      ┌──────────────────┐
│  MS-Pedidos │        │  MS-Inventario  │      │    MS-Envíos     │
│   (:8083)   │        │    (:8081)      │      │    (:8082)       │
│             │        │                 │      │                  │
│  MySQL DB   │        │    MySQL DB     │      │    MySQL DB      │
└──────┬──────┘        └────────┬────────┘      └────────┬─────────┘
       │                        │                        │
       │   FANOUT EXCHANGE      │                        │
       │  pedidos.exchange      │                        │
       ├──────────────────────► │ inventario.sincronizar │
       │                        └────────────────────────┘
       │
       └──────────────────────────────────────────────────►
                                              envio.generar (cola)
                                ┌──────────────────────────────────┐
                                │         RabbitMQ Broker          │
                                │  (Message Broker — EDA)          │
                                └──────────────────────────────────┘
```

### Descripción de los componentes

**Frontend (React + Vite)**
Interfaz de usuario que consume la API REST a través del BFF. Permite a los operadores crear pedidos, consultar historial y cambiar estados.

**BFF — API Gateway**
Backend For Frontend que actúa como punto de entrada único. Enruta las peticiones HTTP al microservicio correspondiente y centraliza la autenticación.

**MS-Pedidos (este repositorio)**
Núcleo del sistema. Gestiona el ciclo de vida de los pedidos, persiste en MySQL y publica eventos de dominio a RabbitMQ cuando un pedido pasa a estado CONFIRMADO.

**MS-Inventario**
Consume el evento `pedido.creado` desde la cola `inventario.sincronizar` y descuenta el stock por SKU.

**MS-Envíos**
Consume el mismo evento desde la cola `envio.generar` y genera el registro de despacho correspondiente.

**RabbitMQ**
Message broker que implementa el patrón **Fanout Exchange**: un único evento publicado por MS-Pedidos es entregado simultáneamente a ambas colas (inventario y envíos), desacoplando completamente los servicios.

### Flujo de un pedido confirmado

```
Operador crea pedido
       │
       ▼
MS-Pedidos guarda en BD (estado: PENDIENTE)
       │
Operador confirma pedido (PATCH /pedidos/{id}/estado)
       │
       ▼
MS-Pedidos:
  1. Valida transición PENDIENTE → CONFIRMADO
  2. Persiste nuevo estado + entrada en historial_estado_pedido
  3. Guarda evento en tabla outbox_evento (patrón Outbox)
  4. OutboxScheduler (cada 30 s) publica a pedidos.exchange (Fanout)
       │
       ├──► Cola inventario.sincronizar → MS-Inventario descuenta stock
       └──► Cola envio.generar        → MS-Envíos genera despacho
```

---

## 3. Tecnologías y lenguajes utilizados

| Capa | Tecnología | Versión | Propósito |
|------|-----------|---------|-----------|
| Lenguaje | Java | 21 | Lenguaje principal del backend |
| Framework | Spring Boot | 3.3.4 | Base del microservicio REST |
| Seguridad | Spring Security + JWT (jjwt) | 0.12.3 | Autenticación stateless con tokens |
| Persistencia | Spring Data JPA + Hibernate | — | ORM para acceso a MySQL |
| Base de datos | MySQL (Aiven Cloud) | 8.x | Almacenamiento relacional |
| Mensajería | RabbitMQ (AMQP) | — | Message broker para eventos de dominio |
| Documentación API | SpringDoc OpenAPI (Swagger UI) | 2.6.0 | Documentación interactiva de endpoints |
| Build | Maven | 3.9.x | Gestión de dependencias y empaquetado |
| Contenedor | Docker | — | Despliegue reproducible |
| Testing | JUnit 5 + Mockito + Spring Security Test | — | Pruebas unitarias e integración |
| Utilidades | Lombok | 1.18.34 | Reducción de boilerplate (getters, builders) |

### Integración de tecnologías

Spring Boot actúa como contenedor principal que integra todos los componentes: expone los endpoints REST, delega la autenticación a Spring Security (que valida el JWT en cada petición), usa JPA/Hibernate para mapear las entidades Java a tablas MySQL, y utiliza `RabbitTemplate` para publicar eventos al exchange de RabbitMQ. La integración es completamente declarativa mediante anotaciones, sin XML de configuración.

---

## 4. Estructura del proyecto

```
src/
├── main/
│   ├── java/com/syncro/pedido/
│   │   ├── config/
│   │   │   ├── RabbitMQConfig.java      # Exchange, colas y bindings
│   │   │   ├── SecurityConfig.java      # Filtros de seguridad JWT
│   │   │   └── SwaggerConfig.java       # Configuración OpenAPI
│   │   ├── controller/
│   │   │   ├── AuthController.java      # POST /auth/login, /auth/register
│   │   │   ├── EmpresaController.java   # POST /empresas
│   │   │   └── PedidoController.java    # CRUD de pedidos
│   │   ├── dto/
│   │   │   ├── request/                 # DTOs de entrada (validados con @Valid)
│   │   │   └── response/                # DTOs de salida (sin exponer entidades)
│   │   ├── event/
│   │   │   ├── OutboxScheduler.java     # Scheduler que reintenta eventos pendientes
│   │   │   └── PedidoCreadoEvent.java   # Payload del evento de dominio
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── PedidoNotFoundException.java
│   │   │   └── TransaccionEstadoInvalidaException.java
│   │   ├── model/                       # Entidades JPA
│   │   ├── repository/                  # Interfaces Spring Data JPA
│   │   ├── security/
│   │   │   ├── JwtAuthFilter.java       # Filtro JWT por petición
│   │   │   └── JwtUtil.java             # Generación y validación de tokens
│   │   └── service/
│   │       ├── AuthService.java
│   │       ├── EmpresaService.java
│   │       ├── PedidoService.java
│   │       └── UserDetailsServiceImpl.java
│   └── resources/
│       └── application.properties       # Configuración por variables de entorno
└── test/
    └── java/com/syncro/pedido/
        ├── controller/PedidoControllerTest.java
        ├── security/JwtUtilTest.java
        └── service/
            ├── AuthServiceTest.java
            └── EmpresaServiceTest.java
```

---

## 5. Requisitos previos

- **Java 21** (JDK)
- **Maven 3.9+** (o usar el wrapper `./mvnw` incluido)
- **Docker** (opcional, para levantar con contenedor)
- **MySQL 8** accesible (local o remoto)
- **RabbitMQ** accesible (local o remoto)

---

## 6. Instalación y configuración

### Clonar el repositorio

```bash
git clone https://github.com/<org>/syncro-ms-pedidos.git
cd syncro-ms-pedidos
```

### Variables de entorno requeridas

Crea un archivo `.env` en la raíz del proyecto (no versionado) o configura las variables en tu sistema:

```env
# Base de datos MySQL
DB_URL=jdbc:mysql://localhost:3306/syncro_pedidos?useSSL=false&serverTimezone=UTC
DB_USERNAME=tu_usuario
DB_PASSWORD=tu_contraseña

# JWT
JWT_SECRET=syncro-secret-key-produccion-minimo-32-chars!!
JWT_EXPIRATION=86400000

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
```

### Perfil de test (H2 en memoria)

Para ejecutar pruebas sin necesidad de MySQL ni RabbitMQ, el perfil `test` usa H2 en memoria. Las variables de entorno no son necesarias al correr tests (ver `src/test/resources/application-test.properties`).

---

## 7. Ejecución del microservicio

### Opción A — Maven directamente

```bash
# Compilar sin ejecutar tests
./mvnw clean package -DskipTests

# Ejecutar el JAR generado
java -jar target/pedido-0.0.1-SNAPSHOT.jar
```

El servicio queda disponible en `http://localhost:8083`.

### Opción B — Docker

```bash
# Construir la imagen
docker build -t syncro-ms-pedidos .

# Ejecutar el contenedor
docker run -p 8083:8083 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/syncro_pedidos \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=secret \
  -e JWT_SECRET=syncro-secret-key-minimo-32-chars!! \
  -e JWT_EXPIRATION=86400000 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_USERNAME=guest \
  -e RABBITMQ_PASSWORD=guest \
  -e RABBITMQ_VHOST=/ \
  syncro-ms-pedidos
```

### Verificar que levantó correctamente

```bash
# Debe responder con la especificación OpenAPI
curl http://localhost:8083/v3/api-docs

# Swagger UI interactivo
open http://localhost:8083/swagger-ui/index.html
```

---

## 8. Endpoints REST (API)

La documentación completa e interactiva está disponible en Swagger UI (`/swagger-ui/index.html`).

### Autenticación

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/auth/register` | Pública | Registra un nuevo operador |
| POST | `/auth/login` | Pública | Inicia sesión, devuelve token JWT |
| POST | `/empresas` | Pública | Crea una nueva empresa |

**Ejemplo — Registro:**
```json
POST /auth/register
{
  "nombre": "Sofía Gómez",
  "email": "sofia@pyme-demo.cl",
  "password": "Admin1234!",
  "empresaId": 1,
  "rol": "OPERADOR"
}
```

**Respuesta exitosa (201):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "email": "sofia@pyme-demo.cl",
  "nombre": "Sofía Gómez",
  "rol": "OPERADOR",
  "empresaId": 1,
  "expiresIn": 86400000
}
```

### Pedidos (requieren `Authorization: Bearer <token>`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/pedidos` | Crea un pedido en estado PENDIENTE |
| GET | `/pedidos/{id}` | Obtiene detalle completo de un pedido |
| PATCH | `/pedidos/{id}/estado` | Cambia el estado del pedido |
| GET | `/pedidos/historial/{empresaId}` | Lista pedidos de una empresa (filtros opcionales) |

**Ejemplo — Crear pedido:**
```json
POST /pedidos
Authorization: Bearer <token>

{
  "empresaId": 1,
  "usuarioId": 2,
  "notas": "Entregar antes de las 18:00",
  "direccion": {
    "calle": "Av. Providencia",
    "numero": "1234",
    "ciudad": "Santiago",
    "region": "Región Metropolitana",
    "pais": "Chile"
  },
  "items": [
    {
      "sku": "ELEC-001",
      "nombre": "Audífonos BT",
      "cantidad": 2,
      "precioUnitario": 29990
    }
  ]
}
```

**Ejemplo — Cambiar estado:**
```json
PATCH /pedidos/15/estado
Authorization: Bearer <token>

{
  "nuevoEstado": "CONFIRMADO",
  "motivo": "Pago verificado exitosamente"
}
```

### Transiciones de estado válidas

```
PENDIENTE ──► CONFIRMADO ──► EN_PREPARACION ──► DESPACHADO ──► EN_RUTA ──► ENTREGADO
    │               │                │
    └──► CANCELADO  └──► CANCELADO   └──► CANCELADO
```

### Códigos de respuesta HTTP

| Código | Significado |
|--------|-------------|
| 200 OK | Operación exitosa |
| 201 Created | Recurso creado correctamente |
| 400 Bad Request | Validación de campos fallida |
| 401 Unauthorized | Token JWT ausente o inválido |
| 404 Not Found | Pedido o recurso no encontrado |
| 409 Conflict | Transición de estado no permitida |
| 500 Internal Server Error | Error inesperado del servidor |

---

## 9. Persistencia de datos (JPA)

### Estrategia de persistencia

El microservicio usa **Spring Data JPA con Hibernate** como ORM. Todas las entidades están mapeadas a tablas MySQL mediante anotaciones JPA, sin XML de configuración.

La configuración `spring.jpa.hibernate.ddl-auto=none` garantiza que Hibernate **no modifica el esquema** en producción; las tablas son gestionadas por scripts SQL versionados.

### Modelo de datos

```
empresa (1) ──────────────────── (N) usuario
   │                                    │
   │ (1)                                │ (N)
   └──── (N) pedido ────────────────────┘
               │
               ├── (1) direccion_entrega    [CascadeType.PERSIST]
               ├── (N) item_pedido          [CascadeType.ALL, orphanRemoval]
               ├── (N) historial_estado_pedido [CascadeType.ALL]
               └── (N) outbox_evento        [tabla independiente]
```

### Entidades principales

**Pedido** — Entidad central. Incluye ciclo de vida del estado, totales calculados, relación con empresa, usuario y dirección. Usa `@PreUpdate` para mantener `fechaActualizacion` sincronizada automáticamente.

**ItemPedido** — Snapshot del producto al momento del pedido. El subtotal se calcula en memoria con `@Transient` para no persistir un valor derivado.

**HistorialEstado** — Registro inmutable de cada cambio de estado: quién lo hizo, cuándo y por qué. Implementa el requerimiento de trazabilidad completa (RF-1.2).

**DireccionEntrega** — Tabla separada para conservar la dirección original aunque el cliente la modifique después.

**OutboxEvento** — Tabla de respaldo para el patrón Outbox. Los eventos fallidos se reintentan hasta 5 veces cada 30 segundos.

### Repositorios (Spring Data JPA)

Los repositorios extienden `JpaRepository` y exponen métodos derivados del nombre del método sin SQL manual:

```java
// PedidoRepository — ejemplos de queries derivadas
List<Pedido> findByEmpresa_IdAndEstado(Long empresaId, EstadoPedido estado);
List<Pedido> findByEmpresa_IdAndFechaCreacionBetween(Long id, LocalDateTime desde, LocalDateTime hasta);

// Query JPQL explícita para condición compuesta
@Query("SELECT p FROM Pedido p WHERE p.estado = :estado AND p.eventoPublicado = false")
List<Pedido> findPedidosConfirmadosSinEvento(@Param("estado") EstadoPedido estado);
```

### Transacciones

Todos los métodos de escritura en `PedidoService` están anotados con `@Transactional`, garantizando atomicidad: si falla cualquier paso (guardar el pedido, el historial o el evento outbox), toda la operación hace rollback.

---

## 10. Comunicación asíncrona con RabbitMQ

### Configuración del exchange y colas

```
pedidos.exchange (FanoutExchange)
       ├──► inventario.sincronizar  (durable queue)
       └──► envio.generar           (durable queue)
```

Se usa un **FanoutExchange** para que el mismo mensaje llegue a ambas colas simultáneamente, sin que MS-Pedidos conozca quiénes son los consumidores.

### Patrón Outbox

Para garantizar la entrega del evento incluso si RabbitMQ está temporalmente caído, se implementa el patrón **Transactional Outbox**:

1. Al confirmar un pedido, se guarda el evento en la tabla `outbox_evento` **dentro de la misma transacción** que actualiza el estado del pedido.
2. Un scheduler (`@Scheduled(fixedDelay = 30000)`) escanea los eventos pendientes cada 30 segundos y los publica a RabbitMQ.
3. Si el envío falla, incrementa el contador de intentos. Tras 5 intentos fallidos, el evento deja de reintentarse y queda registrado con su mensaje de error para auditoría manual.

Este mecanismo garantiza que **nunca se pierde un evento** por una falla transitoria de infraestructura.

---

## 11. Seguridad — JWT

### Flujo de autenticación

```
Cliente ──POST /auth/login──► AuthController
                               └──► AuthService.login()
                                        └──► AuthenticationManager verifica BCrypt
                                        └──► JwtUtil.generateToken()
                               ◄── { token: "eyJ..." }

Cliente ──GET /pedidos/1──► JwtAuthFilter
  Header: Authorization: Bearer <token>
                           ├── Extrae email del payload JWT
                           ├── Carga usuario desde BD (UserDetailsService)
                           ├── Valida firma y expiración
                           └──► SecurityContextHolder.setAuthentication()
                           ──► PedidoController (usuario autenticado)
```

Las contraseñas siempre se almacenan cifradas con **BCrypt** (factor de costo 12). El token JWT incluye el rol del usuario como claim para que el frontend pueda gestionar permisos en la UI.

---

## 12. Pruebas unitarias

### Cómo ejecutar las pruebas

```bash
# Ejecutar todas las pruebas (usa perfil "test" con H2 en memoria)
./mvnw test

# Generar reporte de cobertura con JaCoCo
./mvnw test jacoco:report

# El reporte HTML queda en:
# target/site/jacoco/index.html
```

No se requiere MySQL ni RabbitMQ para correr los tests: el perfil `test` usa una base de datos H2 en memoria y los tests de servicio usan Mockito para aislar dependencias.

### Suite de pruebas implementada

#### `JwtUtilTest` — Tests unitarios puros

| Test | Descripción |
|------|-------------|
| `generateToken_usuarioValido_retornaTokenNoVacio` | El token generado no es nulo ni vacío |
| `generateToken_retornaFormatoJWT` | El token tiene exactamente 3 partes separadas por punto |
| `extractUsername_tokenValido_retornaEmail` | Se extrae correctamente el email del payload |
| `validateToken_tokenValidoMismoUsuario_retornaTrue` | Un token válido del mismo usuario es aceptado |
| `validateToken_tokenDeOtroUsuario_retornaFalse` | Un token de otro usuario es rechazado |
| `isTokenExpired_tokenReciente_retornaFalse` | Un token recién emitido no está expirado |
| `isTokenExpired_tokenExpirado_retornaTrue` | Un token con expiración en el pasado lanza `ExpiredJwtException` |
| `getExpiration_retornaValorConfigurado` | El tiempo de expiración es el configurado |

#### `AuthServiceTest` — Tests con Mockito

| Test | Descripción |
|------|-------------|
| `register_conDatosValidos_retornaAuthResponse` | Registro completo devuelve AuthResponse con token |
| `register_sinRol_asignaOperadorPorDefecto` | Si no se envía rol, se asigna OPERADOR |
| `register_emailDuplicado_lanzaExcepcion` | Email ya registrado lanza `IllegalArgumentException` |
| `register_empresaNoExiste_lanzaExcepcion` | Empresa inexistente lanza excepción con el ID |
| `register_contrasena_siempreGuardaCifrada` | La contraseña nunca se guarda en texto plano |
| `login_credencialesValidas_retornaAuthResponse` | Login exitoso devuelve token y datos del usuario |
| `login_credencialesInvalidas_lanzaExcepcion` | Credenciales incorrectas lanzan `BadCredentialsException` |
| `login_exitoso_incluyeExpiresIn` | La respuesta incluye el tiempo de expiración configurado |

#### `EmpresaServiceTest` — Tests con Mockito

| Test | Descripción |
|------|-------------|
| `crear_datosValidos_retornaEmpresaResponse` | Crea empresa y devuelve response con todos los campos |
| `crear_rutDuplicado_lanzaExcepcion` | RUT ya registrado lanza `IllegalArgumentException` |
| `crear_emailDuplicado_lanzaExcepcion` | Email duplicado lanza excepción con el email |
| `crear_llamaAlRepositorioUnaVez` | Se llama a `save()` exactamente una vez |

#### `PedidoControllerTest` — Tests de integración con MockMvc

| Test | Descripción |
|------|-------------|
| `crearPedido_datosValidos_retorna201` | POST válido retorna 201 con datos del pedido |
| `crearPedido_sinAutenticacion_retorna401` | Sin token JWT retorna 401 |
| `crearPedido_bodyVacio_retorna400` | Body vacío activa validación y retorna 400 |
| `crearPedido_sinItems_retorna400` | Lista de items vacía retorna 400 |
| `obtenerPedido_idExistente_retorna200` | Pedido existente retorna 200 con datos completos |
| `obtenerPedido_idInexistente_retorna404` | ID inexistente retorna 404 con mensaje |
| `cambiarEstado_transicionValida_retorna200` | Transición PENDIENTE→CONFIRMADO retorna 200 |
| `cambiarEstado_transicionInvalida_retorna409` | Transición inválida retorna 409 con mensaje |
| `cambiarEstado_sinNuevoEstado_retorna400` | Body sin `nuevoEstado` retorna 400 |
| `obtenerHistorial_retornaLista` | Historial de empresa retorna lista con datos |
| `obtenerHistorial_listaVacia_retorna200ConArrayVacio` | Sin pedidos retorna 200 con array vacío |

### Métricas de cobertura objetivo

| Componente | Cobertura objetivo |
|------------|-------------------|
| `JwtUtil` | ≥ 90% |
| `AuthService` | ≥ 85% |
| `EmpresaService` | ≥ 85% |
| `PedidoController` | ≥ 80% |
| **Total del proyecto** | **≥ 60%** |

---

## 13. Patrones de diseño aplicados

### Patrones de arquitectura

**Microservicios** — El sistema está dividido en servicios independientes (Pedidos, Inventario, Envíos) que se despliegan, escalan y fallan de forma aislada. Una falla en MS-Envíos no afecta la creación de pedidos.

**Event-Driven Architecture (EDA)** — Los microservicios se comunican mediante eventos publicados a RabbitMQ. MS-Pedidos no conoce ni depende de MS-Inventario o MS-Envíos; simplemente publica el evento y los consumidores reaccionan.

**BFF (Backend For Frontend)** — Un gateway intermediario adapta la API al consumo del frontend, evitando que el cliente deba conocer la topología interna de microservicios.

### Patrones de resiliencia

**Outbox Pattern** — Garantiza la entrega "at-least-once" de eventos aunque el broker esté caído en el momento de la confirmación del pedido. El evento se persiste en la misma transacción de negocio y un scheduler lo publica de forma asíncrona.

**Retry con backoff** — El scheduler reintenta eventos fallidos hasta 5 veces, registrando el mensaje de error en cada intento para facilitar la depuración.

### Patrones de diseño GoF

**Builder** — Todas las entidades y DTOs usan el builder de Lombok (`@Builder`) para una construcción legible y sin constructores con muchos parámetros.

**DTO (Data Transfer Object)** — Los objetos de request/response están completamente separados de las entidades JPA. Las entidades nunca se serializan directamente; siempre se mapean a DTOs en la capa de servicio.

**Repository** — Acceso a datos encapsulado en interfaces que extienden `JpaRepository`. La lógica de negocio en los servicios no conoce detalles de SQL.

**Template Method** — `OncePerRequestFilter` define el esqueleto del filtro JWT y delega los pasos concretos (`doFilterInternal`) a `JwtAuthFilter`.

**Strategy (implícito)** — Las transiciones de estado válidas se definen como un mapa inmutable `Map<EstadoPedido, Set<EstadoPedido>>` en `PedidoService`, permitiendo modificar las reglas sin tocar la lógica de validación.

---

*Proyecto desarrollado para DSY1106 Desarrollo Fullstack III — Syncro Logistics Platform*
