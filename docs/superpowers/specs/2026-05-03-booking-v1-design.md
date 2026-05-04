# Doctor Appointment Booking — v1 Design

**Status:** Approved (pre-implementation)
**Date:** 2026-05-03
**Scope:** First working version of the Koala Doctor Appointment booking API.

---

## 1. Goal & Scope

Build a REST API that supports three end-user features:

1. View availability of all doctors in a clinic.
2. View availability of a selected doctor (across all clinics where they practice).
3. Book a timeslot.

The implementation runs against in-memory dummy data, but every architectural decision is made so that the API contract, domain model, and module seams remain valid when a real database is introduced later.

**Non-goals (v1):** authentication, cancellation, rescheduling, notifications, schedule exceptions (holidays/vacations), multiple appointment types, soft-holds, persistent storage, pagination, search.

The frontend will be built later in a separate repository — this repo is API-only.

---

## 2. Stack & Project Layout

### Stack

- Spring Boot 4.0.6, Java 25, Gradle Kotlin DSL (already in place).
- Add: `spring-boot-starter-web`, `spring-boot-starter-validation`.
- Test: `spring-boot-starter-test` (already in place).

### Package layout

Under `com.koala.koala_doctor_appointment`, organized by domain feature rather than by technical layer:

```
config/         — @Configuration beans (Clock, seed data toggles)
clinic/         — Clinic entity + repository
doctor/         — Doctor entity + repository
practice/       — Practice entity + repository (the doctor-at-clinic relationship)
patient/        — Patient entity + repository
appointment/    — Appointment entity, repository (the locking one), service
availability/   — AvailabilityService (pure slot computation)
api/            — Controllers, DTOs, error handlers
common/         — Shared exceptions, time utilities
```

Within each feature, the layering is controller → service → repository. Controllers take and return DTOs; services work in domain types; repositories own persistence. Domain entities and DTOs are separate types (no JPA-style entity-as-DTO).

This domain-grouped structure keeps each concern contained, which reads better as the project grows and aligns with how a future split into modules or services would naturally fall.

---

## 3. Domain Model

### Entities

```
Clinic
  id           : UUID
  name         : String
  timezone     : ZoneId            // e.g. "America/Los_Angeles"

Doctor
  id           : UUID
  name         : String
  specialty    : String
  // No clinic, working hours, or slot duration on the doctor — those live on Practice.

Practice
  id                    : UUID
  doctorId              : UUID     // FK → Doctor
  clinicId              : UUID     // FK → Clinic
  workingHours          : Map<DayOfWeek, List<TimeRange>>
                                   // TimeRange = (start: LocalTime, end: LocalTime)
                                   // multiple ranges per day allowed (lunch breaks)
  slotDurationMinutes   : int      // e.g. 30; per-practice (a doctor may have different
                                   // slot lengths at different clinics)

Patient
  id           : UUID
  name         : String
  email        : String
  phone        : String

Appointment
  id           : UUID
  practiceId   : UUID              // FK → Practice (captures both doctor and clinic)
  patientId    : UUID              // FK → Patient
  startTime    : Instant           // UTC
  endTime      : Instant           // UTC; computed from practice.slotDurationMinutes
  createdAt    : Instant           // UTC
```

All ids use `java.util.UUID`. v1 generates them via `UUID.randomUUID()` (v4). Jackson serializes UUIDs to/from the canonical 36-character form (`550e8400-e29b-41d4-a716-446655440000`); Spring's `@PathVariable UUID id` parses path segments without any custom converter.

### Why `Practice` exists

A doctor can practice at more than one clinic, with different schedules at each. The schedule (working hours and slot duration) is therefore a property of *the doctor's relationship with a clinic*, not of the doctor or the clinic alone.

In FHIR this entity is `PractitionerRole`. We use the shorter name `Practice` in code. The thing being booked is a practice, not a doctor — the same way Calendly bookings target an "event type" rather than a person.

### Why `Appointment.practiceId` (and not `doctorId + clinicId`)

