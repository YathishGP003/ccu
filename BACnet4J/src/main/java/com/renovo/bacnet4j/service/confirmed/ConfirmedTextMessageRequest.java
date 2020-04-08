
package com.renovo.bacnet4j.service.confirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.npdu.NPCI.NetworkPriority;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.Choice;
import com.renovo.bacnet4j.type.constructed.ChoiceOptions;
import com.renovo.bacnet4j.type.enumerated.MessagePriority;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ConfirmedTextMessageRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 19;

    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, UnsignedInteger.class);
        choiceOptions.addContextual(1, CharacterString.class);
    }

    private final ObjectIdentifier textMessageSourceDevice;
    private Choice messageClass;
    private final MessagePriority messagePriority;
    private final CharacterString message;

    public ConfirmedTextMessageRequest(final ObjectIdentifier textMessageSourceDevice,
            final UnsignedInteger messageClass, final MessagePriority messagePriority, final CharacterString message) {
        this.textMessageSourceDevice = textMessageSourceDevice;
        this.messageClass = new Choice(0, messageClass, choiceOptions);
        this.messagePriority = messagePriority;
        this.message = message;
    }

    public ConfirmedTextMessageRequest(final ObjectIdentifier textMessageSourceDevice,
            final CharacterString messageClass, final MessagePriority messagePriority, final CharacterString message) {
        this.textMessageSourceDevice = textMessageSourceDevice;
        this.messageClass = new Choice(1, messageClass, choiceOptions);
        this.messagePriority = messagePriority;
        this.message = message;
    }

    public ConfirmedTextMessageRequest(final ObjectIdentifier textMessageSourceDevice,
            final MessagePriority messagePriority, final CharacterString message) {
        this.textMessageSourceDevice = textMessageSourceDevice;
        this.messagePriority = messagePriority;
        this.message = message;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public NetworkPriority getNetworkPriority() {
        return messagePriority.getNetworkPriority();
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) {
        localDevice.updateRemoteDevice(textMessageSourceDevice.getInstanceNumber(), from);
        localDevice.getEventHandler().fireTextMessage(textMessageSourceDevice, messageClass, messagePriority, message);
        return null;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, textMessageSourceDevice, 0);
        writeOptional(queue, messageClass, 1);
        write(queue, messagePriority, 2);
        write(queue, message, 3);
    }

    ConfirmedTextMessageRequest(final ByteQueue queue) throws BACnetException {
        textMessageSourceDevice = read(queue, ObjectIdentifier.class, 0);
        messageClass = readOptionalChoice(queue, choiceOptions, 1);
        messagePriority = read(queue, MessagePriority.class, 2);
        message = read(queue, CharacterString.class, 3);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (message == null ? 0 : message.hashCode());
        result = PRIME * result + (messageClass == null ? 0 : messageClass.hashCode());
        result = PRIME * result + (messagePriority == null ? 0 : messagePriority.hashCode());
        result = PRIME * result + (textMessageSourceDevice == null ? 0 : textMessageSourceDevice.hashCode());
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
        final ConfirmedTextMessageRequest other = (ConfirmedTextMessageRequest) obj;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (messageClass == null) {
            if (other.messageClass != null)
                return false;
        } else if (!messageClass.equals(other.messageClass))
            return false;
        if (messagePriority == null) {
            if (other.messagePriority != null)
                return false;
        } else if (!messagePriority.equals(other.messagePriority))
            return false;
        if (textMessageSourceDevice == null) {
            if (other.textMessageSourceDevice != null)
                return false;
        } else if (!textMessageSourceDevice.equals(other.textMessageSourceDevice))
            return false;
        return true;
    }
}
