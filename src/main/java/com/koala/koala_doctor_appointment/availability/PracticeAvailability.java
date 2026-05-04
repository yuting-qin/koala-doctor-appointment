package com.koala.koala_doctor_appointment.availability;

import com.koala.koala_doctor_appointment.clinic.Clinic;
import com.koala.koala_doctor_appointment.doctor.Doctor;
import com.koala.koala_doctor_appointment.practice.Practice;

import java.util.List;

public record PracticeAvailability(
        Practice practice,
        Doctor doctor,
        Clinic clinic,
        List<Slot> slots
) {
}
