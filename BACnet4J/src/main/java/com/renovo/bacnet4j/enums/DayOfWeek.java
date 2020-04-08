
package com.renovo.bacnet4j.enums;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.renovo.bacnet4j.type.primitive.Date;

public enum DayOfWeek {
    MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4), FRIDAY(5), SATURDAY(6), SUNDAY(7), UNSPECIFIED(255);

    private int id;

    DayOfWeek(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isSpecific() {
        return this != UNSPECIFIED;
    }

    public static DayOfWeek forCalendarDow(final int calendarDow) {
        switch (calendarDow) {
        case Calendar.MONDAY:
            return MONDAY;
        case Calendar.TUESDAY:
            return TUESDAY;
        case Calendar.WEDNESDAY:
            return WEDNESDAY;
        case Calendar.THURSDAY:
            return THURSDAY;
        case Calendar.FRIDAY:
            return FRIDAY;
        case Calendar.SATURDAY:
            return SATURDAY;
        case Calendar.SUNDAY:
            return SUNDAY;
        default:
            throw new RuntimeException("Can't get day of week for calendar value: " + calendarDow);
        }
    }

    public static DayOfWeek valueOf(final int id) {
        if (id == MONDAY.id)
            return MONDAY;
        if (id == TUESDAY.id)
            return TUESDAY;
        if (id == WEDNESDAY.id)
            return WEDNESDAY;
        if (id == THURSDAY.id)
            return THURSDAY;
        if (id == FRIDAY.id)
            return FRIDAY;
        if (id == SATURDAY.id)
            return SATURDAY;
        if (id == SUNDAY.id)
            return SUNDAY;
        return UNSPECIFIED;
    }

    public static DayOfWeek forDate(final Date date) {
        final GregorianCalendar gc = date.calculateGC();
        final int calendarDow = gc.get(Calendar.DAY_OF_WEEK);
        return forCalendarDow(calendarDow);
    }

    public boolean matches(final Date date) {
        if (this == DayOfWeek.UNSPECIFIED)
            return true;

        final DayOfWeek thatDow = date.getDayOfWeek();

        // Tolerate the day of week of the given date being unspecified.
        if (thatDow.isSpecific())
            return this == thatDow;

        // Otherwise we need to calculate the given date's day of week.
        return equals(forDate(date));
    }
}
