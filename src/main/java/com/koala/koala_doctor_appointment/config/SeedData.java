package com.koala.koala_doctor_appointment.config;

import com.koala.koala_doctor_appointment.clinic.Clinic;
import com.koala.koala_doctor_appointment.doctor.Doctor;
import com.koala.koala_doctor_appointment.practice.Practice;
import com.koala.koala_doctor_appointment.practice.TimeRange;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SeedData(List<Clinic> clinics, List<Doctor> doctors, List<Practice> practices) {

    public static SeedData empty() {
        return new SeedData(List.of(), List.of(), List.of());
    }

    /**
     * The dummy world used for local development. Two clinics in different timezones,
     * three doctors (one of whom practices at both clinics), four practices total.
     */
    public static SeedData defaults() {
        UUID koalaId = UUID.fromString("c0a1ac01-0000-4000-8000-000000000001");
        UUID bearId = UUID.fromString("c0a1ac01-0000-4000-8000-000000000002");

        UUID drLeeId = UUID.fromString("d0c70900-0000-4000-8000-000000000001");
        UUID drPatelId = UUID.fromString("d0c70900-0000-4000-8000-000000000002");
        UUID drKimId = UUID.fromString("d0c70900-0000-4000-8000-000000000003");

        UUID drLeeKoalaPracticeId = UUID.fromString("9ac71ce0-0000-4000-8000-000000000001");
        UUID drLeeBearPracticeId = UUID.fromString("9ac71ce0-0000-4000-8000-000000000002");
        UUID drPatelKoalaId = UUID.fromString("9ac71ce0-0000-4000-8000-000000000003");
        UUID drKimBearId = UUID.fromString("9ac71ce0-0000-4000-8000-000000000004");

        Clinic koala = new Clinic(koalaId, "Koala Clinic", ZoneId.of("Australia/Sydney"));
        Clinic bear = new Clinic(bearId, "Bear Clinic", ZoneId.of("America/New_York"));

        Doctor lee = new Doctor(drLeeId, "Dr. Lee", "GP");
        Doctor patel = new Doctor(drPatelId, "Dr. Patel", "Pediatrics");
        Doctor kim = new Doctor(drKimId, "Dr. Kim", "Cardiology");

        Map<DayOfWeek, List<TimeRange>> mwfMornings = Map.of(
                DayOfWeek.MONDAY, List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                DayOfWeek.WEDNESDAY, List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                DayOfWeek.FRIDAY, List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)))
        );
        Map<DayOfWeek, List<TimeRange>> tueThuAfternoons = Map.of(
                DayOfWeek.TUESDAY, List.of(new TimeRange(LocalTime.of(13, 0), LocalTime.of(17, 0))),
                DayOfWeek.THURSDAY, List.of(new TimeRange(LocalTime.of(13, 0), LocalTime.of(17, 0)))
        );
        Map<DayOfWeek, List<TimeRange>> weekdaysWithLunch = Map.of(
                DayOfWeek.MONDAY, List.of(
                        new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new TimeRange(LocalTime.of(13, 0), LocalTime.of(17, 0))),
                DayOfWeek.TUESDAY, List.of(
                        new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new TimeRange(LocalTime.of(13, 0), LocalTime.of(17, 0))),
                DayOfWeek.WEDNESDAY, List.of(
                        new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new TimeRange(LocalTime.of(13, 0), LocalTime.of(17, 0))),
                DayOfWeek.THURSDAY, List.of(
                        new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new TimeRange(LocalTime.of(13, 0), LocalTime.of(17, 0)))
        );

        Practice drLeeKoala = new Practice(drLeeKoalaPracticeId, drLeeId, koalaId, mwfMornings, 30);
        Practice drLeeBear = new Practice(drLeeBearPracticeId, drLeeId, bearId, tueThuAfternoons, 30);
        Practice drPatelKoala = new Practice(drPatelKoalaId, drPatelId, koalaId, weekdaysWithLunch, 20);
        Practice drKimBear = new Practice(drKimBearId, drKimId, bearId, weekdaysWithLunch, 45);

        return new SeedData(
                List.of(koala, bear),
                List.of(lee, patel, kim),
                List.of(drLeeKoala, drLeeBear, drPatelKoala, drKimBear)
        );
    }
}
