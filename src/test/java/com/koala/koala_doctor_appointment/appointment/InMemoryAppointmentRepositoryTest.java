package com.koala.koala_doctor_appointment.appointment;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAppointmentRepositoryTest {

    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");

    private final InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();

    @Test
    void savesAppointmentAndFindsItByPracticeAndRange() {
        UUID practiceId = UUID.randomUUID();
        Instant start = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();
        Appointment appt = appointmentAt(practiceId, start);

        Appointment saved = repo.save(appt);

        assertThat(saved).isEqualTo(appt);
        List<Appointment> found = repo.findByPracticeIdAndRange(
                practiceId,
                ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant(),
                ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant()
        );
        assertThat(found).containsExactly(appt);
    }

    @Test
    void rejectsDuplicatePracticeAndStartTime() {
        UUID practiceId = UUID.randomUUID();
        Instant start = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();
        repo.save(appointmentAt(practiceId, start));

        assertThatThrownBy(() -> repo.save(appointmentAt(practiceId, start)))
                .isInstanceOf(SlotAlreadyBookedException.class)
                .hasMessageContaining(practiceId.toString());
    }

    @Test
    void allowsSameStartTimeAcrossDifferentPractices() {
        Instant start = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();
        UUID practiceA = UUID.randomUUID();
        UUID practiceB = UUID.randomUUID();

        repo.save(appointmentAt(practiceA, start));
        Appointment second = appointmentAt(practiceB, start);

        assertThat(repo.save(second)).isEqualTo(second);
    }

    @Test
    void atomicallyResolvesConcurrentSavesForSameSlot() throws Exception {
        int n = 20;
        int iterations = 30;
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (int iter = 0; iter < iterations; iter++) {
            UUID practiceId = UUID.randomUUID();
            Instant start = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();
            CyclicBarrier barrier = new CyclicBarrier(n);
            ExecutorService pool = Executors.newFixedThreadPool(n);
            try {
                for (int i = 0; i < n; i++) {
                    pool.submit(() -> {
                        try {
                            barrier.await();
                            repo.save(appointmentAt(practiceId, start));
                            successes.incrementAndGet();
                        } catch (SlotAlreadyBookedException e) {
                            conflicts.incrementAndGet();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            Thread.currentThread().interrupt();
                        } catch (Throwable t) {
                            other.incrementAndGet();
                        }
                    });
                }
                pool.shutdown();
                assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                pool.shutdownNow();
            }
        }

        assertThat(successes.get()).isEqualTo(iterations);
        assertThat(conflicts.get()).isEqualTo(iterations * (n - 1));
        assertThat(other.get()).isZero();
    }

    private static Appointment appointmentAt(UUID practiceId, Instant start) {
        return new Appointment(
                UUID.randomUUID(),
                practiceId,
                UUID.randomUUID(),
                start,
                start.plus(Duration.ofMinutes(30)),
                Instant.parse("2026-04-30T00:00:00Z")
        );
    }
}
