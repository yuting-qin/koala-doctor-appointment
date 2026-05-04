package com.koala.koala_doctor_appointment.clinic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryClinicRepository implements ClinicRepository {

    private final Map<UUID, Clinic> store = new HashMap<>();

    public InMemoryClinicRepository(Collection<Clinic> initial) {
        initial.forEach(c -> store.put(c.id(), c));
    }

    @Override
    public Optional<Clinic> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Clinic> findAll() {
        return List.copyOf(store.values());
    }
}
