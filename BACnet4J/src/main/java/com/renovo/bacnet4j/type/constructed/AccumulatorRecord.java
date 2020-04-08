
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AccumulatorRecord extends BaseType {
    private final DateTime timestamp;
    private final UnsignedInteger presentValue;
    private final UnsignedInteger accumulatedValue;
    private final AccumulatorStatus accumulatorStatus;

    public AccumulatorRecord(final DateTime timestamp, final UnsignedInteger presentValue,
            final UnsignedInteger accumulatedValue, final AccumulatorStatus accumulatorStatus) {
        this.timestamp = timestamp;
        this.presentValue = presentValue;
        this.accumulatedValue = accumulatedValue;
        this.accumulatorStatus = accumulatorStatus;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timestamp, 0);
        write(queue, presentValue, 1);
        write(queue, accumulatedValue, 2);
        write(queue, accumulatorStatus, 3);
    }

    public AccumulatorRecord(final ByteQueue queue) throws BACnetException {
        timestamp = read(queue, DateTime.class, 0);
        presentValue = read(queue, UnsignedInteger.class, 1);
        accumulatedValue = read(queue, UnsignedInteger.class, 2);
        accumulatorStatus = read(queue, AccumulatorStatus.class, 3);
    }

    public static class AccumulatorStatus extends Enumerated {
        public static final AccumulatorStatus normal = new AccumulatorStatus(0);
        public static final AccumulatorStatus starting = new AccumulatorStatus(1);
        public static final AccumulatorStatus recovered = new AccumulatorStatus(2);
        public static final AccumulatorStatus abnormal = new AccumulatorStatus(3);
        public static final AccumulatorStatus failed = new AccumulatorStatus(4);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

        public static AccumulatorStatus forId(final int id) {
            AccumulatorStatus e = (AccumulatorStatus) idMap.get(id);
            if (e == null)
                e = new AccumulatorStatus(id);
            return e;
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static AccumulatorStatus forName(final String name) {
            return (AccumulatorStatus) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private AccumulatorStatus(final int value) {
            super(value);
        }

        public AccumulatorStatus(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public UnsignedInteger getPresentValue() {
        return presentValue;
    }

    public UnsignedInteger getAccumulatedValue() {
        return accumulatedValue;
    }

    public AccumulatorStatus getAccumulatorStatus() {
        return accumulatorStatus;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (accumulatedValue == null ? 0 : accumulatedValue.hashCode());
        result = PRIME * result + (accumulatorStatus == null ? 0 : accumulatorStatus.hashCode());
        result = PRIME * result + (presentValue == null ? 0 : presentValue.hashCode());
        result = PRIME * result + (timestamp == null ? 0 : timestamp.hashCode());
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
        final AccumulatorRecord other = (AccumulatorRecord) obj;
        if (accumulatedValue == null) {
            if (other.accumulatedValue != null)
                return false;
        } else if (!accumulatedValue.equals(other.accumulatedValue))
            return false;
        if (accumulatorStatus == null) {
            if (other.accumulatorStatus != null)
                return false;
        } else if (!accumulatorStatus.equals(other.accumulatorStatus))
            return false;
        if (presentValue == null) {
            if (other.presentValue != null)
                return false;
        } else if (!presentValue.equals(other.presentValue))
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
        return "AccumulatorRecord [timestamp=" + timestamp + ", presentValue=" + presentValue + ", accumulatedValue=" + accumulatedValue + ", accumulatorStatus=" + accumulatorStatus + ']';
    }
}
