package com.koala.koala_doctor_appointment.api;

import com.koala.koala_doctor_appointment.availability.ClinicAvailability;
import com.koala.koala_doctor_appointment.availability.DoctorAvailability;
import com.koala.koala_doctor_appointment.availability.PracticeAvailability;

final class AvailabilityMappers {

    private AvailabilityMappers() {
    }

    static DoctorAvailabilityResponse toResponse(DoctorAvailability source) {
        return new DoctorAvailabilityResponse(
                source.doctor().id(),
                source.doctor().name(),
                source.doctor().specialty(),
                source.from(),
                source.to(),
                source.practices().stream()
                        .map(AvailabilityMappers::toResponse)
                        .toList()
        );
    }

    static ClinicAvailabilityResponse toResponse(ClinicAvailability source) {
        return new ClinicAvailabilityResponse(
                source.clinic().id(),
                source.clinic().name(),
                source.clinic().timezone().getId(),
                source.from(),
                source.to(),
                source.practices().stream()
                        .map(AvailabilityMappers::toResponse)
                        .toList()
        );
    }

    static PracticeAvailabilityResponse toResponse(PracticeAvailability source) {
        return new PracticeAvailabilityResponse(
                source.practice().id(),
                source.doctor().id(),
                source.doctor().name(),
                source.doctor().specialty(),
                source.clinic().id(),
                source.clinic().name(),
                source.clinic().timezone().getId(),
                source.practice().slotDurationMinutes(),
                source.slots().stream()
                        .map(s -> new SlotResponse(s.startTime(), s.endTime()))
                        .toList()
        );
    }
}
