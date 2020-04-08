
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.BufferReadyAlgo;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class BufferReady extends AbstractEventParameter {
    public static final byte TYPE_ID = 10;

    private final UnsignedInteger notificationThreshold;
    private final UnsignedInteger previousNotificationCount;

    // This parameter is stateful.
    private UnsignedInteger mutablePreviousNotificationCount;

    public BufferReady(final UnsignedInteger notificationThreshold, final UnsignedInteger previousNotificationCount) {
        this.notificationThreshold = notificationThreshold;
        this.previousNotificationCount = previousNotificationCount;
        mutablePreviousNotificationCount = previousNotificationCount;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, notificationThreshold, 0);
        write(queue, previousNotificationCount, 1);
    }

    public BufferReady(final ByteQueue queue) throws BACnetException {
        notificationThreshold = read(queue, UnsignedInteger.class, 0);
        previousNotificationCount = read(queue, UnsignedInteger.class, 1);
    }

    public UnsignedInteger getNotificationThreshold() {
        return notificationThreshold;
    }

    public UnsignedInteger getPreviousNotificationCount() {
        return previousNotificationCount;
    }

    public UnsignedInteger getMutablePreviousNotificationCount() {
        return mutablePreviousNotificationCount;
    }

    @Override
    public void postNotification(final NotificationParameters notifParams) {
        // The previous notification count has to be updated following a notification.
        final BufferReadyNotif brn = (BufferReadyNotif) notifParams.getParameter();
        mutablePreviousNotificationCount = brn.getCurrentNotification();
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return new BufferReadyAlgo();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (notificationThreshold == null ? 0 : notificationThreshold.hashCode());
        result = PRIME * result + (previousNotificationCount == null ? 0 : previousNotificationCount.hashCode());
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
        final BufferReady other = (BufferReady) obj;
        if (notificationThreshold == null) {
            if (other.notificationThreshold != null)
                return false;
        } else if (!notificationThreshold.equals(other.notificationThreshold))
            return false;
        if (previousNotificationCount == null) {
            if (other.previousNotificationCount != null)
                return false;
        } else if (!previousNotificationCount.equals(other.previousNotificationCount))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BufferReady[ notificationThreshold=" + notificationThreshold + ", previousNotificationCount=" + previousNotificationCount + ", mutablePreviousNotificationCount=" + mutablePreviousNotificationCount + ']';
    }    
}
