package com.koala.koala_doctor_appointment.availability;

import com.koala.koala_doctor_appointment.appointment.Appointment;
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

class AvailabilityServiceTest {

    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");

    private final AvailabilityService service = new AvailabilityService();

    @Test
    void computesSlotsForFullWorkingMorningWithNoBookings() {
        Practice practice = new Practice(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Map.of(DayOfWeek.MONDAY,
                        List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)))),
                30
        );

        Instant from = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant to = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();

        List<Slot> slots = service.compute(practice, SYDNEY, from, to, List.of());

        assertThat(slots).hasSize(6);
        assertThat(slots.getFirst().startTime())
                .isEqualTo(ZonedDateTime.of(2026, 5, 4, 9, 0, 0, 0, SYDNEY).toInstant());
        assertThat(slots.getFirst().endTime())
                .isEqualTo(ZonedDateTime.of(2026, 5, 4, 9, 30, 0, 0, SYDNEY).toInstant());
        assertThat(slots.getLast().startTime())
                .isEqualTo(ZonedDateTime.of(2026, 5, 4, 11, 30, 0, 0, SYDNEY).toInstant());
        assertThat(slots.getLast().endTime())
                .isEqualTo(ZonedDateTime.of(2026, 5, 4, 12, 0, 0, 0, SYDNEY).toInstant());
    }

    @Test
    void excludesSlotsThatAlreadyHaveAnAppointment() {
        UUID practiceId = UUID.randomUUID();
        Practice practice = new Practice(
                practiceId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Map.of(DayOfWeek.MONDAY,
                        List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)))),
                30
        );

        Instant tenAm = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();
        Appointment existing = new Appointment(
                UUID.randomUUID(),
                practiceId,
                UUID.randomUUID(),
                tenAm,
                tenAm.plus(Duration.ofMinutes(30)),
                Instant.parse("2026-04-30T00:00:00Z")
        );

        Instant from = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant to = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();

        List<Slot> slots = service.compute(practice, SYDNEY, from, to, List.of(existing));

        assertThat(slots).hasSize(5);
        assertThat(slots).extracting(Slot::startTime).doesNotContain(tenAm);
    }
}
