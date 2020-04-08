
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.enums.DayOfWeek;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.primitive.BitString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class DaysOfWeek extends BitString {
    public DaysOfWeek() {
        super(new boolean[7]);
    }

    public DaysOfWeek(final boolean defaultValue) {
        super(7, defaultValue);
    }

    public DaysOfWeek(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public boolean contains(final DayOfWeek dayOfWeek) {
        return getValue()[dayOfWeek.getId() - 1];
    }

    public boolean contains(final int day) {
        return getValue()[day];
    }

    public boolean isMonday() {
        return getValue()[0];
    }

    public void setMonday(final boolean monday) {
        getValue()[0] = monday;
    }

    public boolean isTuesday() {
        return getValue()[1];
    }

    public void setTuesday(final boolean tuesday) {
        getValue()[1] = tuesday;
    }

    public boolean isWednesday() {
        return getValue()[2];
    }

    public void setWednesday(final boolean wednesday) {
        getValue()[2] = wednesday;
    }

    public boolean isThursday() {
        return getValue()[3];
    }

    public void setThursday(final boolean thursday) {
        getValue()[3] = thursday;
    }

    public boolean isFriday() {
        return getValue()[4];
    }

    public void setFriday(final boolean friday) {
        getValue()[4] = friday;
    }

    public boolean isSaturday() {
        return getValue()[5];
    }

    public void setSaturday(final boolean saturday) {
        getValue()[5] = saturday;
    }

    public boolean isSunday() {
        return getValue()[6];
    }

    public void setSunday(final boolean sunday) {
        getValue()[6] = sunday;
    }
    
    @Override
    public String toString() {
        return "DaysOfWeek [monday=" + isMonday() + ", tuesday=" + isTuesday()+ ", wednesday=" + isWednesday()+ ", thursday=" + isThursday()+ ", friday=" + isFriday() + ", saturday=" + isSaturday()+ ", sunday=" + isSunday()+ "]";
    }    
}
