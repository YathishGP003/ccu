
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class DailySchedule extends BaseType {
    private final SequenceOf<TimeValue> daySchedule;

    public DailySchedule(final SequenceOf<TimeValue> daySchedule) {
        this.daySchedule = daySchedule;
    }

    public SequenceOf<TimeValue> getDaySchedule() {
        return daySchedule;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, daySchedule, 0);
    }

    public DailySchedule(final ByteQueue queue) throws BACnetException {
        daySchedule = readSequenceOf(queue, TimeValue.class, 0);
    }

    @Override
    public String toString() {
        return "DailySchedule [daySchedule=" + daySchedule + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (daySchedule == null ? 0 : daySchedule.hashCode());
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
        final DailySchedule other = (DailySchedule) obj;
        if (daySchedule == null) {
            if (other.daySchedule != null)
                return false;
        } else if (!daySchedule.equals(other.daySchedule))
            return false;
        return true;
    }
}
