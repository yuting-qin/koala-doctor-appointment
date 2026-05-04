package com.koala.koala_doctor_appointment.practice;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryPracticeRepository implements PracticeRepository {

    private final Map<UUID, Practice> store = new HashMap<>();

    public InMemoryPracticeRepository(Collection<Practice> initial) {
        initial.forEach(p -> store.put(p.id(), p));
    }

    @Override
    public Optional<Practice> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Practice> findByClinicId(UUID clinicId) {
        return store.values().stream()
                .filter(p -> p.clinicId().equals(clinicId))
                .toList();
    }

    @Override
    public List<Practice> findByDoctorId(UUID doctorId) {
        return store.values().stream()
                .filter(p -> p.doctorId().equals(doctorId))
                .toList();
    }
}
