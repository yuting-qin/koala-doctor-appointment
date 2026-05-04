package com.koala.koala_doctor_appointment.appointment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAppointmentRepository implements AppointmentRepository {

    // Per-practice list keeps save's check-and-add scoped to one practice's lock,
    // and isolates iteration in find from concurrent saves to other practices.
    private final Map<UUID, List<Appointment>> byPractice = new ConcurrentHashMap<>();
    private final Map<UUID, Object> practiceLocks = new ConcurrentHashMap<>();

    @Override
    public Appointment save(Appointment appointment) {
        UUID practiceId = appointment.practiceId();
        Object lock = practiceLocks.computeIfAbsent(practiceId, id -> new Object());
        synchronized (lock) {
            List<Appointment> list = byPractice.computeIfAbsent(practiceId, id -> new ArrayList<>());
            boolean exists = list.stream()
                    .anyMatch(a -> a.startTime().equals(appointment.startTime()));
            if (exists) {
                throw new SlotAlreadyBookedException(practiceId, appointment.startTime());
            }
            list.add(appointment);
            return appointment;
        }
    }

    @Override
    public List<Appointment> findByPracticeIdAndRange(UUID practiceId, Instant from, Instant to) {
        Object lock = practiceLocks.computeIfAbsent(practiceId, id -> new Object());
        synchronized (lock) {
            List<Appointment> list = byPractice.getOrDefault(practiceId, List.of());
            return list.stream()
                    .filter(a -> !a.startTime().isBefore(from) && a.startTime().isBefore(to))
                    .toList();
        }
    }
}
