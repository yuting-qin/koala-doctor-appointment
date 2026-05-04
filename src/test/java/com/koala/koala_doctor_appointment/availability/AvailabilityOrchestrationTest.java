package com.koala.koala_doctor_appointment.availability;

import com.koala.koala_doctor_appointment.appointment.Appointment;
import com.koala.koala_doctor_appointment.appointment.InMemoryAppointmentRepository;
import com.koala.koala_doctor_appointment.clinic.Clinic;
import com.koala.koala_doctor_appointment.clinic.InMemoryClinicRepository;
import com.koala.koala_doctor_appointment.doctor.Doctor;
import com.koala.koala_doctor_appointment.doctor.InMemoryDoctorRepository;
import com.koala.koala_doctor_appointment.practice.InMemoryPracticeRepository;
import com.koala.koala_doctor_appointment.practice.Practice;
import com.koala.koala_doctor_appointment.practice.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityOrchestrationTest {

    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");

    @Test
    void getForClinic_returnsSlotsForEachPracticeInTheClinic() {
        UUID clinicId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID practiceId = UUID.randomUUID();

        Clinic clinic = new Clinic(clinicId, "Koala Clinic", SYDNEY);
        Doctor doctor = new Doctor(doctorId, "Dr. Lee", "GP");
        Practice practice = new Practice(
                practiceId,
                doctorId,
                clinicId,
                Map.of(DayOfWeek.MONDAY,
                        List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)))),
                30
        );

        AvailabilityOrchestrator orchestrator = new AvailabilityOrchestrator(
                new InMemoryClinicRepository(List.of(clinic)),
                new InMemoryDoctorRepository(List.of(doctor)),
                new InMemoryPracticeRepository(List.of(practice)),
                new InMemoryAppointmentRepository(),
                new AvailabilityService()
        );

        Instant from = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant to = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();

        ClinicAvailability result = orchestrator.getForClinic(clinicId, from, to);

        assertThat(result.clinic()).isEqualTo(clinic);
        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.practices()).hasSize(1);

        PracticeAvailability practiceResult = result.practices().getFirst();
        assertThat(practiceResult.practice()).isEqualTo(practice);
        assertThat(practiceResult.doctor()).isEqualTo(doctor);
        assertThat(practiceResult.slots()).hasSize(6);
        assertThat(practiceResult.slots().getFirst().startTime())
                .isEqualTo(ZonedDateTime.of(2026, 5, 4, 9, 0, 0, 0, SYDNEY).toInstant());
    }

    @Test
    void getForClinic_subtractsExistingBookings() {
        UUID clinicId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID practiceId = UUID.randomUUID();

        Clinic clinic = new Clinic(clinicId, "Koala Clinic", SYDNEY);
        Doctor doctor = new Doctor(doctorId, "Dr. Lee", "GP");
        Practice practice = new Practice(
                practiceId,
                doctorId,
                clinicId,
                Map.of(DayOfWeek.MONDAY,
                        List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)))),
                30
        );

        Instant tenAm = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();
        InMemoryAppointmentRepository appts = new InMemoryAppointmentRepository();
        appts.save(new Appointment(
                UUID.randomUUID(),
                practiceId,
                UUID.randomUUID(),
                tenAm,
                tenAm.plus(Duration.ofMinutes(30)),
                Instant.parse("2026-04-30T00:00:00Z")
        ));

        AvailabilityOrchestrator orchestrator = new AvailabilityOrchestrator(
                new InMemoryClinicRepository(List.of(clinic)),
                new InMemoryDoctorRepository(List.of(doctor)),
                new InMemoryPracticeRepository(List.of(practice)),
                appts,
                new AvailabilityService()
        );

        Instant from = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant to = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();

        ClinicAvailability result = orchestrator.getForClinic(clinicId, from, to);

        PracticeAvailability practiceResult = result.practices().getFirst();
        assertThat(practiceResult.slots()).hasSize(5);
        assertThat(practiceResult.slots()).extracting(Slot::startTime).doesNotContain(tenAm);
    }

    @Test
    void getForClinic_throwsWhenClinicNotFound() {
        AvailabilityOrchestrator orchestrator = new AvailabilityOrchestrator(
                new InMemoryClinicRepository(List.of()),
                new InMemoryDoctorRepository(List.of()),
                new InMemoryPracticeRepository(List.of()),
                new InMemoryAppointmentRepository(),
                new AvailabilityService()
        );
        UUID missing = UUID.randomUUID();
        Instant from = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant to = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                orchestrator.getForClinic(missing, from, to))
                .isInstanceOf(com.koala.koala_doctor_appointment.common.EntityNotFoundException.class)
                .hasMessageContaining(missing.toString());
    }
}
