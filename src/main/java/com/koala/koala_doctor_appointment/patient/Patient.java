package com.koala.koala_doctor_appointment.patient;

import java.util.UUID;

public record Patient(UUID id, String name, String email, String phone) {
}
