package com.example.calculations

import com.example.model.HijriDateInfo
import com.example.model.IslamicHoliday
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

/**
 * Offline Hijri calendar engine backed by java.time.chrono.HijrahDate (Umm al-Qura tabular)
 * with explicit user day offset (-2..+2) and moon-sighting variance awareness.
 */
object HijriCalendarEngine {

    val importantDates: List<IslamicHoliday> = listOf(
        IslamicHoliday(1, 1, "islamic_new_year"),
        IslamicHoliday(1, 10, "ashura"),
        IslamicHoliday(3, 12, "mawlid"),
        IslamicHoliday(7, 27, "isra_miraj"),
        IslamicHoliday(8, 15, "shab_e_barat"),
        IslamicHoliday(9, 1, "ramadan_start"),
        IslamicHoliday(9, 27, "laylatul_qadr"),
        IslamicHoliday(10, 1, "eid_al_fitr"),
        IslamicHoliday(12, 9, "day_of_arafah"),
        IslamicHoliday(12, 10, "eid_al_adha")
    )

    fun toHijri(gregorianDate: LocalDate, offsetDays: Int = 0): HijriDateInfo {
        val adjusted = gregorianDate.plusDays(offsetDays.toLong())
        val hijri = HijrahDate.from(adjusted)
        val day = hijri.get(ChronoField.DAY_OF_MONTH)
        val month = hijri.get(ChronoField.MONTH_OF_YEAR)
        val year = hijri.get(ChronoField.YEAR_OF_ERA)
        return HijriDateInfo(
            day = day,
            month = month,
            year = year,
            gregorianIso = gregorianDate.toString(),
            isRamadan = month == 9
        )
    }

    data class CalendarCell(
        val gregorianDate: LocalDate,
        val hijri: HijriDateInfo,
        val isToday: Boolean,
        val holidayKey: String?
    )

    fun buildHijriMonthGrid(
        hijriYear: Int,
        hijriMonth: Int,
        offsetDays: Int = 0,
        today: LocalDate = LocalDate.now()
    ): List<CalendarCell> {
        val firstOfHijriMonth = HijrahDate.of(hijriYear, hijriMonth, 1)
        val monthLength = firstOfHijriMonth.lengthOfMonth()
        val cells = mutableListOf<CalendarCell>()
        for (d in 1..monthLength) {
            val hDate = HijrahDate.of(hijriYear, hijriMonth, d)
            // Inverse of adding offsetDays when converting Gregorian -> Hijri
            val gDate = LocalDate.from(hDate).minusDays(offsetDays.toLong())
            val info = HijriDateInfo(
                day = d,
                month = hijriMonth,
                year = hijriYear,
                gregorianIso = gDate.toString(),
                isRamadan = hijriMonth == 9
            )
            val holiday = importantDates.firstOrNull {
                it.hijriMonth == hijriMonth && it.hijriDay == d
            }?.key
            cells.add(
                CalendarCell(
                    gregorianDate = gDate,
                    hijri = info,
                    isToday = gDate == today,
                    holidayKey = holiday
                )
            )
        }
        return cells
    }

    fun daysUntilRamadan(today: LocalDate = LocalDate.now(), offsetDays: Int = 0): Long {
        val current = toHijri(today, offsetDays)
        if (current.isRamadan) return 0L
        val targetYear = if (current.month < 9) current.year else current.year + 1
        val nextRamadan = HijrahDate.of(targetYear, 9, 1)
        val gRamadan = LocalDate.from(nextRamadan).minusDays(offsetDays.toLong())
        return ChronoUnit.DAYS.between(today, gRamadan).coerceAtLeast(0L)
    }
}
