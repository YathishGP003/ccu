
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class BufferReadyNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 10;

    private final DeviceObjectPropertyReference bufferProperty;
    private final UnsignedInteger previousNotification;
    private final UnsignedInteger currentNotification;

    public BufferReadyNotif(final DeviceObjectPropertyReference bufferProperty,
            final UnsignedInteger previousNotification, final UnsignedInteger currentNotification) {
        this.bufferProperty = bufferProperty;
        this.previousNotification = previousNotification;
        this.currentNotification = currentNotification;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, bufferProperty, 0);
        write(queue, previousNotification, 1);
        write(queue, currentNotification, 2);
    }

    public BufferReadyNotif(final ByteQueue queue) throws BACnetException {
        bufferProperty = read(queue, DeviceObjectPropertyReference.class, 0);
        previousNotification = read(queue, UnsignedInteger.class, 1);
        currentNotification = read(queue, UnsignedInteger.class, 2);
    }

    public DeviceObjectPropertyReference getBufferProperty() {
        return bufferProperty;
    }

    public UnsignedInteger getPreviousNotification() {
        return previousNotification;
    }

    public UnsignedInteger getCurrentNotification() {
        return currentNotification;
    }

    @Override
    public String toString() {
        return "BufferReadyNotif [bufferProperty=" + bufferProperty + ", previousNotification=" + previousNotification
                + ", currentNotification=" + currentNotification + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (bufferProperty == null ? 0 : bufferProperty.hashCode());
        result = PRIME * result + (currentNotification == null ? 0 : currentNotification.hashCode());
        result = PRIME * result + (previousNotification == null ? 0 : previousNotification.hashCode());
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
        final BufferReadyNotif other = (BufferReadyNotif) obj;
        if (bufferProperty == null) {
            if (other.bufferProperty != null)
                return false;
        } else if (!bufferProperty.equals(other.bufferProperty))
            return false;
        if (currentNotification == null) {
            if (other.currentNotification != null)
                return false;
        } else if (!currentNotification.equals(other.currentNotification))
            return false;
        if (previousNotification == null) {
            if (other.previousNotification != null)
                return false;
        } else if (!previousNotification.equals(other.previousNotification))
            return false;
        return true;
    }
}
