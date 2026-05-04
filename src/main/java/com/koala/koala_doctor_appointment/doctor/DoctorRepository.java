package com.koala.koala_doctor_appointment.doctor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository {

    Optional<Doctor> findById(UUID id);

    List<Doctor> findAll();
}
