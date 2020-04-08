
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.logBuffer.ILogRecord;
import com.renovo.bacnet4j.service.confirmed.ConfirmedEventNotificationRequest;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.util.sero.ByteQueue;

/**
 * @author Suresh Kumar
 */
public class EventLogRecord extends BaseType implements ILogRecord {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, LogStatus.class);
        choiceOptions.addContextual(1, ConfirmedEventNotificationRequest.class);
        choiceOptions.addContextual(2, Real.class);
    }

    private final DateTime timestamp;
    private final Choice choice;

    private long sequenceNumber;

    public EventLogRecord(final DateTime timestamp, final LogStatus logStatus) {
        this.timestamp = timestamp;
        choice = new Choice(0, logStatus, choiceOptions);
    }

    public EventLogRecord(final DateTime timestamp, final ConfirmedEventNotificationRequest notification) {
        this.timestamp = timestamp;
        choice = new Choice(1, notification, choiceOptions);
    }

    public EventLogRecord(final DateTime timestamp, final Real timeChange) {
        this.timestamp = timestamp;
        choice = new Choice(2, timeChange, choiceOptions);
    }

    public EventLogRecord(final ByteQueue queue) throws BACnetException {
        timestamp = read(queue, DateTime.class, 0);
        choice = new Choice(queue, choiceOptions, 1);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timestamp, 0);
        write(queue, choice, 1);
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp;
    }

    public boolean isLogStatus() {
        return choice.getContextId() == 0;
    }

    public LogStatus getLogStatus() {
        return choice.getDatum();
    }

    public boolean isNotification() {
        return choice.getContextId() == 1;
    }

    public ConfirmedEventNotificationRequest getNotification() {
        return choice.getDatum();
    }

    public boolean isTimeChange() {
        return choice.getContextId() == 2;
    }

    public Real getTimeChange() {
        return choice.getDatum();
    }

    public Choice getChoice() {
        return choice;
    }

    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(final long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (choice == null ? 0 : choice.hashCode());
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
        final EventLogRecord other = (EventLogRecord) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        if (timestamp == null) {
            if (other.timestamp != null)
                return false;
        } else if (!timestamp.equals(other.timestamp))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "EventLogRecord [timestamp=" + timestamp + ", choice=" + choice + ", sequenceNumber=" + sequenceNumber + ']';
    }    
}
