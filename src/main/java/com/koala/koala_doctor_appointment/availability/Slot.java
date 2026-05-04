package com.koala.koala_doctor_appointment.availability;

import java.time.Instant;

public record Slot(Instant startTime, Instant endTime) {
}
