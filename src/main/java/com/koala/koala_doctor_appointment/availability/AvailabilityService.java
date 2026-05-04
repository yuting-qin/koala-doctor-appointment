package com.koala.koala_doctor_appointment.availability;

import com.koala.koala_doctor_appointment.appointment.Appointment;
import com.koala.koala_doctor_appointment.practice.Practice;
import com.koala.koala_doctor_appointment.practice.TimeRange;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AvailabilityService {

    public List<Slot> compute(Practice practice, ZoneId zone, Instant from, Instant to,
                              List<Appointment> existing) {
        Set<Instant> bookedStarts = existing.stream()
                .map(Appointment::startTime)
                .collect(Collectors.toSet());

        List<Slot> result = new ArrayList<>();
        LocalDate startDate = from.atZone(zone).toLocalDate();
        LocalDate endDate = to.atZone(zone).toLocalDate();
        int slotMin = practice.slotDurationMinutes();

        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            List<TimeRange> ranges = practice.workingHours()
                    .getOrDefault(date.getDayOfWeek(), List.of());
            for (TimeRange range : ranges) {
                LocalTime cursor = range.start();
                while (!cursor.plusMinutes(slotMin).isAfter(range.end())) {
                    Instant slotStart = ZonedDateTime.of(date, cursor, zone).toInstant();
                    Instant slotEnd = slotStart.plus(Duration.ofMinutes(slotMin));
                    if (!bookedStarts.contains(slotStart)) {
                        result.add(new Slot(slotStart, slotEnd));
                    }
                    cursor = cursor.plusMinutes(slotMin);
                }
            }
        }
        return result;
    }
}
