
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.primitive.BitString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class LogStatus extends BitString {
    public LogStatus(final boolean logDisabled, final boolean bufferPurged, final boolean logInterrupted) {
        super(new boolean[] { logDisabled, bufferPurged, logInterrupted });
    }

    public LogStatus(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public boolean isLogDisabled() {
        return getValue()[0];
    }

    public boolean isBufferPurged() {
        return getValue()[1];
    }

    public boolean isLogInterrupted() {
        return getValue()[2];
    }

    @Override
    public String toString() {
        return "LogStatus [log-disabled=" + isLogDisabled() + ", buffer-purged=" + isBufferPurged() + ", log-interrupted=" + isLogInterrupted() + "]";
    }   
}
