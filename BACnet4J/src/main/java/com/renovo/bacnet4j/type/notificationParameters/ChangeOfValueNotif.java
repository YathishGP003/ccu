
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.Choice;
import com.renovo.bacnet4j.type.constructed.ChoiceOptions;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.primitive.BitString;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ChangeOfValueNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 2;

    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, BitString.class);
        choiceOptions.addContextual(1, Real.class);
    }

    private final Choice newValue;
    private final StatusFlags statusFlags;

    public ChangeOfValueNotif(final BitString newValue, final StatusFlags statusFlags) {
        this.newValue = new Choice(0, newValue, choiceOptions);
        this.statusFlags = statusFlags;
    }

    public ChangeOfValueNotif(final Real newValue, final StatusFlags statusFlags) {
        this.newValue = new Choice(1, newValue, choiceOptions);
        this.statusFlags = statusFlags;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, newValue, 0);
        write(queue, statusFlags, 1);
    }

    public ChangeOfValueNotif(final ByteQueue queue) throws BACnetException {
        newValue = new Choice(queue, choiceOptions, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
    }

    public Choice getNewValue() {
        return newValue;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    @Override
    public String toString() {
        return "ChangeOfValueNotif[ newValue=" + newValue + ", statusFlags=" + statusFlags + ']';
    }
    
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (newValue == null ? 0 : newValue.hashCode());
        result = PRIME * result + (statusFlags == null ? 0 : statusFlags.hashCode());
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
        final ChangeOfValueNotif other = (ChangeOfValueNotif) obj;
        if (newValue == null) {
            if (other.newValue != null)
                return false;
        } else if (!newValue.equals(other.newValue))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
