# Vempain Auth component

This component is part of the [Vempain](https://vempain.poltsi.fi/) project. It provides authentication and authorization services for the Vempain backends
written in Java and Spring Boot.

[AGENTS.md](docs/AGENTS.md) has more detailed orientation and workflow guidance for agents working in this codebase.

## Modules

- `api` publishes the shared API contracts.
- `core` publishes the reusable Spring Security and authentication implementation.
- `test-service` is an unpublished Spring Boot host used to verify how a
  consuming service extends `WebSecurityConfig`. It is built only as part of
  local/CI tests and is not configured for Maven publication.

The test-service target is an aggregate guard rail: it runs the complete
published `api` and `core` test suites before the consumer-application tests.
Run the complete unpublished-version validation with:

```bash
./gradlew :test-service:test
```
