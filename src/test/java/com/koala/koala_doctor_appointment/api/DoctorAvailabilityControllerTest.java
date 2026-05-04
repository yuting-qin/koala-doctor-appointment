package com.koala.koala_doctor_appointment.api;

import com.koala.koala_doctor_appointment.availability.AvailabilityOrchestrator;
import com.koala.koala_doctor_appointment.availability.DoctorAvailability;
import com.koala.koala_doctor_appointment.availability.PracticeAvailability;
import com.koala.koala_doctor_appointment.availability.Slot;
import com.koala.koala_doctor_appointment.clinic.Clinic;
import com.koala.koala_doctor_appointment.common.EntityNotFoundException;
import com.koala.koala_doctor_appointment.doctor.Doctor;
import com.koala.koala_doctor_appointment.practice.Practice;
import com.koala.koala_doctor_appointment.practice.TimeRange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DoctorAvailabilityControllerTest {

    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @MockitoBean
    private AvailabilityOrchestrator orchestrator;

    private MockMvc mockMvc;

    @Autowired
    void initMockMvc(WebApplicationContext context) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getDoctorAvailability_returnsPracticesAcrossClinics() throws Exception {
        UUID doctorId = UUID.randomUUID();
        UUID koalaClinicId = UUID.randomUUID();
        UUID bearClinicId = UUID.randomUUID();
        UUID koalaPracticeId = UUID.randomUUID();
        UUID bearPracticeId = UUID.randomUUID();

        Doctor doctor = new Doctor(doctorId, "Dr. Lee", "GP");
        Clinic koala = new Clinic(koalaClinicId, "Koala Clinic", SYDNEY);
        Clinic bear = new Clinic(bearClinicId, "Bear Clinic", NEW_YORK);
        Practice koalaPractice = new Practice(
                koalaPracticeId, doctorId, koalaClinicId,
                Map.of(DayOfWeek.MONDAY,
                        List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(10, 0)))),
                30
        );
        Practice bearPractice = new Practice(
                bearPracticeId, doctorId, bearClinicId,
                Map.of(DayOfWeek.TUESDAY,
                        List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(9, 45)))),
                45
        );

        Instant koalaSlotStart = ZonedDateTime.of(2026, 5, 4, 9, 0, 0, 0, SYDNEY).toInstant();
        Instant koalaSlotEnd = ZonedDateTime.of(2026, 5, 4, 9, 30, 0, 0, SYDNEY).toInstant();
        Instant bearSlotStart = ZonedDateTime.of(2026, 5, 5, 9, 0, 0, 0, NEW_YORK).toInstant();
        Instant bearSlotEnd = ZonedDateTime.of(2026, 5, 5, 9, 45, 0, 0, NEW_YORK).toInstant();

        Instant from = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant to = ZonedDateTime.of(2026, 5, 7, 0, 0, 0, 0, SYDNEY).toInstant();

        DoctorAvailability stub = new DoctorAvailability(
                doctor, from, to,
                List.of(
                        new PracticeAvailability(koalaPractice, doctor, koala,
                                List.of(new Slot(koalaSlotStart, koalaSlotEnd))),
                        new PracticeAvailability(bearPractice, doctor, bear,
                                List.of(new Slot(bearSlotStart, bearSlotEnd)))
                )
        );
        when(orchestrator.getForDoctor(any(), any(), any())).thenReturn(stub);

        mockMvc.perform(get("/doctors/{doctorId}/availability", doctorId)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.doctorId").value(doctorId.toString()))
                .andExpect(jsonPath("$.doctorName").value("Dr. Lee"))
                .andExpect(jsonPath("$.specialty").value("GP"))
                .andExpect(jsonPath("$.from").value(from.toString()))
                .andExpect(jsonPath("$.to").value(to.toString()))
                .andExpect(jsonPath("$.practices.length()").value(2))
                .andExpect(jsonPath("$.practices[0].practiceId").value(koalaPracticeId.toString()))
                .andExpect(jsonPath("$.practices[0].clinicId").value(koalaClinicId.toString()))
                .andExpect(jsonPath("$.practices[0].clinicName").value("Koala Clinic"))
                .andExpect(jsonPath("$.practices[0].timezone").value("Australia/Sydney"))
                .andExpect(jsonPath("$.practices[0].slotDurationMinutes").value(30))
                .andExpect(jsonPath("$.practices[0].slots.length()").value(1))
                .andExpect(jsonPath("$.practices[1].clinicId").value(bearClinicId.toString()))
                .andExpect(jsonPath("$.practices[1].timezone").value("America/New_York"))
                .andExpect(jsonPath("$.practices[1].slotDurationMinutes").value(45));
    }

    @Test
    void getDoctorAvailability_returns404WhenDoctorNotFound() throws Exception {
        UUID missing = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Doctor", missing.toString()))
                .when(orchestrator).getForDoctor(any(), any(), any());

        Instant from = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant to = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();

        mockMvc.perform(get("/doctors/{doctorId}/availability", missing)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Doctor not found"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString(missing.toString())));
    }
}
