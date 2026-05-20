# Reservity Backend — Implementation Plan

> **Status**: Finalized, awaiting implementation. Do **not** start coding without reading this entire document. Existing project state at time of writing: blank Spring Boot 4.0.3 scaffold at `backend/src/Reservity` with only `UlmsApplication.java` and a starter-heavy `pom.xml`. ER diagram lives at `backend/src/Reservity/ER Diagram.md`.

---

## 0. Locked Decisions (do not re-litigate)

| # | Decision | Value |
|---|---|---|
| 1 | Spring Boot version | **4.0.3** (keep — do not downgrade to 3.4.x without explicit user approval) |
| 2 | Java version | **21 LTS** (downgrade from 25 in current pom) |
| 3 | Web stack | **Servlet (`spring-boot-starter-web`)**. Drop WebFlux entirely. |
| 4 | Persistence | **Spring Data JPA** (`spring-boot-starter-data-jpa`). Replace existing `spring-boot-starter-jdbc`. |
| 5 | Database | **PostgreSQL 18-alpine**, schema managed by **Flyway**. `ddl-auto=validate`. |
| 6 | Auth | **Spring Security + JWT** (jjwt). Stateless. BCrypt password hashing. |
| 7 | JWT lifetimes | **Access: 1 hour. Refresh: 7 days.** Refresh tokens persisted for revocation. |
| 8 | DTO mapping | **MapStruct** (`componentModel = "spring"`) |
| 9 | Boilerplate | **Lombok** (provided scope, excluded from fat jar) |
| 10 | API docs | **springdoc-openapi** (Swagger UI for SvelteKit consumers) |
| 11 | Email | `spring-boot-starter-mail` + **Thymeleaf** HTML templates. Async via `@Async`. **MailHog** for local dev. |
| 12 | File storage | **AWS S3 via AWS SDK for Java v2** (`software.amazon.awssdk:s3`). **MinIO** for local dev (S3-compatible). Start with **Option A (backend-proxied uploads)**, plan to migrate to **Option B (presigned URLs)** before production. |
| 13 | Messaging | Barebones REST only. No WebSocket/STOMP yet. |
| 14 | Testing | JUnit 5 + Mockito + `@DataJpaTest`/`@WebMvcTest` + **Testcontainers Postgres** for integration (no H2). |
| 15 | Group ID | `dev.tmmc` (user owns tmmc.dev) |
| 16 | Package | `dev.tmmc.reservity` (rename from `dev.tmmc.ulms`) |
| 17 | Maven artifactId | `reservity` (rename from `ulms`) |
| 18 | DB name | `reservity` (rename from `ulms`) |
| 19 | Main class | `ReservityApplication` (rename from `UlmsApplication`) |
| 20 | AWS region default (dev) | `eu-central-1` |
| 21 | CORS dev origin | `http://localhost:5173` (SvelteKit/Vite default) |
| 22 | Frontend | SvelteKit, separate project, will consume the JSON API. Backend serves no HTML pages (Thymeleaf is **only** for email templates). |
| 23 | Containerization | Docker Compose with services: `postgres`, `mailhog`, `minio`, `minio-init`, `backend`. Multi-stage Dockerfile (Maven build → JRE 21 runtime). |

---

## 1. ER Diagram Summary

Source: `backend/src/Reservity/ER Diagram.md` (Mermaid).

**Entities (10):**
1. **User** — UUID PK, name, email (unique), `passwordHash` (**added**, not in original ER), `accountType` enum {OWNER, REQUESTER, ADMIN}, audit timestamps.
2. **Organization** — UUID PK, name, profileDetails.
3. **Membership** — composite PK (userId, orgId), `role` enum {ADMIN, MEMBER}. Join entity between User ↔ Organization.
4. **Space** — UUID PK, polymorphic ownership (`ownerId` UUID + `ownerType` enum {USER, ORGANIZATION}), title, description, `category` enum {APARTMENT, HALL, OFFICE}, operatingHours, maxCapacity, pricePerHour, pricePerDay, currency, isFree, city, address, latitude, longitude, rulesAndRequirements.
5. **SpaceImage** — UUID PK, FK spaceId, url (S3 object URL or key).
6. **ReservationRequest** — UUID PK, FK spaceId, FK userId (requester), requestedDate, startTime, endTime, purposeDescription, `status` enum {PENDING, APPROVED, DENIED}, createdAt, updatedAt, decisionDate, rejectionReason, contactPhone, attendeesCount.
7. **Reservation** — UUID PK, FK spaceId, FK requestId (source request, one-to-one), startTime, endTime. **Created only when a ReservationRequest is APPROVED.**
8. **Notification** — UUID PK, FK userId, `type` enum {EMAIL, IN_APP}, message, read (boolean), createdAt.
9. **Message** — UUID PK, FK senderId, FK receiverId, content, timestamp.
10. **Review** — UUID PK, FK reviewerId, polymorphic target (`targetId` UUID + `targetType` enum {USER, ORGANIZATION}), rating (int), comment.

**Enums to define as Java enums** (stored as `EnumType.STRING` in DB, never ordinal):
`AccountType`, `MembershipRole`, `OwnerType`, `SpaceCategory`, `RequestStatus`, `NotificationType`, `ReviewTargetType`.

**Polymorphism note**: Both `Space` (owner) and `Review` (target) use **explicit discriminator columns** (`ownerType`/`targetType` + `ownerId`/`targetId`), **NOT** Hibernate `@Any` or JPA `@Inheritance`. Resolution happens at the service layer by branching on the type enum.

---

## 2. pom.xml — Required Changes

The current `backend/src/Reservity/pom.xml` needs the following edits before any code is written. **Read the existing file first** to preserve unrelated structure.

