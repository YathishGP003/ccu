
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ValueSource extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, Null.class);
        choiceOptions.addContextual(1, DeviceObjectReference.class);
        choiceOptions.addContextual(2, Address.class);
    }

    private final Choice choice;

    public ValueSource() {
        choice = new Choice(0, Null.instance, choiceOptions);
    }

    public ValueSource(final DeviceObjectReference object) {
        choice = new Choice(1, object, choiceOptions);
    }

    public ValueSource(final Address address) {
        choice = new Choice(2, address, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public ValueSource(final ByteQueue queue) throws BACnetException {
        choice = new Choice(queue, choiceOptions);
    }

    public boolean isNone() {
        return choice.isa(Null.class);
    }

    public Null getNone() {
        return (Null) choice.getDatum();
    }

    public boolean isObject() {
        return choice.isa(DeviceObjectReference.class);
    }

    public DeviceObjectReference getObject() {
        return (DeviceObjectReference) choice.getDatum();
    }

    public boolean isAddress() {
        return choice.isa(Address.class);
    }

    public Address getAddress() {
        return (Address) choice.getDatum();
    }

    @Override
    public String toString() {
        return "ValueSource [choice=" + choice + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (choice == null ? 0 : choice.hashCode());
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
        final ValueSource other = (ValueSource) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }
}
