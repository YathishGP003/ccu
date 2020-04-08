
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Primitive;
import com.renovo.bacnet4j.type.primitive.Time;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class TimeValue extends BaseType {
    private final Time time;
    private final Primitive value;

    public TimeValue(final Time time, final Primitive value) {
        this.time = time;
        this.value = value;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, time);
        write(queue, value);
    }

    public TimeValue(final ByteQueue queue) throws BACnetException {
        time = read(queue, Time.class);
        value = read(queue, Primitive.class);
    }

    public Time getTime() {
        return time;
    }

    public Primitive getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (time == null ? 0 : time.hashCode());
        result = PRIME * result + (value == null ? 0 : value.hashCode());
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
        final TimeValue other = (TimeValue) obj;
        if (time == null) {
            if (other.time != null)
                return false;
        } else if (!time.equals(other.time))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TimeValue [time=" + time + ", value=" + value + "]";
    }
}