### Property changes
- `<java.version>25</java.version>` → `<java.version>21</java.version>`
- `<artifactId>ulms</artifactId>` → `<artifactId>reservity</artifactId>`
- `<name>ulms</name>` → `<name>reservity</name>`
- `<description>` → `Reservity backend — university space reservation system`

### Remove these dependencies
- `spring-boot-starter-jdbc` (replaced by JPA)
- `spring-boot-starter-webflux` + `spring-boot-starter-webflux-test`
- `spring-boot-starter-webmvc` (NOTE: in SB 4.x the artifact is named `spring-boot-starter-web`. Verify against the actual SB 4.0.3 BOM — the existing pom uses `webmvc` which may be a typo or a renamed artifact. Confirm before editing.)
- `thymeleaf-extras-springsecurity6` (only useful for server-rendered Thymeleaf views)

### Add these dependencies
```xml
<!-- Replace jdbc with JPA -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Servlet web stack (verify exact artifactId for SB 4.0.3) -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <scope>provided</scope>
</dependency>

<!-- MapStruct -->
<dependency>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct</artifactId>
  <version>1.6.3</version>
</dependency>
<!-- mapstruct-processor goes in maven-compiler-plugin annotationProcessorPaths -->

<!-- JWT (jjwt) -->
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.6</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>

<!-- OpenAPI / Swagger UI -->
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
  <!-- WARNING: verify compatibility with Spring Boot 4.0.3. springdoc historically lags major SB releases.
       If 2.6.0 is incompatible, check for a 2.7+ release or temporarily fall back to manual OpenAPI YAML. -->
</dependency>

<!-- AWS S3 (SDK v2) -->
<!-- Add BOM in <dependencyManagement> -->
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>s3</artifactId>
</dependency>

<!-- Testcontainers (test scope) -->
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
```

### dependencyManagement additions
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>bom</artifactId>
      <version>2.28.16</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-bom</artifactId>
      <version>1.20.4</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### Build plugin updates
- Add `maven-compiler-plugin` configuration with `<annotationProcessorPaths>` containing **Lombok** and **MapStruct processor**.
- Configure `spring-boot-maven-plugin` to **exclude Lombok** from the repackaged fat jar:
```xml
<configuration>
  <excludes>
    <exclude>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
    </exclude>
  </excludes>
</configuration>
```

### Heads-up
- **Spring Boot 4.0.3 is brand new.** Some artifact names may have shifted from 3.x. Always verify against the actual BOM. The existing pom lists oddities like `spring-boot-starter-webmvc` (not `-web`) and `spring-boot-starter-actuator-test` etc. Treat the pom as authoritative for what artifact names SB 4.x actually uses; verify each one before deletion or substitution.
- If `springdoc-openapi-starter-webmvc-ui` fails to resolve against SB 4.0.3, do **not** silently downgrade Spring Boot. Flag to the user and either skip Swagger UI temporarily or find a compatible springdoc version.

---

## 3. Package Rename

All files under `backend/src/Reservity/src/main/java/dev/tmmc/ulms/` and `.../src/test/java/dev/tmmc/ulms/` must move to `dev/tmmc/reservity/`. Specifically:

- Move `UlmsApplication.java` → `ReservityApplication.java` under the new package.
- Update class name and `@SpringBootApplication`-annotated class.
- Update `UlmsApplicationTests.java` → `ReservityApplicationTests.java`.
- Delete the old `dev/tmmc/ulms/` directory.
- Verify `application.yml`/`application.properties` `spring.application.name` → `reservity`.
- Delete stale build artifacts: `target/`, `compile_log*.txt`, `compile_result.txt`.

**The folder `backend/src/Reservity/` itself stays as-is** — it already matches the new name.

---

## 4. Final Package & Module Layout

Feature-sliced layout. Each feature contains its own `entity/`, `repository/`, `service/`, `controller/`, `dto/`, `mapper/` subpackages.

