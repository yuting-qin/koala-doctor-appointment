package com.koala.koala_doctor_appointment.availability;

import com.koala.koala_doctor_appointment.appointment.Appointment;
import com.koala.koala_doctor_appointment.appointment.AppointmentRepository;
import com.koala.koala_doctor_appointment.clinic.Clinic;
import com.koala.koala_doctor_appointment.clinic.ClinicRepository;
import com.koala.koala_doctor_appointment.common.EntityNotFoundException;
import com.koala.koala_doctor_appointment.doctor.Doctor;
import com.koala.koala_doctor_appointment.doctor.DoctorRepository;
import com.koala.koala_doctor_appointment.practice.Practice;
import com.koala.koala_doctor_appointment.practice.PracticeRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AvailabilityOrchestrator {

    private final ClinicRepository clinics;
    private final DoctorRepository doctors;
    private final PracticeRepository practices;
    private final AppointmentRepository appointments;
    private final AvailabilityService availability;

    public AvailabilityOrchestrator(ClinicRepository clinics,
                                    DoctorRepository doctors,
                                    PracticeRepository practices,
                                    AppointmentRepository appointments,
                                    AvailabilityService availability) {
        this.clinics = clinics;
        this.doctors = doctors;
        this.practices = practices;
        this.appointments = appointments;
        this.availability = availability;
    }

    public ClinicAvailability getForClinic(UUID clinicId, Instant from, Instant to) {
        Clinic clinic = clinics.findById(clinicId)
                .orElseThrow(() -> new EntityNotFoundException("Clinic", clinicId.toString()));
        List<PracticeAvailability> practiceResults = practices.findByClinicId(clinicId).stream()
                .map(p -> {
                    Doctor doctor = doctors.findById(p.doctorId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Doctor", p.doctorId().toString()));
                    return buildPracticeAvailability(p, doctor, clinic, from, to);
                })
                .toList();
        return new ClinicAvailability(clinic, from, to, practiceResults);
    }

    public DoctorAvailability getForDoctor(UUID doctorId, Instant from, Instant to) {
        Doctor doctor = doctors.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor", doctorId.toString()));
        List<PracticeAvailability> practiceResults = practices.findByDoctorId(doctorId).stream()
                .map(p -> {
                    Clinic clinic = clinics.findById(p.clinicId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Clinic", p.clinicId().toString()));
                    return buildPracticeAvailability(p, doctor, clinic, from, to);
                })
                .toList();
        return new DoctorAvailability(doctor, from, to, practiceResults);
    }

    private PracticeAvailability buildPracticeAvailability(Practice practice, Doctor doctor,
                                                           Clinic clinic,
                                                           Instant from, Instant to) {
        List<Appointment> existing = appointments.findByPracticeIdAndRange(practice.id(), from, to);
        List<Slot> slots = availability.compute(practice, clinic.timezone(), from, to, existing);
        return new PracticeAvailability(practice, doctor, clinic, slots);
    }
}
