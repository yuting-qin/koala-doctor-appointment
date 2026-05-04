package com.koala.koala_doctor_appointment.api;

import com.koala.koala_doctor_appointment.appointment.Appointment;
import com.koala.koala_doctor_appointment.appointment.AppointmentService;
import com.koala.koala_doctor_appointment.appointment.BookRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody AppointmentRequest request) {
        Appointment booked = appointmentService.book(
                new BookRequest(request.practiceId(), request.patientId(), request.startTime()));
        AppointmentResponse body = AppointmentResponse.from(booked);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/appointments/" + booked.id());
        return ResponseEntity.created(java.net.URI.create("/appointments/" + booked.id()))
                .headers(headers)
                .body(body);
    }
}
