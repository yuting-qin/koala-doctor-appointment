package com.koala.koala_doctor_appointment.availability;

import com.koala.koala_doctor_appointment.doctor.Doctor;

import java.time.Instant;
import java.util.List;

public record DoctorAvailability(
        Doctor doctor,
        Instant from,
        Instant to,
        List<PracticeAvailability> practices
) {
}
