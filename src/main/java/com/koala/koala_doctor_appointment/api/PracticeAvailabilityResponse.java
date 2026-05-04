package com.koala.koala_doctor_appointment.api;

import java.util.List;
import java.util.UUID;

public record PracticeAvailabilityResponse(
        UUID practiceId,
        UUID doctorId,
        String doctorName,
        String specialty,
        UUID clinicId,
        String clinicName,
        String timezone,
        int slotDurationMinutes,
        List<SlotResponse> slots
) {
}
