# AGENTS.md

## What this repo is

- `vempain-auth` is a **shared Spring Boot library**, not a runnable service. The root project has three Gradle modules: `api`, `core` and `test-service`
  (`settings.gradle`); `test-service` is a minimal consumer application (`TestServiceApplication`, `TestServiceSecurityConfig`, `TestServiceController`) used to
  verify the security wiring and ACL authorization on a real classpath over MockMvc.
- `api/` publishes request/response DTOs and REST interfaces; `core/` publishes the Spring components that implement them.
- Tests need an explicit bootstrapping app because there is no production `@SpringBootApplication`; use `core/src/test/java/fi/poltsi/vempain/auth/TestApp.java`
  and `IntegrationTestSetup.java` as the reference pattern.

## Module boundaries

- `api/src/main/java/fi/poltsi/vempain/auth/api/**`: enums, DTOs, base response types.
- `api/src/main/java/fi/poltsi/vempain/auth/rest/{LoginAPI,UserAPI,UnitAPI,AclAPI}.java`: HTTP contracts live in `api`; `core` implements them.
- `core/src/main/java/fi/poltsi/vempain/auth/controller/{Login,User,Unit,Acl}Controller.java`: the concrete controllers. Every service that
  includes `vempain-auth-core` hosts them through component scanning, so user, unit and ACL management (`/content-management/users|units|acls`) is implemented
  once here and never in a consuming backend. `AdministrationGuard`
  (`security/`) gates the management endpoints with the modify privilege on the administrator ACL (`Constants.ADMIN_ID`), never a role.
- `core/src/main/java/fi/poltsi/vempain/auth/service/**`: business logic (`UserService`, `AclService`, `UserDetailsServiceImpl`, etc.).
- `core/src/main/java/fi/poltsi/vempain/auth/security/**`: Spring Security / JWT wiring.
- `core/src/main/java/fi/poltsi/vempain/auth/entity/**` + `repository/**`: JPA model and repositories.

## Main runtime flow

- Login flow is: `LoginAPI` -> `LoginController.authenticateUser()` -> `AuthenticationManager`/`UserDetailsServiceImpl` -> `JwtUtils` -> `LoginResponse`.
- `UserDetailsImpl` wraps `UserAccount` and exposes unit memberships as Spring Security authorities; `LoginController` also serializes units into
  `UnitResponse`.
- ACLs are central to the data model: `AbstractVempainEntity` requires `aclId`, `creator`, `created`, and optional modifier fields; `AclService` validates these
  heavily before save/update.
- Authorization is resource-based, not role-based. Every protected resource carries an `acl_id`; `AclAuthorizationService` grants an operation only when
  the authenticated user or one of their units has a matching ACL row with the requested read/create/modify/delete privilege. It fails closed: a
  non-positive `acl_id`, missing ACL rows, an anonymous or non-Vempain principal all yield `false`. Applications should keep endpoint authentication
  in their local `WebSecurityConfig` and apply ACL checks at the resource service/controller boundary with `@PreAuthorize` or an equivalent explicit
  service call. Do not add `hasRole`/`ROLE_*` authorization rules or any test-mode bypass.
- `AclAuthorizationService` must stay at 100% line coverage inside this repository, so regressions are caught before a release is consumed by the
  backends: `AclAuthorizationServiceUTC` (every branch with mocks), `AclAuthorizationServiceITC` (real ACL rows in PostgreSQL) and the
  `test-service` module's `TestServiceSecurityITC` (the `@PreAuthorize("@aclAuthorizationService.canRead(#p0)")` pattern over HTTP) all exercise
  granted and denied cases. Extend them whenever the evaluation rules change.
- Units nest: `unit_unit` (`V2__unit_members.sql`, entity `UnitUnit`, `UnitUnitRepository`) stores parent/child edges and `user_unit` the
  direct user members. `UnitMembershipService` computes descendant/ancestor closures and `wouldCreateCycle`, the backstop that
  `UnitService.updateMembers` applies before storing `unit_ids` (400 for self-membership, an unknown member or a chain leading back to
  the unit); `user_ids` are applied through the owning `UserAccount.units` side. `UnitRequest/UnitResponse.user_ids|unit_ids` and
  `UserRequest/UserResponse.unit_ids` carry memberships (a null list in a request leaves that membership untouched). Membership is
  transitive for authorization: `UserDetailsServiceImpl.loadUserByUsername` builds the principal with `effectiveUnits` (direct units plus
  every unit containing them), so ACL rows granted to an outer unit apply to the members of its sub-units. Tests:
  `UnitMembershipServiceUTC`, `UnitMembershipITC`, `UserUnitManagementCTC`, `UserDetailsServiceImplUTC`, `UnitMembershipContractJTC`.