```
dev.tmmc.reservity
├── ReservityApplication.java
│
├── common/
│   ├── config/
│   │   ├── SecurityConfig.java          # filter chain, CORS, public matchers
│   │   ├── JpaConfig.java               # @EnableJpaAuditing
│   │   ├── OpenApiConfig.java           # springdoc bean customizations
│   │   ├── MailConfig.java              # JavaMailSender bean (if not autoconfigured)
│   │   ├── AsyncConfig.java             # @EnableAsync + TaskExecutor
│   │   ├── S3Config.java                # S3Client + S3Presigner beans
│   │   └── WebConfig.java               # CORS, Jackson customizations if needed
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   │   ├── ApiError.java                # error response DTO
│   │   ├── EntityNotFoundException.java
│   │   ├── ReservationConflictException.java
│   │   ├── IllegalStatusTransitionException.java
│   │   └── ForbiddenOperationException.java
│   ├── security/
│   │   ├── JwtService.java              # issue/parse/validate
│   │   ├── JwtAuthFilter.java           # OncePerRequestFilter
│   │   ├── CustomUserDetailsService.java
│   │   ├── SecurityUser.java            # UserDetails wrapper
│   │   ├── PasswordEncoderConfig.java   # BCryptPasswordEncoder bean
│   │   └── CurrentUser.java             # @AuthenticationPrincipal helper
│   ├── audit/
│   │   └── BaseEntity.java              # @MappedSuperclass: id, createdAt, updatedAt
│   ├── pagination/
│   │   ├── PageResponse.java            # stable pagination wrapper
│   │   └── PageMapper.java
│   └── storage/
│       ├── StorageService.java          # interface
│       ├── S3StorageService.java        # AWS SDK v2 impl
│       └── StorageProperties.java       # @ConfigurationProperties("reservity.storage.s3")
│
├── auth/
│   ├── AuthController.java              # /api/auth/{register,login,refresh,me}
│   ├── AuthService.java
│   ├── RefreshToken.java                # entity (id, userId, tokenHash, expiresAt, revoked)
│   ├── RefreshTokenRepository.java
│   └── dto/
│       ├── RegisterRequest.java
│       ├── LoginRequest.java
│       ├── RefreshRequest.java
│       └── AuthResponse.java            # accessToken, refreshToken, expiresIn
│
├── user/
│   ├── entity/User.java
│   ├── repository/UserRepository.java
│   ├── service/UserService.java
│   ├── controller/UserController.java   # /api/users/{me,{id}}
│   ├── dto/{UserResponse, UserUpdateRequest}.java
│   └── mapper/UserMapper.java
│
├── organization/
│   ├── entity/{Organization, Membership, MembershipId}.java
│   ├── repository/{OrganizationRepository, MembershipRepository}.java
│   ├── service/{OrganizationService, MembershipService}.java
│   ├── controller/OrganizationController.java
│   ├── dto/{...}.java
│   └── mapper/{OrganizationMapper, MembershipMapper}.java
│
├── space/
│   ├── entity/{Space, SpaceImage}.java
│   ├── repository/{SpaceRepository, SpaceImageRepository}.java
│   ├── repository/SpaceSpecifications.java   # JpaSpecificationExecutor filters
│   ├── service/{SpaceService, SpaceImageService}.java
│   ├── controller/{SpaceController, SpaceImageController}.java
│   ├── dto/{SpaceCreateRequest, SpaceUpdateRequest, SpaceResponse, SpaceSearchCriteria, SpaceImageResponse}.java
│   └── mapper/{SpaceMapper, SpaceImageMapper}.java
│
├── reservation/
│   ├── entity/{ReservationRequest, Reservation}.java
│   ├── repository/{ReservationRequestRepository, ReservationRepository}.java
│   ├── service/{ReservationRequestService, ReservationService, ReservationOverlapValidator, RequestStatusMachine}.java
│   ├── controller/{ReservationRequestController, ReservationController}.java
│   ├── dto/{...}.java
│   ├── mapper/{...}.java
│   └── event/{ReservationRequestedEvent, ReservationDecidedEvent}.java
│
├── notification/
│   ├── entity/Notification.java
│   ├── repository/NotificationRepository.java
│   ├── service/{NotificationService, EmailService, EmailTemplateService}.java
│   ├── controller/NotificationController.java
│   ├── dto/{...}.java
│   ├── mapper/NotificationMapper.java
│   └── listener/ReservationEventListener.java   # @EventListener — turns events into notifications
│
├── messaging/
│   ├── entity/Message.java
│   ├── repository/MessageRepository.java
│   ├── service/MessageService.java
│   ├── controller/MessageController.java
│   ├── dto/{MessageRequest, MessageResponse}.java
│   ├── mapper/MessageMapper.java
│   └── event/MessageReceivedEvent.java
│
└── review/
    ├── entity/Review.java
    ├── repository/ReviewRepository.java
    ├── service/ReviewService.java
    ├── controller/ReviewController.java
    ├── dto/{...}.java
    └── mapper/ReviewMapper.java
```

### Resources

```
src/main/resources/
├── application.yml                      # defaults
├── application-dev.yml                  # MailHog, MinIO, verbose SQL, dev seed
├── application-prod.yml                 # env-driven, no SQL logging
├── application-test.yml                 # Testcontainers
├── db/migration/
│   ├── V1__create_extensions.sql
│   ├── V2__create_users.sql
│   ├── V3__create_organizations_and_memberships.sql
│   ├── V4__create_spaces_and_images.sql
│   ├── V5__create_reservations.sql
│   ├── V6__create_notifications.sql
│   ├── V7__create_messages.sql
│   ├── V8__create_reviews.sql
│   ├── V9__create_refresh_tokens.sql
│   ├── V10__create_indexes.sql
│   └── V11__seed_dev_data.sql           # only loaded in dev profile (use Flyway placeholders or separate location)
└── templates/email/
    ├── reservation-requested.html
    ├── reservation-approved.html
    ├── reservation-denied.html
    └── new-message.html
```

---

## 5. Persistence Strategy

- **UUID PKs** — `@Id @GeneratedValue(strategy = GenerationType.UUID)` (Hibernate 6+ generates client-side).
- **BaseEntity** (`@MappedSuperclass`) — `id`, `createdAt` (`@CreatedDate`), `updatedAt` (`@LastModifiedDate`). Enable with `@EnableJpaAuditing` in `JpaConfig`. Apply `@EntityListeners(AuditingEntityListener.class)`.
- **Membership composite key** — `@Embeddable MembershipId { UUID userId; UUID orgId; }`. Membership entity uses `@EmbeddedId` and `@MapsId` on the two `@ManyToOne` relations.
- **Polymorphic owner/target** — store `ownerId UUID NOT NULL` + `ownerType VARCHAR NOT NULL` (`@Enumerated(STRING)`). Add **DB-level CHECK constraint** in Flyway to enforce enum values. Service layer resolves the actual owner via `if (ownerType == USER) userRepo.findById(...) else organizationRepo.findById(...)`. Same for `Review.targetType`/`targetId`.
- **Lazy by default** — every `@ManyToOne` and `@OneToMany` is `FetchType.LAZY`. Use `@EntityGraph` or explicit `JOIN FETCH` JPQL for hot read paths (e.g., listing spaces with their images).
- **Enums** — always `@Enumerated(EnumType.STRING)`. Never ordinal.
- **Soft delete** — not in scope. Hard deletes only.
- **Auditing of *who*** (`@CreatedBy`/`@LastModifiedBy`) — out of scope for MVP. Can be added later via an `AuditorAware` bean reading `SecurityContextHolder`.
- **JpaSpecificationExecutor** — extended by `SpaceRepository` for dynamic filter queries (city, category, capacity range, price range, isFree).

