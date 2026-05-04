package com.koala.koala_doctor_appointment.api;

import java.time.Instant;

public record SlotResponse(Instant startTime, Instant endTime) {
}
