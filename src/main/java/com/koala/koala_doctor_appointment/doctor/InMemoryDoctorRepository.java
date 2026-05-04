package com.koala.koala_doctor_appointment.doctor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryDoctorRepository implements DoctorRepository {

    private final Map<UUID, Doctor> store = new HashMap<>();

    public InMemoryDoctorRepository(Collection<Doctor> initial) {
        initial.forEach(d -> store.put(d.id(), d));
    }

    @Override
    public Optional<Doctor> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Doctor> findAll() {
        return List.copyOf(store.values());
    }
}