### Concurrency: reservation overlap protection

The reservation approve flow has a race condition: two pending requests for overlapping times could both pass validation and both get approved. Mitigation:

1. **Application-level**: in `ReservationRequestService.approve(...)`, re-run overlap validation inside the transaction.
2. **Pessimistic lock on Space row** — `@Lock(LockModeType.PESSIMISTIC_WRITE)` query method on `SpaceRepository.findByIdForUpdate(id)`. Call before approving.
3. **DB-level safety net (recommended)** — Postgres exclusion constraint on `reservations` using `tstzrange` and `&&` operator + GiST index. Add in Flyway:
   ```sql
   ALTER TABLE reservations ADD CONSTRAINT no_overlap
     EXCLUDE USING gist (space_id WITH =, tstzrange(start_time, end_time) WITH &&);
   ```
   Requires `btree_gist` extension (add in V1).

---

## 6. Authentication & Security Details

### Endpoints
```
POST /api/auth/register     -> 201 + AuthResponse
POST /api/auth/login        -> 200 + AuthResponse
POST /api/auth/refresh      -> 200 + AuthResponse (rotates refresh token)
GET  /api/auth/me           -> 200 + UserResponse  (requires bearer)
POST /api/auth/logout       -> 204 (revokes refresh token)
```

### Token strategy
- **Access token (JWT)** — 1 hour TTL. HS256 signed with `JWT_SECRET` (env var, min 256 bits). Claims: `sub=userId`, `email`, `accountType`, `iat`, `exp`. Stateless — never persisted.
- **Refresh token** — 7 day TTL. Random 256-bit token, stored **hashed** (SHA-256) in `refresh_tokens` table with `userId`, `tokenHash`, `expiresAt`, `revoked`. Rotated on each use (old one revoked, new one issued).
- **JwtAuthFilter** — extends `OncePerRequestFilter`, runs before `UsernamePasswordAuthenticationFilter`. Extracts `Authorization: Bearer <token>`, validates, populates `SecurityContextHolder` with `UsernamePasswordAuthenticationToken`.
- **CustomUserDetailsService** — loads by email, returns `SecurityUser` (wraps `User` entity, exposes authorities derived from `accountType`).

### SecurityConfig
- `SessionCreationPolicy.STATELESS`
- CSRF disabled (JWT, no cookies for auth)
- CORS configured via dedicated `CorsConfigurationSource` bean — allowed origins: `http://localhost:5173` (dev), prod origin via env var. Allowed methods: GET, POST, PATCH, PUT, DELETE, OPTIONS. Allowed headers: `*`. `allowCredentials=false` (using bearer tokens, not cookies).
- Public matchers:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
  - `GET /v3/api-docs/**`
  - `GET /swagger-ui/**`
  - `GET /actuator/health`
- Everything else: `.authenticated()`
- `@EnableMethodSecurity` for `@PreAuthorize` on owner/admin checks.
- **Method-level guards** via dedicated `@Component` beans, e.g. `@PreAuthorize("@spaceSecurity.isOwner(#id, principal)")`.

### Password storage
- `User.passwordHash` is a `VARCHAR(60)` column (BCrypt output length). **Never** included in any DTO. `@JsonIgnore` on the entity field as belt-and-braces.
- BCrypt strength 12 (default 10 is fine too — confirm with user if needed).

---

## 7. DTO & Mapping Conventions

