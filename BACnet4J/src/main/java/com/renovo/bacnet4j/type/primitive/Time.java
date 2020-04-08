
package com.renovo.bacnet4j.type.primitive;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class Time extends Primitive {
    public static final int MAX_TIME = 24 * 60 * 60 * 100;
    public static final int UNSPECIFIC = 255;
    public static final Time UNSPECIFIED = new Time(UNSPECIFIC, UNSPECIFIC, UNSPECIFIC, UNSPECIFIC);

    public static final byte TYPE_ID = 11;

    private final int hour;
    private final int minute;
    private final int second;
    private final int hundredth;

    public Time(final int hour, final int minute, final int second, final int hundredth) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.hundredth = hundredth;
    }

    public Time(final LocalDevice localDevice) {
        this(getNow(localDevice));
    }

    private static GregorianCalendar getNow(final LocalDevice localDevice) {
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(localDevice.getClock().millis());
        return gc;
    }

    public Time(final GregorianCalendar now) {
        this.hour = now.get(Calendar.HOUR_OF_DAY);
        this.minute = now.get(Calendar.MINUTE);
        this.second = now.get(Calendar.SECOND);
        this.hundredth = now.get(Calendar.MILLISECOND) / 10;
    }

    public boolean isHourUnspecified() {
        return hour == UNSPECIFIC;
    }

    public int getHour() {
        return hour;
    }

    public boolean isMinuteUnspecified() {
        return minute == UNSPECIFIC;
    }

    public int getMinute() {
        return minute;
    }

    public boolean isSecondUnspecified() {
        return second == UNSPECIFIC;
    }

    public int getSecond() {
        return second;
    }

    public boolean isHundredthUnspecified() {
        return hundredth == UNSPECIFIC;
    }

    public int getHundredth() {
        return hundredth;
    }

    public boolean isFullySpecified() {
        return !isHourUnspecified() && !isMinuteUnspecified() && !isSecondUnspecified() && !isHundredthUnspecified();
    }

    public int getHundredthInDay() {
        return hour * 60 * 60 * 100 //
                + minute * 60 * 100 //
                + second * 100 //
                + hundredth;
    }

    public int getSmallestDiff(final Time that) {
        final int thishun = getHundredthInDay();
        final int thathun = that.getHundredthInDay();

        if (thishun == thathun)
            return 0;

        int contiguous;
        int wraparound;

        if (thishun > thathun) {
            contiguous = thishun - thathun;
            wraparound = thathun + MAX_TIME - thishun;
        } else {
            contiguous = thathun - thishun;
            wraparound = thishun + MAX_TIME - thathun;
        }

        return contiguous > wraparound ? wraparound : contiguous;
    }

    /**
     * @param that
     *            The time with which to compare this
     * @return true if this < that.
     */
    public boolean before(final Time that) {
        if (!this.isHourUnspecified() && !that.isHourUnspecified()) {
            if (this.hour < that.hour)
                return true;
            if (this.hour > that.hour)
                return false;
        }

        if (!this.isMinuteUnspecified() && !that.isMinuteUnspecified()) {
            if (this.minute < that.minute)
                return true;
            if (this.minute > that.minute)
                return false;
        }

        if (!this.isSecondUnspecified() && !that.isSecondUnspecified()) {
            if (this.second < that.second)
                return true;
            if (this.second > that.second)
                return false;
        }

        if (this.isHundredthUnspecified() || that.isHundredthUnspecified())
            return false;

        return this.hundredth < that.hundredth;
    }

    /**
     * @param that
     *            The time with which to compare this
     * @return true if this > that
     */
    public boolean after(final Time that) {
        if (!this.isHourUnspecified() && !that.isHourUnspecified()) {
            if (this.hour > that.hour)
                return true;
            if (this.hour < that.hour)
                return false;
        }

        if (!this.isMinuteUnspecified() && !that.isMinuteUnspecified()) {
            if (this.minute > that.minute)
                return true;
            if (this.minute < that.minute)
                return false;
        }

        if (!this.isSecondUnspecified() && !that.isSecondUnspecified()) {
            if (this.second > that.second)
                return true;
            if (this.second < that.second)
                return false;
        }

        if (this.isHundredthUnspecified() || that.isHundredthUnspecified())
            return true;

        return this.hundredth > that.hundredth;
    }

    //
    // Reading and writing
    //
    public Time(final ByteQueue queue) throws BACnetErrorException {
        readTag(queue, TYPE_ID);
        hour = queue.popU1B();
        minute = queue.popU1B();
        second = queue.popU1B();
        hundredth = queue.popU1B();
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        queue.push((byte) hour);
        queue.push((byte) minute);
        queue.push((byte) second);
        queue.push((byte) hundredth);
    }

    @Override
    protected long getLength() {
        return 4;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + hour;
        result = PRIME * result + hundredth;
        result = PRIME * result + minute;
        result = PRIME * result + second;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Time other = (Time) obj;
        if (hour != other.hour)
            return false;
        if (hundredth != other.hundredth)
            return false;
        if (minute != other.minute)
            return false;
        if (second != other.second)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return Integer.toString(hour) + ":" + Integer.toString(minute) + ":" + Integer.toString(second) + "."
                + hundredth;

    }
}
