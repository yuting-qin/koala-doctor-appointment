package com.koala.koala_doctor_appointment.api;

import com.koala.koala_doctor_appointment.appointment.SlotAlreadyBookedException;
import com.koala.koala_doctor_appointment.common.EntityNotFoundException;
import com.koala.koala_doctor_appointment.common.InvalidSlotException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle(ex.entityType() + " not found");
        problem.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<ProblemDetail> handleSlotAlreadyBooked(SlotAlreadyBookedException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Slot already booked");
        problem.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(InvalidSlotException.class)
    public ResponseEntity<ProblemDetail> handleInvalidSlot(InvalidSlotException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problem.setTitle("Invalid slot");
        problem.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problem);
    }
}