- **Three DTOs per entity** typically: `XxxCreateRequest`, `XxxUpdateRequest`, `XxxResponse`. Patch-style updates use `@JsonInclude(NON_NULL)` semantics (null = don't update).
- **Bean validation** on request DTOs: `@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`, `@Future`, `@FutureOrPresent`, etc. Triggered by `@Valid` on controller params.
- **MapStruct mappers** — one per feature, `@Mapper(componentModel = "spring")`. Inject into services like any Spring bean. Use `@Mapping(target="x", ignore=true)` for fields the service sets manually.
- **Never expose entities through controllers.** Always map to a Response DTO first.
- **Pagination wrapper** — `PageResponse<T> { List<T> content; int page; int size; long totalElements; int totalPages; boolean hasNext; }`. Avoid returning Spring's `Page<T>` directly because its serialized JSON shape is not part of Spring's stable contract.

---

## 8. REST API Surface

```
# Auth (public except /me)
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/logout
GET    /api/auth/me

# Users
GET    /api/users/{id}
PATCH  /api/users/me

# Organizations
GET    /api/organizations                               # paginated, filterable
POST   /api/organizations
GET    /api/organizations/{id}
PATCH  /api/organizations/{id}                          # admin of org only
DELETE /api/organizations/{id}                          # admin of org only

# Memberships
GET    /api/organizations/{id}/members
POST   /api/organizations/{id}/members                  # invite/add user
DELETE /api/organizations/{id}/members/{userId}
PATCH  /api/organizations/{id}/members/{userId}         # change role

# Spaces
GET    /api/spaces                                      # filters: city, category, minCapacity, maxPrice, isFree, q (text)
POST   /api/spaces                                      # owner sets ownerType+ownerId from auth context
GET    /api/spaces/{id}
PATCH  /api/spaces/{id}                                 # owner only
DELETE /api/spaces/{id}                                 # owner only

# Space images (Option A: backend-proxied multipart)
GET    /api/spaces/{id}/images
POST   /api/spaces/{id}/images                          # multipart/form-data, field "file"
DELETE /api/spaces/{id}/images/{imageId}

# Reservation requests
POST   /api/spaces/{id}/reservation-requests            # requester submits
GET    /api/reservation-requests/mine                   # requests I've submitted
GET    /api/reservation-requests/received               # requests for spaces I own
GET    /api/reservation-requests/{id}
POST   /api/reservation-requests/{id}/approve           # owner only
POST   /api/reservation-requests/{id}/deny              # owner only, body: {reason}

# Reservations (read-only; created by approve flow)
GET    /api/reservations/mine
GET    /api/reservations?spaceId={id}                   # public availability view

# Notifications
GET    /api/notifications                               # paginated, current user's
PATCH  /api/notifications/{id}/read
POST   /api/notifications/read-all

# Messages
GET    /api/messages?with={userId}                      # conversation between current user and other
POST   /api/messages                                    # body: {receiverId, content}

# Reviews
GET    /api/reviews?targetType={USER|ORGANIZATION}&targetId={uuid}
POST   /api/reviews                                     # body: {targetType, targetId, rating, comment}
```

All under `/api`, JSON only. OpenAPI auto-generated by springdoc; expose at `/swagger-ui.html`.

---

## 9. Reservation Domain Logic (the core flow)

This is the most error-prone part. Pin down the rules clearly:

### Submission (`POST /api/spaces/{id}/reservation-requests`)
1. Caller must be authenticated (any account type).
2. Validate `startTime < endTime`, both `@Future`.
3. Validate window falls within Space's `operatingHours` (parse the stored string — define a format like `"MON-FRI 08:00-22:00, SAT 09:00-18:00"` and write a parser; or simplify to `openTime/closeTime` columns and update the ER if needed — flag this to user before implementing).
4. Validate `attendeesCount <= space.maxCapacity`.
5. Check no overlap with existing **APPROVED** `Reservation`s for that space.
6. Persist `ReservationRequest` with `status=PENDING`.
7. Publish `ReservationRequestedEvent(requestId, spaceId, ownerId)` via `ApplicationEventPublisher`.
8. Return 201 + `ReservationRequestResponse`.

### Approval (`POST /api/reservation-requests/{id}/approve`)
1. Caller must be the space owner (User or Org admin if `ownerType=ORGANIZATION`).
2. Load request inside transaction. If `status != PENDING`, throw `IllegalStatusTransitionException` (409).
3. Acquire pessimistic lock on the Space row.
4. **Re-run overlap validation** against existing APPROVED reservations.
5. Create `Reservation` row. DB exclusion constraint is the final safety net.
6. Set `request.status = APPROVED`, `decisionDate = now()`. Save.
7. Publish `ReservationDecidedEvent(requestId, APPROVED)`.
8. Return 200 + updated `ReservationRequestResponse`.

### Denial (`POST /api/reservation-requests/{id}/deny`)
1. Same authorization as approve.
2. If `status != PENDING`, throw `IllegalStatusTransitionException`.
3. Set `status = DENIED`, `decisionDate = now()`, `rejectionReason = body.reason`.
4. Publish `ReservationDecidedEvent(requestId, DENIED)`.
5. Return 200.

### State machine
Encode in `RequestStatusMachine` helper:
- `PENDING → APPROVED` ✅
- `PENDING → DENIED` ✅
- Any other transition → `IllegalStatusTransitionException`.

---

## 10. Notification System

### Architecture
- Event-driven via Spring's `ApplicationEventPublisher` / `@EventListener`. Keeps services decoupled.
- `NotificationService.notify(userId, type, templateKey, model)` is the single entry point.
- **In-app**: persist a `Notification` row. Frontend polls `GET /api/notifications`. (Upgrade to SSE later if needed.)
- **Email**: Thymeleaf renders HTML template from `templates/email/`, `JavaMailSender` sends. Wrapped in `@Async` so the request thread is never blocked on SMTP.

### Triggers (in `notification/listener/`)
- `ReservationRequestedEvent` → notify space owner (in-app + email).
- `ReservationDecidedEvent` → notify requester (in-app + email).
- `MessageReceivedEvent` → notify recipient (in-app only by default; email is noisy for chat).

### Async configuration
- `AsyncConfig` enables `@EnableAsync` and defines a `TaskExecutor` bean named `notificationExecutor` with sensible pool sizes (core 2, max 8, queue 100).
- `EmailService.send(...)` annotated `@Async("notificationExecutor")`.
- **Important**: `@Async` methods cannot be called from within the same bean (proxy bypass). Always call `EmailService` from a different bean.

### Templates
Thymeleaf HTML templates in `src/main/resources/templates/email/`. `EmailTemplateService` wraps `SpringTemplateEngine.process(templateName, context)`. Pass model as a `Map<String, Object>`.

### Local dev: MailHog
- MailHog container in docker-compose. SMTP on `1025`, web UI on `8025`.
- `application-dev.yml`:
  ```yaml
  spring.mail:
    host: mailhog
    port: 1025
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false
  ```
- Devs view sent emails at `http://localhost:8025`.

---

## 11. File Storage (S3) — Implementation Spec

### Approach: Option A first (backend-proxied multipart uploads)
Rationale: simpler to ship, consistent validation, easy local dev with MinIO. Migrate to Option B (presigned URLs) before production deployment to reduce backend bandwidth. The `StorageService` interface hides the difference.

### `StorageService` interface
```java
public interface StorageService {
    StoredObject upload(InputStream content, long contentLength, String contentType, String key);
    void delete(String key);
    String publicUrl(String key);
    URL presignPut(String key, String contentType, Duration ttl);   // for future Option B
    URL presignGet(String key, Duration ttl);                       // for private buckets
}

public record StoredObject(String key, String url, String contentType, long size) {}
```

### `S3StorageService` (AWS SDK v2)
- Uses `S3Client` (sync) for `putObject`/`deleteObject`.
- Uses `S3Presigner` for presigned URLs.
- Both beans configured in `S3Config`. In dev profile, `endpointOverride(URI.create("http://minio:9000"))` and `forcePathStyle(true)` are set so the same code talks to MinIO.
- Credentials resolved via the AWS SDK default credentials chain (env vars → profile → IAM role). For MinIO, set `AWS_ACCESS_KEY_ID=minioadmin` and `AWS_SECRET_ACCESS_KEY=minioadmin` env vars on the backend container.

### `StorageProperties`
```yaml
reservity:
  storage:
    s3:
      bucket: reservity-uploads
      region: eu-central-1
      endpoint-override:           # set in dev only
      path-style-access: false     # true in dev
      public-base-url:             # optional CDN/CloudFront override
```

### Upload validation
Configured in `application.yml`:
```yaml
spring.servlet.multipart:
  max-file-size: 5MB
  max-request-size: 25MB
  enabled: true
```
Service-layer checks:
- Content-type in `{image/jpeg, image/png, image/webp}`.
- Max 10 images per space.
- Generated key format: `spaces/{spaceId}/{uuid}.{ext}`.
- Stored URL: `${publicBaseUrl}/{key}` if set, otherwise `https://{bucket}.s3.{region}.amazonaws.com/{key}` (or MinIO equivalent in dev).

### `SpaceImage.url` column
Stores the **full public URL** (not just the key) so the frontend doesn't need to know about S3 structure. Tradeoff: if the bucket/CDN URL changes, you need a migration. Acceptable for this project.

### Future migration to Option B (presigned URLs)
When ready for production, add two endpoints:
```
POST /api/spaces/{id}/images/presign  -> { uploadUrl, key, expiresAt }
POST /api/spaces/{id}/images/confirm  -> { key } -> persists SpaceImage row
```
The existing `POST /api/spaces/{id}/images` (Option A) can stay as a fallback or be removed.

---

## 12. Configuration Files

### `application.yml` (defaults — committed)
```yaml
spring:
  application:
    name: reservity
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
      hibernate.jdbc.time_zone: UTC
    open-in-view: false
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 25MB

reservity:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-token-ttl: 1h
      refresh-token-ttl: 7d
  cors:
    allowed-origins:
      - ${CORS_ALLOWED_ORIGIN:http://localhost:5173}
  storage:
    s3:
      bucket: ${AWS_S3_BUCKET}
      region: ${AWS_REGION:eu-central-1}
      endpoint-override: ${AWS_S3_ENDPOINT:}
      path-style-access: ${AWS_S3_PATH_STYLE:false}
      public-base-url: ${AWS_S3_PUBLIC_URL:}

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

### `application-dev.yml`
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate.format_sql: true
  mail:
    host: mailhog
    port: 1025
    properties:
      mail.smtp.auth: false

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.springframework.security: DEBUG

reservity:
  storage:
    s3:
      bucket: reservity-uploads
      endpoint-override: http://minio:9000
      path-style-access: true
      public-base-url: http://localhost:9000/reservity-uploads
```

### `application-prod.yml`
- Everything env-driven. No SQL logging. Real SMTP. Real S3 (no endpoint override).

### `application-test.yml`
- Testcontainers JDBC URL (`jdbc:tc:postgresql:18:///reservity`).
- Flyway enabled.
- Mail uses `spring.mail.host=localhost` with a captured `JavaMailSender` mock or GreenMail if needed.

### Secrets management
- `JWT_SECRET`, DB creds, AWS creds, SMTP creds → **env vars only**, never committed.
- Add a `.env.example` at repo root documenting required vars.
- `.env` in `.gitignore`.

---

## 13. Error Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps:

| Exception | HTTP | Notes |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Include `fieldErrors: [{field, message}]` |
| `ConstraintViolationException` | 400 | For `@Validated` on path/query params |
| `EntityNotFoundException` (custom) | 404 | |
| `BadCredentialsException` | 401 | Generic message — don't leak which field was wrong |
| `AccessDeniedException` | 403 | |
| `ForbiddenOperationException` (custom) | 403 | |
| `IllegalStatusTransitionException` | 409 | |
| `ReservationConflictException` | 409 | Overlap detected |
| `DataIntegrityViolationException` | 409 | Unique constraint, FK, etc. |
| `MaxUploadSizeExceededException` | 413 | |
| `Exception` (catch-all) | 500 | Sanitized message; full stack in logs only |

### `ApiError` shape
```json
{
  "timestamp": "2026-04-07T12:34:56Z",
  "status": 404,
  "error": "Not Found",
  "message": "Space with id ... not found",
  "path": "/api/spaces/abc",
  "fieldErrors": [
    { "field": "email", "message": "must be a valid email" }
  ]
}
```

---

## 14. Testing Strategy

| Layer | Tooling | Scope |
|---|---|---|
| Unit | JUnit 5 + Mockito | Service logic, mappers (no Spring context) |
| Repository | `@DataJpaTest` + Testcontainers Postgres | Real Postgres, real Flyway, real SQL. **No H2.** |
| Web | `@WebMvcTest` + MockMvc + `@MockBean` services + `spring-security-test` | Controller routing, validation, security rules |
| Integration | `@SpringBootTest(webEnvironment=RANDOM_PORT)` + Testcontainers + `TestRestTemplate` | Full reservation flow end-to-end |

### Key tests to write per milestone
- **Milestone 1 (auth)**: register, login, refresh, /me, expired token, invalid token, password mismatch.
- **Milestone 2 (reservation flow)**: submit → approve → reservation created; submit overlapping → 409; approve already-approved → 409; non-owner approval → 403; DB exclusion constraint catches concurrent approves.
- **Notifications**: event published → notification row exists; email template renders without exception.

Use a shared `AbstractIntegrationTest` base class with a `@Container` static Postgres so it starts once per JVM.

---

## 15. Docker Setup

### `docker-compose.yml` (replace existing)

```yaml
services:
  postgres:
    container_name: reservity_postgres
    image: postgres:18-alpine
    ports:
      - "5431:5432"
    environment:
      POSTGRES_USER: reservity
      POSTGRES_PASSWORD: reservity
      POSTGRES_DB: reservity
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks: [reservity-net]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U reservity -d reservity"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  mailhog:
    container_name: reservity_mailhog
    image: mailhog/mailhog
    ports:
      - "1025:1025"   # SMTP
      - "8025:8025"   # Web UI
    networks: [reservity-net]
    restart: unless-stopped

  minio:
    container_name: reservity_minio
    image: minio/minio
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"   # API
      - "9001:9001"   # Web UI
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio-data:/data
    networks: [reservity-net]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  minio-init:
    container_name: reservity_minio_init
    image: minio/mc
    depends_on:
      minio:
        condition: service_healthy
    networks: [reservity-net]
    entrypoint: >
      /bin/sh -c "
      mc alias set local http://minio:9000 minioadmin minioadmin;
      mc mb --ignore-existing local/reservity-uploads;
      mc anonymous set download local/reservity-uploads;
      exit 0;
      "

  backend:
    container_name: reservity_backend
    build:
      context: ./src/Reservity
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/reservity
      SPRING_DATASOURCE_USERNAME: reservity
      SPRING_DATASOURCE_PASSWORD: reservity
      JWT_SECRET: ${JWT_SECRET:-change-me-in-production-use-a-real-256-bit-secret}
      AWS_S3_BUCKET: reservity-uploads
      AWS_REGION: eu-central-1
      AWS_S3_ENDPOINT: http://minio:9000
      AWS_S3_PATH_STYLE: "true"
      AWS_S3_PUBLIC_URL: http://localhost:9000/reservity-uploads
      AWS_ACCESS_KEY_ID: minioadmin
      AWS_SECRET_ACCESS_KEY: minioadmin
      CORS_ALLOWED_ORIGIN: http://localhost:5173
    depends_on:
      postgres:
        condition: service_healthy
      mailhog:
        condition: service_started
      minio:
        condition: service_healthy
    networks: [reservity-net]
    restart: unless-stopped

volumes:
  postgres-data: {}
  minio-data: {}

networks:
  reservity-net:
    driver: bridge
```

### `Dockerfile` (multi-stage, replaces existing)

```dockerfile
# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/reservity-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### `.dockerignore` (in `src/Reservity/`)
```
target/
.idea/
.vscode/
*.iml
compile_log*.txt
compile_result.txt
HELP.md
```

---

## 16. Implementation Order & Milestones

Each milestone should be a working, committable state. Run tests and `docker compose up` between milestones to verify.

### Milestone 0 — Foundation
1. Read existing `pom.xml`, fix per Section 2.
2. Rename package per Section 3 (delete old `dev/tmmc/ulms/`, create `dev/tmmc/reservity/`).
3. Create the package layout from Section 4 as empty packages with `package-info.java` placeholders if needed.
4. Add `application.yml`/`application-dev.yml`/`application-prod.yml`/`application-test.yml`.
5. Implement `BaseEntity`, `JpaConfig`, `OpenApiConfig`, `WebConfig` (CORS), `AsyncConfig`, `GlobalExceptionHandler`, `ApiError`, custom exceptions.
6. Update `docker-compose.yml` and `Dockerfile` per Section 15.
7. Verify: `docker compose up` starts all services; backend reaches Postgres; Flyway runs (no migrations yet but no errors); `/actuator/health` returns UP; `/swagger-ui.html` loads.

### Milestone 1 — Auth (the first real feature)
1. Flyway `V1__create_extensions.sql` (`pgcrypto`, `btree_gist`).
2. Flyway `V2__create_users.sql` and `V9__create_refresh_tokens.sql`.
3. `User` entity, `UserRepository`.
4. `RefreshToken` entity, `RefreshTokenRepository`.
5. `JwtService`, `JwtAuthFilter`, `CustomUserDetailsService`, `SecurityUser`, `PasswordEncoderConfig`, `SecurityConfig`.
6. `AuthService`, `AuthController`, auth DTOs.
7. Tests: register/login/refresh/me/logout happy paths + failure cases.
8. **Verify end-to-end**: hit endpoints with curl/Postman, confirm JWTs work, refresh rotation works, protected endpoint returns 401 without token.

### Milestone 2 — Users & Organizations
1. Flyway `V3__create_organizations_and_memberships.sql`.
2. `Organization`, `Membership`, `MembershipId` entities.
3. Repositories, services, controllers, DTOs, mappers.
4. Authorization rules (org admin can edit, members can read).
5. Tests.

### Milestone 3 — Spaces (without images)
1. Flyway `V4__create_spaces_and_images.sql` (both tables, but image upload comes next milestone).
2. `Space`, `SpaceImage` entities.
3. `SpaceRepository extends JpaRepository<Space, UUID>, JpaSpecificationExecutor<Space>`.
4. `SpaceSpecifications` for filter queries.
5. Service, controller, DTOs, mapper.
6. Owner authorization (`@spaceSecurity.isOwner(...)`).
7. Tests.

### Milestone 4 — File storage + Space images
1. `StorageService` interface, `S3StorageService`, `StorageProperties`, `S3Config`.
2. Verify MinIO works end-to-end via integration test.
3. `SpaceImageController` with multipart upload endpoint.
4. Validation (size, content-type, count).
5. Tests using Testcontainers MinIO (`org.testcontainers:minio` if available, else manual container).

### Milestone 5 — Reservation flow (the core)
1. Flyway `V5__create_reservations.sql` with exclusion constraint.
2. `ReservationRequest`, `Reservation` entities.
3. `ReservationOverlapValidator`, `RequestStatusMachine`.
4. `ReservationRequestService` with submit/approve/deny.
5. Pessimistic locking on Space.
6. Events published.
7. Controllers, DTOs, mappers.
8. **Critical tests**: overlap detection, concurrent approve race (use `@Transactional` + parallel threads or just trust the DB constraint and test it directly), illegal state transitions.

### Milestone 6 — Notifications
1. Flyway `V6__create_notifications.sql`.
2. `Notification` entity, repository, service.
3. `EmailService` (`@Async`), `EmailTemplateService`, Thymeleaf templates.
4. `ReservationEventListener` wires events → notifications.
5. `NotificationController` (list, mark read).
6. Verify: trigger a reservation request, see notification row + email in MailHog UI.

### Milestone 7 — Messages
1. Flyway `V7__create_messages.sql`.
2. `Message` entity, repository, service, controller, DTO, mapper.
3. `MessageReceivedEvent` → notification listener.
4. Tests.

### Milestone 8 — Reviews
1. Flyway `V8__create_reviews.sql`.
2. `Review` entity (polymorphic target), repository, service, controller, DTO, mapper.
3. Validation (rating 1-5, target must exist, one review per reviewer per target?).
4. Tests.

### Milestone 9 — Indexes, polish, docs
1. Flyway `V10__create_indexes.sql` — email unique index, FK indexes, search indexes.
2. `V11__seed_dev_data.sql` for the dev profile (configure Flyway placeholders or a separate `db/dev` location loaded only with `spring.flyway.locations` override in `application-dev.yml`).
3. README at `backend/` with: prerequisites, `docker compose up` instructions, env var list, how to run tests, how to view MailHog/MinIO/Swagger.
4. `.env.example`.

---

## 17. Open Questions for Each New Implementation Session

When picking up work, **ask the user before assuming** if any of these aren't yet decided in this doc or in code:

1. **`Space.operatingHours` format** — string parser format not yet defined. Options:
   - Free-form string (no validation, just display).
   - Structured: split into `openTime TIME, closeTime TIME, daysOfWeek VARCHAR` columns. **Requires ER update.**
   - JSON column with weekly schedule.
   - **Recommendation**: start with free-form string for MVP, validate `startTime/endTime` against requested date only, leave operating-hours enforcement as a TODO. Confirm with user.
2. **One review per reviewer per target?** — add a unique constraint, or allow multiple? Not specified in ER.
3. **Soft delete** for any entity? — currently planned: no, hard delete only.
4. **`@CreatedBy`/`@LastModifiedBy` auditing** — not in MVP scope, ok?
5. **BCrypt strength** — default 10 fine, or bump to 12?
6. **Refresh token reuse detection** — if a revoked refresh token is presented, should we revoke ALL of that user's refresh tokens (security best practice) or just reject the one? **Recommendation**: revoke all.
7. **Email verification flow** — not in MVP. Users are active immediately after register. Confirm.
8. **Password reset flow** — not in MVP. Confirm.

---

## 18. What NOT to Do (Guardrails)

- **Do not** add Spring Cloud AWS — use the AWS SDK v2 directly.
- **Do not** use H2 anywhere, including tests. Testcontainers Postgres only.
- **Do not** use `@Inheritance` or `@Any` for polymorphic relations. Use explicit discriminator columns.
- **Do not** expose JPA entities through controllers. Always map to DTOs.
- **Do not** store enums as ordinals.
- **Do not** call `@Async` methods from within the same bean (proxy bypass — silently runs synchronously).
- **Do not** put `JWT_SECRET`, AWS keys, or DB passwords in committed files.
- **Do not** silently downgrade Spring Boot to 3.x if a dependency is incompatible — ask the user first.
- **Do not** add server-side rendering. Thymeleaf is for email templates **only**.
- **Do not** add WebSocket/STOMP yet — barebones REST for messaging.
- **Do not** introduce new architectural dependencies (Kafka, Redis, ElasticSearch, etc.) without explicit user approval.
- **Do not** skip Flyway and use `ddl-auto=update`. Flyway is the source of truth, JPA validates only.

---

## 19. Reference: Existing State at Plan Time

- `backend/docker-compose.yml` — existing, will be replaced (currently uses DB name `ulms`, no MinIO, no MailHog).
- `backend/src/Reservity/pom.xml` — Spring Boot 4.0.3 starter scaffold, Java 25, has `webflux` + `webmvc` + `jdbc` + `thymeleaf` + `mail` + `security` + `flyway` + `validation` + `cache` + `actuator`. Needs the changes in Section 2.
- `backend/src/Reservity/src/main/java/dev/tmmc/ulms/UlmsApplication.java` — single empty `@SpringBootApplication` class. To be renamed/moved.
- `backend/src/Reservity/src/main/resources/application.properties` — only contains `spring.application.name=ulms`. To be replaced with `application.yml`.
- `backend/src/Reservity/ER Diagram.md` — Mermaid ER diagram (authoritative source for entity definitions).
- `backend/ReservityDiagram.drawio.xml` — older drawio diagram, **outdated** vs `ER Diagram.md`. Ignore in favor of the .md file.
- `backend/src/Reservity/target/` and various `compile_log*.txt` — stale build artifacts. Safe to delete.

---

## 20. Quick Reference for Future Sessions

When a new Claude session picks this up:
1. **Read this file in full first.**
2. Read `backend/src/Reservity/ER Diagram.md` for entity definitions.
3. Read `backend/src/Reservity/pom.xml` to see current dependency state.
4. Check `git log` to see what's already been implemented since this plan was written.
5. Identify the next milestone in Section 16 that hasn't been completed.
6. If the user hasn't given a specific instruction, ask which milestone to work on rather than assuming.
7. Confirm any Section 17 open questions with the user before they become blockers.
8. Respect Section 18 guardrails strictly.
