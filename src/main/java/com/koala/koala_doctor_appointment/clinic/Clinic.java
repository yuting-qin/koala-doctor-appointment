package com.koala.koala_doctor_appointment.clinic;

import java.time.ZoneId;
import java.util.UUID;

public record Clinic(UUID id, String name, ZoneId timezone) {
}
