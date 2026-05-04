package com.koala.koala_doctor_appointment.config;

import com.koala.koala_doctor_appointment.appointment.AppointmentRepository;
import com.koala.koala_doctor_appointment.appointment.AppointmentService;
import com.koala.koala_doctor_appointment.appointment.InMemoryAppointmentRepository;
import com.koala.koala_doctor_appointment.availability.AvailabilityOrchestrator;
import com.koala.koala_doctor_appointment.availability.AvailabilityService;
import com.koala.koala_doctor_appointment.clinic.ClinicRepository;
import com.koala.koala_doctor_appointment.clinic.InMemoryClinicRepository;
import com.koala.koala_doctor_appointment.doctor.DoctorRepository;
import com.koala.koala_doctor_appointment.doctor.InMemoryDoctorRepository;
import com.koala.koala_doctor_appointment.patient.InMemoryPatientRepository;
import com.koala.koala_doctor_appointment.patient.Patient;
import com.koala.koala_doctor_appointment.patient.PatientRepository;
import com.koala.koala_doctor_appointment.practice.InMemoryPracticeRepository;
import com.koala.koala_doctor_appointment.practice.PracticeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Configuration
public class AppConfig {

    @Bean
    SeedData seedData(@Value("${app.seed.enabled:true}") boolean seedEnabled) {
        return seedEnabled ? SeedData.defaults() : SeedData.empty();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ClinicRepository clinicRepository(SeedData seed) {
        return new InMemoryClinicRepository(seed.clinics());
    }

    @Bean
    DoctorRepository doctorRepository(SeedData seed) {
        return new InMemoryDoctorRepository(seed.doctors());
    }

    @Bean
    PracticeRepository practiceRepository(SeedData seed) {
        return new InMemoryPracticeRepository(seed.practices());
    }

    @Bean
    PatientRepository patientRepository(@Value("${app.seed.enabled:true}") boolean seedEnabled) {
        if (!seedEnabled) {
            return new InMemoryPatientRepository(List.of());
        }
        Patient alice = new Patient(
                UUID.fromString("9a71e171-0000-4000-8000-000000000001"),
                "Alice Nguyen", "alice@example.com", "+61400000001");
        Patient bob = new Patient(
                UUID.fromString("9a71e171-0000-4000-8000-000000000002"),
                "Bob Singh", "bob@example.com", "+61400000002");
        return new InMemoryPatientRepository(List.of(alice, bob));
    }

    @Bean
    AppointmentRepository appointmentRepository() {
        return new InMemoryAppointmentRepository();
    }

    @Bean
    AvailabilityService availabilityService() {
        return new AvailabilityService();
    }

    @Bean
    AvailabilityOrchestrator availabilityOrchestrator(ClinicRepository clinics,
                                                      DoctorRepository doctors,
                                                      PracticeRepository practices,
                                                      AppointmentRepository appointments,
                                                      AvailabilityService availability) {
        return new AvailabilityOrchestrator(clinics, doctors, practices, appointments, availability);
    }

    @Bean
    AppointmentService appointmentService(PracticeRepository practices,
                                          ClinicRepository clinics,
                                          PatientRepository patients,
                                          AppointmentRepository appointments,
                                          AvailabilityService availability,
                                          Clock clock) {
        return new AppointmentService(practices, clinics, patients, appointments, availability, clock);
    }
}
