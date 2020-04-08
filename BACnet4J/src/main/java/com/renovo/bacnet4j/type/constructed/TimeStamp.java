
package com.renovo.bacnet4j.type.constructed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Time;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class TimeStamp extends BaseType {
    static final Logger LOG = LoggerFactory.getLogger(TimeStamp.class);

    private static final ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, Time.class);
        choiceOptions.addContextual(1, UnsignedInteger.class);
        choiceOptions.addContextual(2, DateTime.class);
    }

    public static final TimeStamp UNSPECIFIED_TIME = new TimeStamp(Time.UNSPECIFIED);
    public static final TimeStamp UNSPECIFIED_SEQUENCE = new TimeStamp(UnsignedInteger.ZERO);
    public static final TimeStamp UNSPECIFIED_DATETIME = new TimeStamp(DateTime.UNSPECIFIED);

    private final Choice choice;

    public TimeStamp(final Time time) {
        choice = new Choice(0, time, choiceOptions);
    }

    public TimeStamp(final UnsignedInteger sequenceNumber) {
        choice = new Choice(1, sequenceNumber, choiceOptions);
    }

    public TimeStamp(final DateTime dateTime) {
        choice = new Choice(2, dateTime, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public TimeStamp(final ByteQueue queue) throws BACnetException {
        choice = new Choice(queue, choiceOptions);
    }

    public boolean isTime() {
        return choice.isa(Time.class);
    }

    public Time getTime() {
        return choice.getDatum();
    }

    public boolean isSequenceNumber() {
        return choice.isa(UnsignedInteger.class);
    }

    public UnsignedInteger getSequenceNumber() {
        return choice.getDatum();
    }

    public boolean isDateTime() {
        return choice.isa(DateTime.class);
    }

    public DateTime getDateTime() {
        return choice.getDatum();
    }

    @Override
    public String toString() {
        return "TimeStamp [choice=" + choice + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (choice == null ? 0 : choice.hashCode());
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
        final TimeStamp other = (TimeStamp) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

}
