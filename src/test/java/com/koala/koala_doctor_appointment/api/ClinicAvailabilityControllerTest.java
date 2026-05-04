package com.koala.koala_doctor_appointment.api;

import com.koala.koala_doctor_appointment.availability.AvailabilityOrchestrator;
import com.koala.koala_doctor_appointment.availability.ClinicAvailability;
import com.koala.koala_doctor_appointment.availability.PracticeAvailability;
import com.koala.koala_doctor_appointment.availability.Slot;
import com.koala.koala_doctor_appointment.common.EntityNotFoundException;
import com.koala.koala_doctor_appointment.clinic.Clinic;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ClinicAvailabilityControllerTest {

    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");

    @MockitoBean
    private AvailabilityOrchestrator orchestrator;

    private MockMvc mockMvc;

    @Autowired
    void initMockMvc(WebApplicationContext context) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getClinicAvailability_returnsJsonWithSlots() throws Exception {
        UUID clinicId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID practiceId = UUID.randomUUID();

        Clinic clinic = new Clinic(clinicId, "Koala Clinic", SYDNEY);
        Doctor doctor = new Doctor(doctorId, "Dr. Lee", "GP");
        Practice practice = new Practice(
                practiceId, doctorId, clinicId,
                Map.of(DayOfWeek.MONDAY,
                        List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(10, 0)))),
                30
        );
        Instant slot1Start = ZonedDateTime.of(2026, 5, 4, 9, 0, 0, 0, SYDNEY).toInstant();
        Instant slot1End = ZonedDateTime.of(2026, 5, 4, 9, 30, 0, 0, SYDNEY).toInstant();
        Instant slot2Start = ZonedDateTime.of(2026, 5, 4, 9, 30, 0, 0, SYDNEY).toInstant();
        Instant slot2End = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();

        Instant from = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant to = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();

        ClinicAvailability stub = new ClinicAvailability(
                clinic, from, to,
                List.of(new PracticeAvailability(
                        practice, doctor, clinic,
                        List.of(new Slot(slot1Start, slot1End), new Slot(slot2Start, slot2End))
                ))
        );
        when(orchestrator.getForClinic(any(), any(), any())).thenReturn(stub);

        mockMvc.perform(get("/clinics/{clinicId}/availability", clinicId)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clinicId").value(clinicId.toString()))
                .andExpect(jsonPath("$.clinicName").value("Koala Clinic"))
                .andExpect(jsonPath("$.timezone").value("Australia/Sydney"))
                .andExpect(jsonPath("$.from").value("2026-05-03T14:00:00Z"))
                .andExpect(jsonPath("$.to").value("2026-05-04T14:00:00Z"))
                .andExpect(jsonPath("$.practices.length()").value(1))
                .andExpect(jsonPath("$.practices[0].practiceId").value(practiceId.toString()))
                .andExpect(jsonPath("$.practices[0].doctorId").value(doctorId.toString()))
                .andExpect(jsonPath("$.practices[0].doctorName").value("Dr. Lee"))
                .andExpect(jsonPath("$.practices[0].specialty").value("GP"))
                .andExpect(jsonPath("$.practices[0].slotDurationMinutes").value(30))
                .andExpect(jsonPath("$.practices[0].slots.length()").value(2))
                .andExpect(jsonPath("$.practices[0].slots[0].startTime")
                        .value(slot1Start.toString()))
                .andExpect(jsonPath("$.practices[0].slots[0].endTime")
                        .value(slot1End.toString()));
    }

    @Test
    void getClinicAvailability_returns404ProblemDetailWhenClinicNotFound() throws Exception {
        UUID missing = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Clinic", missing.toString()))
                .when(orchestrator).getForClinic(any(), any(), any());

        Instant from = ZonedDateTime.of(2026, 5, 4, 0, 0, 0, 0, SYDNEY).toInstant();
        Instant to = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();

        mockMvc.perform(get("/clinics/{clinicId}/availability", missing)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Clinic not found"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString(missing.toString())));
    }

    @Test
    void getClinicAvailability_returns400WhenFromMissing() throws Exception {
        UUID clinicId = UUID.randomUUID();
        Instant to = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();

        mockMvc.perform(get("/clinics/{clinicId}/availability", clinicId)
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getClinicAvailability_returns400WhenFromMalformed() throws Exception {
        UUID clinicId = UUID.randomUUID();
        Instant to = ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, SYDNEY).toInstant();

        mockMvc.perform(get("/clinics/{clinicId}/availability", clinicId)
                        .param("from", "not-a-date")
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }
}
