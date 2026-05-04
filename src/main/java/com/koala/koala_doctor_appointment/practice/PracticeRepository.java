package com.koala.koala_doctor_appointment.practice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PracticeRepository {

    Optional<Practice> findById(UUID id);

    List<Practice> findByClinicId(UUID clinicId);

    List<Practice> findByDoctorId(UUID doctorId);
}
