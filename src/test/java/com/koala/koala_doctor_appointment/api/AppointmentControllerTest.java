package com.koala.koala_doctor_appointment.api;

import com.koala.koala_doctor_appointment.appointment.Appointment;
import com.koala.koala_doctor_appointment.appointment.AppointmentService;
import com.koala.koala_doctor_appointment.appointment.BookRequest;
import com.koala.koala_doctor_appointment.appointment.SlotAlreadyBookedException;
import com.koala.koala_doctor_appointment.common.EntityNotFoundException;
import com.koala.koala_doctor_appointment.common.InvalidSlotException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AppointmentControllerTest {

    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");

    @MockitoBean
    private AppointmentService appointmentService;

    private MockMvc mockMvc;

    @Autowired
    void initMockMvc(WebApplicationContext context) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void postAppointment_returns201WithAppointmentBody() throws Exception {
        UUID appointmentId = UUID.fromString("e5f6a7b8-0000-4000-8000-000000000001");
        UUID practiceId = UUID.fromString("9ac71ce0-0000-4000-8000-000000000001");
        UUID patientId = UUID.fromString("9a71e171-0000-4000-8000-000000000001");
        Instant start = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();
        Instant end = start.plus(Duration.ofMinutes(30));
        Instant createdAt = Instant.parse("2026-05-04T00:00:00Z");

        Appointment booked = new Appointment(appointmentId, practiceId, patientId, start, end, createdAt);
        when(appointmentService.book(any(BookRequest.class))).thenReturn(booked);

        String body = """
                {
                  "practiceId": "%s",
                  "patientId":  "%s",
                  "startTime":  "%s"
                }
                """.formatted(practiceId, patientId, start);

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/appointments/" + appointmentId))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                .andExpect(jsonPath("$.practiceId").value(practiceId.toString()))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.startTime").value(start.toString()))
                .andExpect(jsonPath("$.endTime").value(end.toString()))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));
    }

    @Test
    void postAppointment_returns409WhenSlotAlreadyBooked() throws Exception {
        UUID practiceId = UUID.fromString("9ac71ce0-0000-4000-8000-000000000001");
        UUID patientId = UUID.fromString("9a71e171-0000-4000-8000-000000000001");
        Instant start = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();

        doThrow(new SlotAlreadyBookedException(practiceId, start))
                .when(appointmentService).book(any(BookRequest.class));

        String body = """
                {
                  "practiceId": "%s",
                  "patientId":  "%s",
                  "startTime":  "%s"
                }
                """.formatted(practiceId, patientId, start);

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Slot already booked"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString(practiceId.toString())));
    }

    @Test
    void postAppointment_returns422WhenSlotInvalid() throws Exception {
        UUID practiceId = UUID.fromString("9ac71ce0-0000-4000-8000-000000000001");
        UUID patientId = UUID.fromString("9a71e171-0000-4000-8000-000000000001");
        Instant start = ZonedDateTime.of(2026, 5, 4, 9, 7, 0, 0, SYDNEY).toInstant();

        doThrow(new InvalidSlotException("startTime " + start + " is not aligned to the 30-minute slot grid"))
                .when(appointmentService).book(any(BookRequest.class));

        String body = """
                {
                  "practiceId": "%s",
                  "patientId":  "%s",
                  "startTime":  "%s"
                }
                """.formatted(practiceId, patientId, start);

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Invalid slot"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("not aligned")));
    }

    @Test
    void postAppointment_returns404WhenPracticeNotFound() throws Exception {
        UUID missingPractice = UUID.randomUUID();
        UUID patientId = UUID.fromString("9a71e171-0000-4000-8000-000000000001");
        Instant start = ZonedDateTime.of(2026, 5, 4, 10, 0, 0, 0, SYDNEY).toInstant();

        doThrow(new EntityNotFoundException("Practice", missingPractice.toString()))
                .when(appointmentService).book(any(BookRequest.class));

        String body = """
                {
                  "practiceId": "%s",
                  "patientId":  "%s",
                  "startTime":  "%s"
                }
                """.formatted(missingPractice, patientId, start);

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Practice not found"));
    }

    @Test
    void postAppointment_returns400WhenBodyMissingFields() throws Exception {
        // Missing patientId and startTime
        String body = """
                { "practiceId": "9ac71ce0-0000-4000-8000-000000000001" }
                """;

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void postAppointment_returns400WhenBodyMalformedJson() throws Exception {
        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }
}
