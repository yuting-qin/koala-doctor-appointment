package com.koala.koala_doctor_appointment.availability;

import com.koala.koala_doctor_appointment.appointment.InMemoryAppointmentRepository;
import com.koala.koala_doctor_appointment.clinic.Clinic;
import com.koala.koala_doctor_appointment.clinic.InMemoryClinicRepository;
import com.koala.koala_doctor_appointment.common.EntityNotFoundException;
import com.koala.koala_doctor_appointment.doctor.Doctor;
import com.koala.koala_doctor_appointment.doctor.InMemoryDoctorRepository;
import com.koala.koala_doctor_appointment.practice.InMemoryPracticeRepository;
import com.koala.koala_doctor_appointment.practice.Practice;
import com.koala.koala_doctor_appointment.practice.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvailabilityOrchestratorForDoctorTest {

    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Test
    void getForDoctor_groupsPracticesAcrossMultipleClinics() {
        UUID doctorId = UUID.randomUUID();
        UUID koalaClinicId = UUID.randomUUID();
        UUID bearClinicId = UUID.randomUUID();
        UUID koalaPracticeId = UUID.randomUUID();
        UUID bearPracticeId = UUID.randomUUID();

        Doctor doctor = new Doctor(doctorId, "Dr. Lee", "GP");
        Clinic koala = new Clinic(koalaClinicId, "Koala Clinic", SYDNEY);
        Clinic bear = new Clinic(bearClinicId, "Bear Clinic", NEW_YORK);
        Practice koalaPractice = new Practice(
                koalaPracticeId, doctorId, koalaClinicId,
                Map.of(DayOfWeek.MONDAY,
                        List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)))),
                30
        );
        Practice bearPractice = new Practice(
                bearPracticeId, doctorId, bearClinicId,
                Map.of(DayOfWeek.TUESDAY,
                        List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(10, 30)))),
                45
        );

        AvailabilityOrchestrator orchestrator = new AvailabilityOrchestrator(
                new InMemoryClinicRepository(List.of(koala, bear)),
                new InMemoryDoctorRepository(List.of(doctor)),
                new InMemoryPracticeRepository(List.of(koalaPractice, bearPractice)),
                new InMemoryAppointmentRepository(),
                new AvailabilityService()
        );

        // Window: Mon May 4 → Thu May 7 in Sydney. Wide enough that the conversion to
        // New York's local dates still includes Tuesday May 5 (NY) — needed because
        // AvailabilityService.compute iterates local dates strictly less than to-date,
        // so the to-instant must be beyond the end of Tuesday in NY.
        Instant from = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant to = ZonedDateTime.of(2026, 5, 7, 0, 0, 0, 0, SYDNEY).toInstant();

        DoctorAvailability result = orchestrator.getForDoctor(doctorId, from, to);

        assertThat(result.doctor()).isEqualTo(doctor);
        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.practices())
                .extracting(p -> p.practice().id())
                .containsExactlyInAnyOrder(koalaPracticeId, bearPracticeId);

        PracticeAvailability koalaResult = result.practices().stream()
                .filter(p -> p.practice().id().equals(koalaPracticeId))
                .findFirst().orElseThrow();
        assertThat(koalaResult.clinic()).isEqualTo(koala);
        assertThat(koalaResult.slots()).hasSize(6); // Mon 9–12 / 30min

        PracticeAvailability bearResult = result.practices().stream()
                .filter(p -> p.practice().id().equals(bearPracticeId))
                .findFirst().orElseThrow();
        assertThat(bearResult.clinic()).isEqualTo(bear);
        assertThat(bearResult.slots()).hasSize(2); // Tue 9–10:30 / 45min
    }

    @Test
    void getForDoctor_throwsWhenDoctorNotFound() {
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

        assertThatThrownBy(() -> orchestrator.getForDoctor(missing, from, to))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(missing.toString());
    }
}
