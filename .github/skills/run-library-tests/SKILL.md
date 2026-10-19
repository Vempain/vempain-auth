---
name: run-library-tests
description: Build and test the vempain-auth library modules (api, core, test-service) with Gradle and Testcontainers. Use before reporting any change as done.
---

# Run the vempain-auth tests

- Requirements: Java 25 and Docker (PostgreSQL Testcontainers `postgres:18-alpine`, Flyway; no H2).
- Commands: `./gradlew :api:test`, `./gradlew :core:test`, `./gradlew :test-service:test`, full run `./gradlew clean test` (what CI runs).
- Suffixes: `UTC` unit, `ITC` integration/container, `CTC` controller (MockMvc), `JTC` JSON/DTO contract; helpers (`TestApp`,
  `IntegrationTestSetup`, `Test*Config`, `Test*Tools`) carry no suffix.
- Keep `AclAuthorizationService` at 100% line coverage (JaCoCo report under `core/build/reports/jacoco`).
- Formatting: 4 spaces, 160 columns (`.editorconfig`, `.config/eclipse-formatter.xml`); do not reformat unrelated code. Report tests run and
  failures from the output.