A single foreign key carries both facts and prevents inconsistent states (e.g. an appointment that references a clinic the doctor doesn't practice at).

### Time handling

| Concern | Choice |
|---|---|
| Wire format (HTTP) | ISO 8601 UTC, `2026-05-15T16:00:00Z` |
| Stored timestamps | `Instant` (UTC, unambiguous) |
| Working hours | `LocalTime`, interpreted in the *clinic's* timezone |
| DST | Handled via `ZoneId` — local times remain stable across transitions |

The clinic owns the timezone (clinics operate in a place). When generating slots, working hours are projected from clinic-local time into UTC `Instant`s using `ZoneId.getRules()`. Frontends are expected to format times for display; the API returns UTC and includes the clinic's `timezone` in responses so the frontend can localize without a second lookup.

### `Clock` injection

A `Clock` bean (`Clock.systemUTC()`) is injected anywhere `Instant.now()` would otherwise be called. Tests substitute a fixed clock without static mocking.

---

## 4. API Contract

All times on the wire are ISO 8601 UTC. All ids are UUIDs in canonical 36-character form.

### Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/clinics/{clinicId}/availability?from=&to=` | Free slots for every practice in the clinic |
| `GET` | `/doctors/{doctorId}/availability?from=&to=` | Free slots for the doctor, grouped by practice |
| `POST` | `/appointments` | Book a slot |

No `/doctors` listing endpoint in v1 — the clinic-availability response includes each doctor's id and name, which is enough for the planned frontend flows.

### Request validation

Common to both availability endpoints:

- `from` and `to` are required ISO 8601 UTC instants.
- `from < to`.
- `to - from ≤ 14 days` (configurable via `app.availability.max-window-days`).
- `to` must be in the future (allowing `from = now` so a frontend can ask "from now").

For `POST /appointments`:

- All three fields (`practiceId`, `patientId`, `startTime`) are required and non-null.
- `startTime` must be in the future.
- `startTime` must align to the practice's slot grid (i.e. it must equal one of the candidate slot start times that the availability endpoint would return).
- Referenced ids (`practiceId`, `patientId`) must exist.

### Sample responses

`GET /clinics/c0a80101-0000-4000-8000-000000000001/availability?from=2026-05-15T00:00:00Z&to=2026-05-16T00:00:00Z`

```json
{
  "clinicId": "c0a80101-0000-4000-8000-000000000001",
  "clinicName": "Koala Clinic",
  "timezone": "America/Los_Angeles",
  "from": "2026-05-15T00:00:00Z",
  "to": "2026-05-16T00:00:00Z",
  "practices": [
    {
      "practiceId": "f1e2d3c4-0000-4000-8000-000000000001",
      "doctorId": "a1b2c3d4-0000-4000-8000-000000000001",
      "doctorName": "Dr. Lee",
      "specialty": "GP",
      "slotDurationMinutes": 30,
      "slots": [
        {"startTime": "2026-05-15T16:00:00Z", "endTime": "2026-05-15T16:30:00Z"},
        {"startTime": "2026-05-15T16:30:00Z", "endTime": "2026-05-15T17:00:00Z"}
      ]
    }
  ]
}
```

`GET /doctors/{doctorId}/availability?from=...&to=...`

```json
{
  "doctorId": "a1b2c3d4-0000-4000-8000-000000000001",
  "doctorName": "Dr. Lee",
  "specialty": "GP",
  "from": "2026-05-15T00:00:00Z",
  "to": "2026-05-22T00:00:00Z",
  "practices": [
    {
      "practiceId": "f1e2d3c4-0000-4000-8000-000000000001",
      "clinicId": "c0a80101-0000-4000-8000-000000000001",
      "clinicName": "Koala Clinic",
      "timezone": "America/Los_Angeles",
      "slotDurationMinutes": 30,
      "slots": [
        {"startTime": "2026-05-15T16:00:00Z", "endTime": "2026-05-15T16:30:00Z"}
      ]
    },
    {
      "practiceId": "f1e2d3c4-0000-4000-8000-000000000002",
      "clinicId": "c0a80101-0000-4000-8000-000000000002",
      "clinicName": "Bear Clinic",
      "timezone": "America/New_York",
      "slotDurationMinutes": 45,
      "slots": [
        {"startTime": "2026-05-16T18:00:00Z", "endTime": "2026-05-16T18:45:00Z"}
      ]
    }
  ]
}
```

`POST /appointments`

Request:
```json
{
  "practiceId": "f1e2d3c4-0000-4000-8000-000000000001",
  "patientId":  "b9a8c7d6-0000-4000-8000-000000000001",
  "startTime":  "2026-05-15T16:00:00Z"
}
```

Response (`201 Created`):
```json
{
  "id":         "e5f6a7b8-0000-4000-8000-000000000123",
  "practiceId": "f1e2d3c4-0000-4000-8000-000000000001",
  "patientId":  "b9a8c7d6-0000-4000-8000-000000000001",
  "startTime":  "2026-05-15T16:00:00Z",
  "endTime":    "2026-05-15T16:30:00Z",
  "createdAt":  "2026-05-03T22:14:07Z"
}
```

The client never sends `endTime` — it is computed server-side from `practice.slotDurationMinutes`.

### Error handling

All errors are returned as RFC 7807 `ProblemDetail` (Spring Boot's built-in support). A single `@RestControllerAdvice` translates domain exceptions to ProblemDetail bodies; controllers stay clean.

| HTTP status | Trigger |
|---|---|
| `400 Bad Request` | Malformed payload, validation failure (missing fields, bad date format, window too wide, `to ≤ from`) |
| `404 Not Found` | Unknown `clinicId` / `doctorId` / `practiceId` / `patientId` |
| `409 Conflict` | `SlotAlreadyBookedException` — the `(practiceId, startTime)` is already booked |
| `422 Unprocessable Entity` | `startTime` not aligned to the practice's slot grid, or in the past |

Sample error body:
```json
{
  "type": "https://api.koala.example/errors/slot-already-booked",
  "title": "Slot already booked",
  "status": 409,
  "detail": "The 09:00 slot on 2026-05-15 for practice f1e2d3c4-0000-4000-8000-000000000001 is no longer available.",
  "instance": "/appointments"
}
```

---

## 5. Implementation Seams

### Repository interfaces

Plain Java interfaces, with hand-rolled in-memory implementations registered as `@Repository` beans. No Spring Data JPA in v1.

```java
ClinicRepository
  Optional<Clinic> findById(UUID id);
  List<Clinic>     findAll();

DoctorRepository
  Optional<Doctor> findById(UUID id);

PracticeRepository
  Optional<Practice> findById(UUID id);
  List<Practice>     findByClinicId(UUID clinicId);
  List<Practice>     findByDoctorId(UUID doctorId);

PatientRepository
  Optional<Patient> findById(UUID id);

AppointmentRepository
  /**
   * Atomically reserve a slot.
   * Throws SlotAlreadyBookedException if (practiceId, startTime) already exists.
   */
  Appointment save(Appointment appointment) throws SlotAlreadyBookedException;
  List<Appointment> findByPracticeIdAndRange(UUID practiceId, Instant from, Instant to);
```

### Concurrency contract

`AppointmentRepository.save(...)` is the production-grade promise: the check + insert is atomic, and a duplicate `(practiceId, startTime)` always raises `SlotAlreadyBookedException`.

- **In-memory implementation (v1):** a `ConcurrentHashMap<UUID, Object>` holds a lock object per practice id. `save` synchronizes on the practice's lock object, checks for an existing appointment with the same `startTime`, and inserts atomically.
- **Future Postgres implementation:** a `UNIQUE (practice_id, start_time)` constraint, plus a try/catch that converts `DataIntegrityViolationException` into `SlotAlreadyBookedException`.

Code above the repository — services, controllers, error advice — is identical in both worlds. The in-memory locking is throwaway; the seam (interface + exception) is permanent.

This shape also documents the boundary at which production scaling decisions live (per-resource sharding, exclusion constraints, etc.) without pulling them into v1.

### `AvailabilityService`

A pure-function service with no I/O dependencies:

```java
List<Slot> compute(
    Practice practice,
    ZoneId clinicZone,
    Instant from,
    Instant to,
    List<Appointment> existingBookings
);
```

The caller (the controller's surrounding service) loads the practice, the clinic timezone, and the bookings, then calls `compute`. Pure functions make the gnarly cases — DST transitions, midnight boundaries, lunch-break splits, all-booked days — straightforward to unit-test.

### Configuration

`application.yml` (rename from `.properties`):

```yaml
app:
  availability:
    max-window-days: 14
  seed:
    enabled: true
```

### Seed data

A `SeedDataLoader` `@Component` implementing `ApplicationRunner` populates the in-memory repositories on startup when `app.seed.enabled=true`. The seed lives in Java, not YAML — the data shapes are already Java objects, and the loader serves as the spec for "what the dummy world looks like."

Recommended seed:

- 2 clinics: Koala Clinic (`America/Los_Angeles`), Bear Clinic (`America/New_York`).
- 3 doctors. One of them has practices at both clinics — exercises the multi-practice availability response.
- 4 practices total.
- 3 patients.
- 3–5 pre-existing appointments scattered across practices, so the "subtract bookings" path is exercised non-trivially.

---

## 6. Testing Strategy

| Layer | Test type | Coverage |
|---|---|---|
| `AvailabilityService` | Pure JUnit | Full day, partial overlap with bookings, DST forward/back, lunch-break splits, working-hours-empty days, requested window crossing midnight, requested window outside any working hours |
| In-memory `AppointmentRepository` | JUnit + threaded | (1) save succeeds; (2) duplicate `(practiceId, startTime)` throws `SlotAlreadyBookedException`; (3) **N=20 threads racing the same slot — exactly 1 succeeds, 19 throw conflict** |
| Controllers | `@WebMvcTest` + MockMvc | Route wiring, request validation, error advice maps to ProblemDetail JSON, response shapes match the contract above |
| End-to-end | `@SpringBootTest` + MockMvc | Full happy path: hit availability → POST appointment → re-hit availability and confirm the slot is gone |

The N-thread concurrency test is the highest-value single test in this suite — it proves the `save()` contract actually holds, and it would be painful to retrofit after a race condition is found in the wild. Implementation: a `CountDownLatch` releases all threads simultaneously at `repository.save()`.

---

## 7. Out of Scope (Explicit)

These are deferred to keep v1 small. Each is listed so we don't sleep-walk into adding it.

- Authentication and authorization.
- Cancellation, rescheduling, no-shows.
- Notifications (email, SMS, push). The booking service will publish a domain event on success in a later iteration; no listener exists in v1.
- Schedule exceptions: vacations, holidays, ad-hoc blocks.
- Multiple appointment types per practice (e.g. "new patient" vs "follow-up" with different durations).
- Persistent database. The interfaces are designed so the Postgres implementation is a drop-in.
- Soft-holds / reservation TTLs while a patient is on the confirmation screen.
- Pagination on availability responses. The capped 14-day window keeps results small.
- Search ("find earliest available across all GPs in this city").
- Rate limiting, observability (metrics/tracing), audit logging.

---

## 8. Future Evolution Hooks

Design choices in v1 that exist specifically so later iterations are additive rather than breaking:

- **Repository interfaces** isolate persistence — Postgres swap-in changes only the `@Repository` impls.
- **`AppointmentRepository.save` contract** — the atomic-uniqueness promise stays the same when storage changes.
- **`Practice` entity** — multi-clinic doctors work today; no future migration of `Appointment.doctorId → practiceId` needed.
- **`AvailabilityService` as a pure function** — caching, materialization, or replacement with a "next 30 days" read-model can sit in the calling service without changing the function's contract.
- **`Clock` bean** — time-sensitive features (reminders, expiring soft-holds) become testable from day one.
- **Domain event seam (planned, not yet wired)** — when notifications or audit are added, `AppointmentService.book()` will publish `AppointmentBooked` via Spring's `ApplicationEventPublisher`. Listeners are purely additive.
- **UUID ids from day one** — the API contract and persistence layer don't need a future id-type migration. If the project later wants time-ordered ids for DB locality, switching to UUIDv7 is internal to id generation; the wire format stays the same.
