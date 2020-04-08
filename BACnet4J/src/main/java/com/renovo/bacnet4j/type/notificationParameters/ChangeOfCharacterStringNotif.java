
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ChangeOfCharacterStringNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 17;

    private final CharacterString changedValue;
    private final StatusFlags statusFlags;
    private final CharacterString alarmValues;

    public ChangeOfCharacterStringNotif(final CharacterString changedValue, final StatusFlags statusFlags,
            final CharacterString alarmValues) {
        this.changedValue = changedValue;
        this.statusFlags = statusFlags;
        this.alarmValues = alarmValues;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, changedValue, 0);
        write(queue, statusFlags, 1);
        write(queue, alarmValues, 2);
    }

    public ChangeOfCharacterStringNotif(final ByteQueue queue) throws BACnetException {
        changedValue = read(queue, CharacterString.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
        alarmValues = read(queue, CharacterString.class, 2);
    }

    public CharacterString getChangedValue() {
        return changedValue;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public CharacterString getAlarmValues() {
        return alarmValues;
    }

    @Override
    public String toString() {
        return "ChangeOfCharacterStringNotif[ changedValue=" + changedValue + ", statusFlags=" + statusFlags + ", alarmValues=" + alarmValues + ']';
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (alarmValues == null ? 0 : alarmValues.hashCode());
        result = prime * result + (changedValue == null ? 0 : changedValue.hashCode());
        result = prime * result + (statusFlags == null ? 0 : statusFlags.hashCode());
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
        final ChangeOfCharacterStringNotif other = (ChangeOfCharacterStringNotif) obj;
        if (alarmValues == null) {
            if (other.alarmValues != null)
                return false;
        } else if (!alarmValues.equals(other.alarmValues))
            return false;
        if (changedValue == null) {
            if (other.changedValue != null)
                return false;
        } else if (!changedValue.equals(other.changedValue))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
