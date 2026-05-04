package com.koala.koala_doctor_appointment.patient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryPatientRepository implements PatientRepository {

    private final Map<UUID, Patient> store = new HashMap<>();

    public InMemoryPatientRepository(Collection<Patient> initial) {
        initial.forEach(p -> store.put(p.id(), p));
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }
}
