
package com.renovo.bacnet4j.apdu;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.IllegalPduTypeException;
import com.renovo.bacnet4j.npdu.NPCI.NetworkPriority;
import com.renovo.bacnet4j.type.constructed.ServicesSupported;
import com.renovo.bacnet4j.util.sero.ByteQueue;

abstract public class APDU {
    public static APDU createAPDU(final ServicesSupported services, final ByteQueue queue) throws BACnetException {
        // Get the first byte. The 4 high-order bits will tell us the type of PDU this is.
        byte type = queue.peek(0);
        type = (byte) ((type & 0xff) >> 4);

        if (type == ConfirmedRequest.TYPE_ID)
            return new ConfirmedRequest(queue);
        if (type == UnconfirmedRequest.TYPE_ID)
            return new UnconfirmedRequest(services, queue);
        if (type == SimpleACK.TYPE_ID)
            return new SimpleACK(queue);
        if (type == ComplexACK.TYPE_ID)
            return new ComplexACK(queue);
        if (type == SegmentACK.TYPE_ID)
            return new SegmentACK(queue);
        if (type == Error.TYPE_ID)
            return new Error(queue);
        if (type == Reject.TYPE_ID)
            return new Reject(queue);
        if (type == Abort.TYPE_ID)
            return new Abort(queue);
        throw new IllegalPduTypeException(Byte.toString(type));
    }

    abstract public byte getPduType();

    abstract public void write(ByteQueue queue);

    protected int getShiftedTypeId(final byte typeId) {
        return typeId << 4;
    }

    abstract public boolean expectsReply();

    public NetworkPriority getNetworkPriority() {
        return null;
    }
}
