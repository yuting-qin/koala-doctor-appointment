package com.koala.koala_doctor_appointment.patient;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository {

    Optional<Patient> findById(UUID id);
}
