
package com.renovo.bacnet4j.service.confirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.service.acknowledgement.GetEventInformationAck;
import com.renovo.bacnet4j.service.acknowledgement.GetEventInformationAck.EventSummary;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class GetEventInformationRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 29;

    private final ObjectIdentifier lastReceivedObjectIdentifier; // Optional

    public GetEventInformationRequest(final ObjectIdentifier lastReceivedObjectIdentifier) {
        this.lastReceivedObjectIdentifier = lastReceivedObjectIdentifier;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final SequenceOf<EventSummary> summaries = new SequenceOf<>();

        for (final BACnetObject bo : localDevice.getLocalObjects()) {
            final EventSummary eventSummary = bo.getEventSummary();
            if (eventSummary != null)
                summaries.add(eventSummary);
        }

        return new GetEventInformationAck(summaries, Boolean.FALSE);
    }

    @Override
    public void write(final ByteQueue queue) {
        writeOptional(queue, lastReceivedObjectIdentifier, 0);
    }

    GetEventInformationRequest(final ByteQueue queue) throws BACnetException {
        lastReceivedObjectIdentifier = readOptional(queue, ObjectIdentifier.class, 0);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (lastReceivedObjectIdentifier == null ? 0 : lastReceivedObjectIdentifier.hashCode());
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
        final GetEventInformationRequest other = (GetEventInformationRequest) obj;
        if (lastReceivedObjectIdentifier == null) {
            if (other.lastReceivedObjectIdentifier != null)
                return false;
        } else if (!lastReceivedObjectIdentifier.equals(other.lastReceivedObjectIdentifier))
            return false;
        return true;
    }
}
