
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.AmbiguousValue;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class CommandFailureNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 3;

    private final Encodable commandValue;
    private final StatusFlags statusFlags;
    private final Encodable feedbackValue;

    public CommandFailureNotif(final Encodable commandValue, final StatusFlags statusFlags,
            final Encodable feedbackValue) {
        this.commandValue = commandValue;
        this.statusFlags = statusFlags;
        this.feedbackValue = feedbackValue;
    }

    @Override
    public void write(final ByteQueue queue) {
        writeANY(queue, commandValue, 0);
        write(queue, statusFlags, 1);
        writeANY(queue, feedbackValue, 2);
    }

    public CommandFailureNotif(final ByteQueue queue) throws BACnetException {
        commandValue = new AmbiguousValue(queue, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
        feedbackValue = new AmbiguousValue(queue, 2);
    }

    public Encodable getCommandValue() {
        return commandValue;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public Encodable getFeedbackValue() {
        return feedbackValue;
    }

    @Override
    public String toString() {
        return "CommandFailureNotif [commandValue=" + commandValue + ", statusFlags=" + statusFlags + ", feedbackValue="
                + feedbackValue + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (commandValue == null ? 0 : commandValue.hashCode());
        result = PRIME * result + (feedbackValue == null ? 0 : feedbackValue.hashCode());
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
        final CommandFailureNotif other = (CommandFailureNotif) obj;
        if (commandValue == null) {
            if (other.commandValue != null)
                return false;
        } else if (!commandValue.equals(other.commandValue))
            return false;
        if (feedbackValue == null) {
            if (other.feedbackValue != null)
                return false;
        } else if (!feedbackValue.equals(other.feedbackValue))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
