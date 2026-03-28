# AGENTS.md

## What this repo is

- `vempain-auth` is a **shared Spring Boot library**, not a runnable service. The root project has two Gradle modules: `api` and `core` (`settings.gradle`).
- `api/` publishes request/response DTOs and REST interfaces; `core/` publishes the Spring components that implement them.
- Tests need an explicit bootstrapping app because there is no production `@SpringBootApplication`; use `core/src/test/java/fi/poltsi/vempain/auth/TestApp.java`
  and `IntegrationTestSetup.java` as the reference pattern.

## Module boundaries

- `api/src/main/java/fi/poltsi/vempain/auth/api/**`: enums, DTOs, base response types.
- `api/src/main/java/fi/poltsi/vempain/auth/rest/LoginAPI.java`: HTTP contract lives in `api`; implementations should implement these interfaces from `core`.
- `core/src/main/java/fi/poltsi/vempain/auth/controller/LoginController.java`: current concrete controller implementation.
- `core/src/main/java/fi/poltsi/vempain/auth/service/**`: business logic (`UserService`, `AclService`, `UserDetailsServiceImpl`, etc.).
- `core/src/main/java/fi/poltsi/vempain/auth/security/**`: Spring Security / JWT wiring.
- `core/src/main/java/fi/poltsi/vempain/auth/entity/**` + `repository/**`: JPA model and repositories.

## Main runtime flow

- Login flow is: `LoginAPI` -> `LoginController.authenticateUser()` -> `AuthenticationManager`/`UserDetailsServiceImpl` -> `JwtUtils` -> `LoginResponse`.
- `UserDetailsImpl` wraps `UserAccount` and exposes unit memberships as Spring Security authorities; `LoginController` also serializes units into
  `UnitResponse`.
- ACLs are central to the data model: `AbstractVempainEntity` requires `aclId`, `creator`, `created`, and optional modifier fields; `AclService` validates these
  heavily before save/update.
- Password policy lives in `core/.../tools/AuthTools.java` (`passwordCheck` + bcrypt strength 12). Tests creating users should hash passwords with
  `AuthTools.passwordHash(...)` or the configured `PasswordEncoder`.

## Database / Flyway

- Auth schema migrations live in `core/src/main/resources/db/migration/auth/` (currently `V1__init.sql`).
- The migration creates `user_account`, `acl`, `unit`, `user_unit`, plus the ACL sequence(s); ACL allocation logic depends on that schema existing.
- Because this library is consumed on another app’s classpath, migration versions must not collide with the consuming service’s Flyway versions.

## Testing workflow

- Full test run in CI is `./gradlew clean test` (`.github/workflows/ci.yaml`).
- Useful local commands:
    - `./gradlew :api:test`
    - `./gradlew :core:test`
    - `./gradlew clean test`
- Integration tests use PostgreSQL Testcontainers (`postgres:18-alpine`) and Flyway, not H2. See `IntegrationTestSetup.java`, `AclServiceConcurrencyITC.java`,
  and `LoginRTC.java`.
- `IntegrationTestSetup` keeps the seeded admin user (`Constants.ADMIN_ID == 1L`) and resets other rows before each test; do not write tests that blindly delete
  all users.
- Test suffixes are meaningful: `UTC` = unit-style tests, `ITC` = integration/container tests, `RTC` = controller/request tests, `JTC` = JSON/DTO contract
  tests.

## Conventions specific to this repo

- Formatting is tab-indented for Java (`.editorconfig`); avoid reformatting unrelated code.
- DTOs commonly use Lombok builders and snake_case JSON mappings; see `UserResponse`, `UnitResponse`, and `ResponseDeserializationUTC.java` for the
  serialization contract.
- REST endpoints are defined via constants in `api/.../Constants.java`; e.g. login uses `Constants.LOGIN_PATH` (`/login`).
- Security config expects host applications/tests to provide properties such as `vempain.cors.allowed-origins`, `vempain.cors.max-age`,
  `vempain.cors.cors-pattern`, `vempain.app.jwt-secret`, and `vempain.app.jwt-expiration-ms`.

## Publishing / versions

- Java toolchain is 25 and Spring Boot version is controlled via `gradle.properties`.
- Artifacts publish to GitHub Packages as `vempain-auth-api` and `vempain-auth-core`; CI derives the release version from `VERSION` and existing Git tags.
- Manual Postgres setup for local debugging exists in `docker_db.sh`, but automated tests prefer Testcontainers.

