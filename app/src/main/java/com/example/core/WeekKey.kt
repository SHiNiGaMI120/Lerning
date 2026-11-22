package com.example.core

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * ISO-8601 week helpers (Monday-first, week 1 = first week with 4+ days).
 *
 * Implemented with [GregorianCalendar] instead of java.time so it works on
 * minSdk 24 without core library desugaring. Used to make weekly challenges
 * reset every week instead of accumulating lifetime progress.
 */
object WeekKey {

    private fun isoCalendar(timeMillis: Long): GregorianCalendar =
        GregorianCalendar(TimeZone.getDefault()).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = timeMillis
        }

    /** Key for the current ISO week, e.g. "2026-W47". */
    fun current(): String = of(System.currentTimeMillis())

    /** Key for the ISO week containing [timeMillis], e.g. "2026-W47". */
    fun of(timeMillis: Long): String {
        val cal = isoCalendar(timeMillis)
        return "%04d-W%02d".format(cal.weekYear, cal.get(Calendar.WEEK_OF_YEAR))
    }

    /** Start (inclusive) of the current ISO week, in epoch milliseconds. */
    fun currentWeekStartMillis(): Long {
        val cal = isoCalendar(System.currentTimeMillis())
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
