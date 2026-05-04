package com.koala.koala_doctor_appointment.appointment;

import com.koala.koala_doctor_appointment.availability.AvailabilityService;
import com.koala.koala_doctor_appointment.clinic.Clinic;
import com.koala.koala_doctor_appointment.clinic.InMemoryClinicRepository;
import com.koala.koala_doctor_appointment.common.EntityNotFoundException;
import com.koala.koala_doctor_appointment.common.InvalidSlotException;
import com.koala.koala_doctor_appointment.patient.InMemoryPatientRepository;
import com.koala.koala_doctor_appointment.patient.Patient;
import com.koala.koala_doctor_appointment.practice.InMemoryPracticeRepository;
import com.koala.koala_doctor_appointment.practice.Practice;
import com.koala.koala_doctor_appointment.practice.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentServiceTest {

    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");

    @Test
    void booksAppointmentAndPersistsIt() {
        Fixture f = mondayMorningPractice();

        Instant start = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();
        Appointment booked = f.service.book(new BookRequest(f.practiceId, f.patientId, start));

        assertThat(booked.id()).isNotNull();
        assertThat(booked.practiceId()).isEqualTo(f.practiceId);
        assertThat(booked.patientId()).isEqualTo(f.patientId);
        assertThat(booked.startTime()).isEqualTo(start);
        assertThat(booked.endTime()).isEqualTo(start.plus(Duration.ofMinutes(30)));
        assertThat(booked.createdAt()).isNotNull();

        Instant dayStart = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant nextDay = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();
        assertThat(f.appointments.findByPracticeIdAndRange(f.practiceId, dayStart, nextDay))
                .containsExactly(booked);
    }

    @Test
    void rejectsBookingWhenStartTimeIsInThePast() {
        Instant slot = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();
        Clock afterSlot = Clock.fixed(slot.plus(Duration.ofMinutes(1)), ZoneOffset.UTC);
        Fixture f = mondayMorningPractice(afterSlot);

        assertThatThrownBy(() ->
                f.service.book(new BookRequest(f.practiceId, f.patientId, slot)))
                .isInstanceOf(InvalidSlotException.class)
                .hasMessageContaining("past");

        Instant dayStart = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant nextDay = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();
        assertThat(f.appointments.findByPracticeIdAndRange(f.practiceId, dayStart, nextDay)).isEmpty();
    }

    @Test
    void rejectsBookingWhenStartTimeNotOnSlotGrid() {
        Fixture f = mondayMorningPractice();
        Instant offGrid = ZonedDateTime.of(2026, 5, 4, 9, 7, 0, 0, SYDNEY).toInstant();

        assertThatThrownBy(() ->
                f.service.book(new BookRequest(f.practiceId, f.patientId, offGrid)))
                .isInstanceOf(InvalidSlotException.class)
                .hasMessageContaining("not aligned");
    }

    @Test
    void rejectsBookingWhenStartTimeOutsideWorkingHours() {
        Fixture f = mondayMorningPractice();
        // 13:00 Monday — on grid but after the 12:00 close
        Instant afterHours = ZonedDateTime.of(2026, 5, 4, 13, 0, 0, 0, SYDNEY).toInstant();

        assertThatThrownBy(() ->
                f.service.book(new BookRequest(f.practiceId, f.patientId, afterHours)))
                .isInstanceOf(InvalidSlotException.class)
                .hasMessageContaining("working hours");
    }

    @Test
    void propagatesSlotAlreadyBookedFromRepository() {
        Fixture f = mondayMorningPractice();
        Instant slot = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();
        f.service.book(new BookRequest(f.practiceId, f.patientId, slot));

        assertThatThrownBy(() ->
                f.service.book(new BookRequest(f.practiceId, f.patientId, slot)))
                .isInstanceOf(SlotAlreadyBookedException.class);
    }

    @Test
    void rejectsBookingWhenPracticeNotFound() {
        InMemoryPracticeRepository practices = new InMemoryPracticeRepository(List.of());
        InMemoryClinicRepository clinics = new InMemoryClinicRepository(List.of());
        InMemoryPatientRepository patients = new InMemoryPatientRepository(List.of());
        InMemoryAppointmentRepository appointments = new InMemoryAppointmentRepository();
        AppointmentService service = new AppointmentService(
                practices, clinics, patients, appointments,
                new AvailabilityService(), fixedClockBefore());

        UUID missing = UUID.randomUUID();
        Instant start = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();

        assertThatThrownBy(() ->
                service.book(new BookRequest(missing, UUID.randomUUID(), start)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(missing.toString());
    }

    @Test
    void rejectsBookingWhenPatientNotFound() {
        Fixture f = mondayMorningPractice();
        UUID missingPatient = UUID.randomUUID();
        Instant slot = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();

        assertThatThrownBy(() ->
                f.service.book(new BookRequest(f.practiceId, missingPatient, slot)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(missingPatient.toString());

        Instant dayStart = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant nextDay = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();
        assertThat(f.appointments.findByPracticeIdAndRange(f.practiceId, dayStart, nextDay)).isEmpty();
    }

    private record Fixture(UUID practiceId,
                           UUID patientId,
                           InMemoryAppointmentRepository appointments,
                           AppointmentService service) {
    }

    private static Fixture mondayMorningPractice() {
        return mondayMorningPractice(fixedClockBefore());
    }

    private static Fixture mondayMorningPractice(Clock clock) {
        UUID clinicId = UUID.randomUUID();
        UUID practiceId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Clinic clinic = new Clinic(clinicId, "Test Clinic", SYDNEY);
        Practice practice = new Practice(
                practiceId,
                UUID.randomUUID(),
                clinicId,
                Map.of(DayOfWeek.MONDAY,
                        List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)))),
                30
        );
        Patient patient = new Patient(patientId, "Test Patient", "test@example.com", "+61400000000");
        InMemoryPracticeRepository practices = new InMemoryPracticeRepository(List.of(practice));
        InMemoryClinicRepository clinics = new InMemoryClinicRepository(List.of(clinic));
        InMemoryPatientRepository patients = new InMemoryPatientRepository(List.of(patient));
        InMemoryAppointmentRepository appointments = new InMemoryAppointmentRepository();
        AppointmentService service = new AppointmentService(
                practices, clinics, patients, appointments, new AvailabilityService(), clock);
        return new Fixture(practiceId, patientId, appointments, service);
    }

    private static Clock fixedClockBefore() {
        return Clock.fixed(Instant.parse("2026-04-30T00:00:00Z"), ZoneOffset.UTC);
    }
}