- `UserService.findPaged`/`UnitService.findPaged` page the (small) user and unit lists in memory (`PagingTools`): name or id sort, search
  by name/login name; the controllers only guard and delegate.
- Password policy lives in `core/.../tools/AuthTools.java` (`passwordCheck` + bcrypt strength 12). Tests creating users should hash passwords with
  `AuthTools.passwordHash(...)` or the configured `PasswordEncoder`.

## Database / Flyway

- Auth schema migrations live in `core/src/main/resources/db/migration/auth/` (`V1__init.sql`, `V2__unit_members.sql`).
- The migrations create `user_account`, `acl`, `unit`, `user_unit`, `unit_unit`, plus the ACL sequence (s); ACL allocation logic depends on that schema
  existing.
- Because this library is consumed on another app’s classpath, migration versions must not collide with the consuming service’s Flyway versions.

## Testing workflow

- Full test run in CI is `./gradlew clean test` (`.github/workflows/ci.yaml`).
- Useful local commands:
    - `./gradlew :api:test`
    - `./gradlew :core:test`
    - `./gradlew clean test`
- Integration tests use PostgreSQL Testcontainers (`postgres:18-alpine`) and Flyway, not H2. See `IntegrationTestSetup.java`, `AclServiceConcurrencyITC.java`,
  and `LoginCTC.java`.
- `IntegrationTestSetup` keeps the seeded admin user (`Constants.ADMIN_ID == 1L`) and resets other rows before each test; do not write tests that blindly delete
  all users.
- Test suffixes are meaningful and shared across the Vempain Java repos: `UTC` = unit-style tests, `ITC` = integration/container tests, `CTC` =
  controller tests (MockMvc, e.g. `LoginCTC`), `JTC` = JSON/DTO contract tests (`PagedRequestJTC`, `PagedResponseJTC`, `AclRequestJTC`). Helper
  classes (`TestApp`, `IntegrationTestSetup`, `Test*Config`, `Test*Tools`) carry no suffix.
- After every code modification, run the relevant module tests and report the results in the response.

## Conventions specific to this repo

- Formatting is tab-indented for Java (`.editorconfig`); avoid reformatting unrelated code.
- Prefer Lombok annotations for applicable Java boilerplate such as constructors, accessors, builders, and logging, unless they obscure behavior or conflict
  with framework requirements.
- DTOs commonly use Lombok builders and snake_case JSON mappings; see `UserResponse`, `UnitResponse`, and `ResponseDeserializationUTC.java` for the
  serialization contract.
- JSON contract is mandatory snake_case only (`total_pages`, `total_elements`, etc.); never introduce camelCase JSON field names in request/response DTOs,
  samples, or tests.
- For Jackson usage, prefer v3 `tools.jackson.databind.*` naming/mapper APIs and keep non-`tools.jackson` annotations only when no `tools.jackson`
  replacement is available in the active dependency set.
- REST endpoints are defined via constants in `api/.../Constants.java`; e.g. login uses `Constants.LOGIN_PATH` (`/login`).
- Security config expects host applications/tests to provide properties such as `vempain.cors.allowed-origins`, `vempain.cors.max-age`,
  `vempain.cors.cors-pattern`, `vempain.app.jwt-secret`, and `vempain.app.jwt-expiration-ms`.

## Publishing / versions

- Java toolchain and Spring Boot versions are pinned in `gradle/libs.versions.toml` (`java`, `spring-boot`); keep them aligned with the consuming backends.
- Artifacts publish to GitHub Packages as `vempain-auth-api` and `vempain-auth-core`; CI derives the release version from `VERSION` and existing Git tags.
- Manual Postgres setup for local debugging exists in `docker_db.sh`, but automated tests prefer Testcontainers.
- Dependabot (`.github/dependabot.yaml`) covers GitHub Actions and Gradle from Maven Central; this library consumes no GitHub Packages
  artifacts, so it declares no private registry (the consuming backends do).

## Tag ACL rule

Tags are metadata, not ACL-bearing resources. Tag entities have no ACL information, so tag list, search, and mutation endpoints must not perform ACL checks on
tags. ACL checks apply only to resources that explicitly carry an ACL.
