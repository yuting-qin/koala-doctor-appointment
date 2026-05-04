package com.koala.koala_doctor_appointment.config;

import com.koala.koala_doctor_appointment.clinic.Clinic;
import com.koala.koala_doctor_appointment.clinic.ClinicRepository;
import com.koala.koala_doctor_appointment.doctor.Doctor;
import com.koala.koala_doctor_appointment.doctor.DoctorRepository;
import com.koala.koala_doctor_appointment.practice.Practice;
import com.koala.koala_doctor_appointment.practice.PracticeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "app.seed.enabled=true")
class SeedDataLoaderTest {

    @Autowired ClinicRepository clinics;
    @Autowired DoctorRepository doctors;
    @Autowired PracticeRepository practices;

    @Test
    void seedsTwoClinicsInDifferentTimezones() {
        List<Clinic> all = clinics.findAll();
        assertThat(all).hasSize(2);
        assertThat(all).extracting(Clinic::name)
                .containsExactlyInAnyOrder("Koala Clinic", "Bear Clinic");
        assertThat(all).extracting(Clinic::timezone)
                .contains(ZoneId.of("Australia/Sydney"));
    }

    @Test
    void seedsAtLeastOneDoctorPracticingAtBothClinics() {
        long multiClinicDoctors = doctors.findAll().stream()
                .map(Doctor::id)
                .filter(id -> {
                    List<Practice> ps = practices.findByDoctorId(id);
                    return ps.stream().map(Practice::clinicId).distinct().count() >= 2;
                })
                .count();
        assertThat(multiClinicDoctors).isGreaterThanOrEqualTo(1);
    }
}
