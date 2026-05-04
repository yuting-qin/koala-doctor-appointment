package com.koala.koala_doctor_appointment.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository {

    /**
     * Atomically reserve a slot.
     *
     * <p>Throws {@link SlotAlreadyBookedException} if an appointment with the same
     * {@code (practiceId, startTime)} already exists. The check + insert is atomic.
     */
    Appointment save(Appointment appointment);

    List<Appointment> findByPracticeIdAndRange(UUID practiceId, Instant from, Instant to);
}
