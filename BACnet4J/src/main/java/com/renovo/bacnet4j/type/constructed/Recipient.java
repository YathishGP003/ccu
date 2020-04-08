
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class Recipient extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, ObjectIdentifier.class);
        choiceOptions.addContextual(1, Address.class);
    }

    private final Choice choice;

    public Recipient(final ObjectIdentifier device) {
        choice = new Choice(0, device, choiceOptions);
    }

    public Recipient(final Address address) {
        choice = new Choice(1, address, choiceOptions);
    }

    public boolean isDevice() {
        return choice.isa(ObjectIdentifier.class);
    }

    public ObjectIdentifier getDevice() {
        return choice.getDatum();
    }

    public boolean isAddress() {
        return choice.isa(Address.class);
    }

    public Address getAddress() {
        return choice.getDatum();
    }

    public <T extends Encodable> T getValue() {
        return choice.getDatum();
    }
    
    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public Recipient(final ByteQueue queue) throws BACnetException {
        choice = new Choice(queue, choiceOptions);
    }

    public Address toAddress(final LocalDevice localDevice) throws BACnetException {
        if (isAddress())
            return getAddress();

        final int deviceId = getDevice().getInstanceNumber();
        final RemoteDevice rd = localDevice.getRemoteDevice(deviceId).get();
        return rd.getAddress();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (choice == null ? 0 : choice.hashCode());
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
        final Recipient other = (Recipient) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Recipient [choice=" + choice + "]";
    }
}
