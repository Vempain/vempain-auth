---
name: release-and-consume
description: Release a change of the vempain-auth shared library and roll it into the consuming backends (version file, GitHub Packages, libs.versions.toml bumps, local Maven for unreleased versions). Use whenever api/ or core/ changes must reach a backend.
---

# Release vempain-auth and consume it

- Artifacts: `vempain-auth-api` and `vempain-auth-core` on GitHub Packages. CI (`.github/workflows/ci.yaml` ->
  `spring-boot-library.yaml` of `vempain-workflows`) derives the version from `VERSION` and existing tags on `main`; bump `VERSION` for a
  release.
- Consumers pin `vempain-auth` in `gradle/libs.versions.toml`: `vempain-admin-backend`, `vempain-file-backend` (the website backend does not
  use this library). Java and Spring Boot versions in `gradle/libs.versions.toml` must stay identical across all Java repositories.
- Flyway versions in `core/src/main/resources/db/migration/auth/` run on the consumers' admin datasources: never collide with their own
  migration numbers and never renumber a released migration.
- Before the release is published, verify a consumer against the unreleased build: `./gradlew -PreleaseVersion=<next> publishToMavenLocal`
  here, bump the consumer's catalog, and build it with a Gradle init script that puts `mavenLocal()` first (the consumers' build files list
  only GitHub Packages).
- A change to the REST contracts (`LoginAPI`, `UserAPI`, `UnitAPI`, `AclAPI`) or the DTOs is also a change for `vempain-auth-frontend`
  (models and management screens) and `vempain-cli` (login/session); update them in the same change set.
- Run `./gradlew clean test` here before publishing.
