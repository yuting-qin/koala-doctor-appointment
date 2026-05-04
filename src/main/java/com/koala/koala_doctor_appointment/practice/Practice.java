package com.koala.koala_doctor_appointment.practice;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Practice(
        UUID id,
        UUID doctorId,
        UUID clinicId,
        Map<DayOfWeek, List<TimeRange>> workingHours,
        int slotDurationMinutes
) {
}
