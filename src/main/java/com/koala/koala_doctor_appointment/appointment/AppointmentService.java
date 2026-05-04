package com.koala.koala_doctor_appointment.appointment;

import com.koala.koala_doctor_appointment.availability.AvailabilityService;
import com.koala.koala_doctor_appointment.availability.Slot;
import com.koala.koala_doctor_appointment.clinic.Clinic;
import com.koala.koala_doctor_appointment.clinic.ClinicRepository;
import com.koala.koala_doctor_appointment.common.EntityNotFoundException;
import com.koala.koala_doctor_appointment.common.InvalidSlotException;
import com.koala.koala_doctor_appointment.patient.PatientRepository;
import com.koala.koala_doctor_appointment.practice.Practice;
import com.koala.koala_doctor_appointment.practice.PracticeRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class AppointmentService {

    private final PracticeRepository practices;
    private final ClinicRepository clinics;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final AvailabilityService availability;
    private final Clock clock;

    public AppointmentService(PracticeRepository practices,
                              ClinicRepository clinics,
                              PatientRepository patients,
                              AppointmentRepository appointments,
                              AvailabilityService availability,
                              Clock clock) {
        this.practices = practices;
        this.clinics = clinics;
        this.patients = patients;
        this.appointments = appointments;
        this.availability = availability;
        this.clock = clock;
    }

    public Appointment book(BookRequest request) {
        Practice practice = practices.findById(request.practiceId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Practice", request.practiceId().toString()));
        Clinic clinic = clinics.findById(practice.clinicId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Clinic", practice.clinicId().toString()));
        patients.findById(request.patientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Patient", request.patientId().toString()));

        Instant now = clock.instant();
        if (!request.startTime().isAfter(now)) {
            throw new InvalidSlotException(
                    "startTime " + request.startTime() + " is in the past");
        }

        // The legal slots on the requested day are the single source of truth for
        // both grid alignment and working-hours bounds. Reuse the availability
        // computation with no booked-subtractions to get the day's candidate slots.
        Instant dayStart = request.startTime().truncatedTo(ChronoUnit.SECONDS)
                .atZone(clinic.timezone()).toLocalDate()
                .atStartOfDay(clinic.timezone()).toInstant();
        Instant dayEnd = dayStart.plus(Duration.ofDays(1));
        List<Slot> daySlots = availability.compute(practice, clinic.timezone(), dayStart, dayEnd, List.of());
        boolean onGrid = daySlots.stream().anyMatch(s -> s.startTime().equals(request.startTime()));
        if (!onGrid) {
            // Differentiate "off grid" from "outside working hours" so error messages stay
            // useful — both are InvalidSlotException, but the detail varies.
            int slotMin = practice.slotDurationMinutes();
            long secondsFromMidnight = request.startTime()
                    .atZone(clinic.timezone()).toLocalTime().toSecondOfDay();
            boolean alignedToGrid = secondsFromMidnight % (slotMin * 60L) == 0;
            if (!alignedToGrid) {
                throw new InvalidSlotException(
                        "startTime " + request.startTime() + " is not aligned to the "
                                + slotMin + "-minute slot grid");
            }
            throw new InvalidSlotException(
                    "startTime " + request.startTime() + " is outside the practice's working hours");
        }

        Instant endTime = request.startTime()
                .plus(Duration.ofMinutes(practice.slotDurationMinutes()));
        Appointment appointment = new Appointment(
                UUID.randomUUID(),
                request.practiceId(),
                request.patientId(),
                request.startTime(),
                endTime,
                now
        );
        return appointments.save(appointment);
    }
}
