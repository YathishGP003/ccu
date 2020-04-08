
package com.renovo.bacnet4j.type.constructed;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.enums.DayOfWeek;
import com.renovo.bacnet4j.enums.Month;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetRuntimeException;
import com.renovo.bacnet4j.type.DateMatchable;
import com.renovo.bacnet4j.type.primitive.Date;
import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class WeekNDay extends OctetString implements DateMatchable {
    public static class WeekOfMonth extends Enumerated {
        public static final WeekOfMonth days1to7 = new WeekOfMonth(1);
        public static final WeekOfMonth days8to14 = new WeekOfMonth(2);
        public static final WeekOfMonth days15to21 = new WeekOfMonth(3);
        public static final WeekOfMonth days22to28 = new WeekOfMonth(4);
        public static final WeekOfMonth days29to31 = new WeekOfMonth(5);
        public static final WeekOfMonth last7Days = new WeekOfMonth(6);
        public static final WeekOfMonth any = new WeekOfMonth(255);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

        public static WeekOfMonth forName(final String name) {
            return (WeekOfMonth) Enumerated.forName(nameMap, name);
        }
        
        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }
        
        public static WeekOfMonth valueOf(final byte b) {
            switch (b) {
            case 1:
                return days1to7;
            case 2:
                return days8to14;
            case 3:
                return days15to21;
            case 4:
                return days22to28;
            case 5:
                return days29to31;
            case 6:
                return last7Days;
            default:
                return any;
            }
        }

        private WeekOfMonth(final int value) {
            super(value);
        }

        public WeekOfMonth(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        /**
         * Returns a unmodifiable map.
         *
         * @return unmodifiable map
         */
        public static Map<Integer, String> getPrettyMap() {
            return Collections.unmodifiableMap(prettyMap);
        }

        /**
         * Returns a unmodifiable nameMap.
         *
         * @return unmodifiable map
         */
        public static Map<String, Enumerated> getNameMap() {
            return Collections.unmodifiableMap(nameMap);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }

    public WeekNDay(final Month month, final WeekOfMonth weekOfMonth, final DayOfWeek dayOfWeek) {
        super(new byte[] { month.getId(), weekOfMonth.byteValue(), (byte) dayOfWeek.getId() });
    }

    public Month getMonth() {
        return Month.valueOf(getBytes()[0]);
    }

    public WeekOfMonth getWeekOfMonth() {
        return WeekOfMonth.valueOf(getBytes()[1]);
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.valueOf(getBytes()[2]);
    }

    public WeekNDay(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    @Override
    public boolean matches(final Date that) {
        if (!that.isSpecific())
            throw new BACnetRuntimeException("Dates for matching must be completely specified: " + that);

        if (!getMonth().matches(that.getMonth()))
            return false;

        if (!matchWeekOfMonth(that))
            return false;

        if (!getDayOfWeek().matches(that))
            return false;

        return true;
    }

    private boolean matchWeekOfMonth(final Date that) {
        final WeekOfMonth wom = getWeekOfMonth();
        if (wom.equals(WeekOfMonth.any))
            return true;
        final int day = that.getDay();
        if (wom.equals(WeekOfMonth.days1to7))
            return day >= 1 && day <= 7;
        if (wom.equals(WeekOfMonth.days8to14))
            return day >= 8 && day <= 14;
        if (wom.equals(WeekOfMonth.days15to21))
            return day >= 15 && day <= 21;
        if (wom.equals(WeekOfMonth.days22to28))
            return day >= 22 && day <= 28;
        if (wom.equals(WeekOfMonth.days29to31))
            return day >= 29 && day <= 31;

        // Calculate the last day of the month.
        final GregorianCalendar gc = that.calculateGC();
        final int lastDay = gc.getActualMaximum(Calendar.DATE);
        return day >= lastDay - 6 && day <= lastDay;
    }

    @Override
    public String toString() {
        return "WeekNDay [Month=" + getMonth() + ", WeekOfMonth=" + getWeekOfMonth() + ", DayOfWeek=" + getDayOfWeek() + "]";
    }

}
