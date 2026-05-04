# Koala Doctor Appointment

A REST API for an online doctor appointment booking system.

## Overview

The service exposes endpoints for browsing doctor and clinic availability and for booking appointments. Domain concepts:

- **Practice** — a medical practice that operates one or more clinics.
- **Clinic** — a physical location with operating hours where doctors hold sessions.
- **Doctor** — a practitioner with their own working hours, attached to a practice.
- **Patient** — the booker of an appointment.
- **Slot / Availability** — bookable time ranges derived from clinic and doctor hours, minus existing appointments.
- **Appointment** — a confirmed booking against a slot.

Current endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/clinics/{clinicId}/availability?from=&to=` | Available slots at a clinic in a time window |
| `GET` | `/doctors/{doctorId}/availability?from=&to=` | Available slots for a doctor in a time window |
| `POST` | `/appointments` | Book an appointment |

Errors are returned as [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) `ProblemDetail` responses.

Data is held in in-memory repositories and pre-populated with seed data on startup, so the API is usable without any external dependencies.

## Tech

- Java 25 (Gradle toolchain — installed automatically if missing)
- Spring Boot 4.0.6
- Gradle Kotlin DSL
- springdoc-openapi for runtime API docs and build-time OpenAPI spec generation

## Running the application

From the project root:

```
./gradlew bootRun
```

The service starts on `http://localhost:8080`.

Quick check:

```
curl http://localhost:8080/clinics/<clinicId>/availability?from=2026-05-05T00:00:00Z&to=2026-05-06T00:00:00Z
```

Live API docs while the app is running:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

## Running tests

```
./gradlew test
```

## OpenAPI spec

The committed OpenAPI spec lives at [`docs/openapi/openapi.yaml`](docs/openapi/openapi.yaml) so API changes show up in PR diffs.

To regenerate it after changing controllers or DTOs (stop `bootRun` first — the task forks its own Spring Boot process on port 8080):

```
./gradlew clean generateOpenApiDocs
```

Commit the updated spec along with the source changes.

A pre-push git hook (in [`.githooks/pre-push`](.githooks/pre-push)) regenerates the spec automatically and blocks the push if it drifts from the committed file, so you don't have to remember. The hook is shared via the repo and self-installs the first time you run `./gradlew build` (it points `core.hooksPath` at `.githooks`). To install manually without a build:

```
./gradlew installGitHooks
```

CI runs the same drift check, so a forgotten regeneration is caught either at push time or in CI.
