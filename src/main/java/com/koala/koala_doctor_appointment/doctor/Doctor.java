package com.koala.koala_doctor_appointment.doctor;

import java.util.UUID;

public record Doctor(UUID id, String name, String specialty) {
}
