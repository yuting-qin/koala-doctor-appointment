package com.koala.koala_doctor_appointment.appointment;

import java.time.Instant;
import java.util.UUID;

public class SlotAlreadyBookedException extends RuntimeException {

    private final UUID practiceId;
    private final Instant startTime;

    public SlotAlreadyBookedException(UUID practiceId, Instant startTime) {
        super("Slot at " + startTime + " is already booked for practice " + practiceId);
        this.practiceId = practiceId;
        this.startTime = startTime;
    }

    public UUID practiceId() {
        return practiceId;
    }

    public Instant startTime() {
        return startTime;
    }
}
