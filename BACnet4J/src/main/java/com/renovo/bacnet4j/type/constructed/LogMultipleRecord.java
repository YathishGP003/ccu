
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.logBuffer.ILogRecord;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class LogMultipleRecord extends BaseType implements ILogRecord {
    private final DateTime timestamp;
    private final LogData logData;

    private long sequenceNumber;

    public LogMultipleRecord(final DateTime timestamp, final LogData logData) {
        this.timestamp = timestamp;
        this.logData = logData;
    }

    public LogMultipleRecord(final ByteQueue queue) throws BACnetException {
        timestamp = read(queue, DateTime.class, 0);
        logData = read(queue, LogData.class, 1);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timestamp, 0);
        write(queue, logData, 1);
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp;
    }

    public LogData getLogData() {
        return logData;
    }

    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(final long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public String toString() {
        return "LogMultipleRecord [timestamp=" + timestamp + ", logData=" + logData + ", sequenceNumber="
                + sequenceNumber + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (logData == null ? 0 : logData.hashCode());
        result = prime * result + (timestamp == null ? 0 : timestamp.hashCode());
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
        final LogMultipleRecord other = (LogMultipleRecord) obj;
        if (logData == null) {
            if (other.logData != null)
                return false;
        } else if (!logData.equals(other.logData))
            return false;
        if (timestamp == null) {
            if (other.timestamp != null)
                return false;
        } else if (!timestamp.equals(other.timestamp))
            return false;
        return true;
    }
}
