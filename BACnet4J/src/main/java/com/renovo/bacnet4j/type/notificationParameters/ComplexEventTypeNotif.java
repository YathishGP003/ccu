
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.util.sero.ByteQueue;

@SuppressWarnings("unchecked")
public class ComplexEventTypeNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 6;

    private final SequenceOf<PropertyValue> values;

    public ComplexEventTypeNotif(final SequenceOf<PropertyValue> values) {
        this.values = values;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, values);
    }

    public ComplexEventTypeNotif(final ByteQueue queue) throws BACnetException {
        values = new SequenceOf(queue, PropertyValue.class, 6);
    }

    public SequenceOf<PropertyValue> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "ComplexEventTypeNotif[ values=" + values + ']';
    }
    
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (values == null ? 0 : values.hashCode());
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
        final ComplexEventTypeNotif other = (ComplexEventTypeNotif) obj;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }
}
