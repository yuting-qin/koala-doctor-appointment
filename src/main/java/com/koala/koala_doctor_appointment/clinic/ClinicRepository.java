package com.koala.koala_doctor_appointment.clinic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository {

    Optional<Clinic> findById(UUID id);

    List<Clinic> findAll();
}
