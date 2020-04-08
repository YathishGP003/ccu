
package com.renovo.bacnet4j.service.confirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.NotImplementedException;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class VtCloseRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 22;

    private final SequenceOf<UnsignedInteger> listOfRemoteVTSessionIdentifiers;

    public VtCloseRequest(final SequenceOf<UnsignedInteger> listOfRemoteVTSessionIdentifiers) {
        this.listOfRemoteVTSessionIdentifiers = listOfRemoteVTSessionIdentifiers;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, listOfRemoteVTSessionIdentifiers);
    }

    VtCloseRequest(final ByteQueue queue) throws BACnetException {
        listOfRemoteVTSessionIdentifiers = readSequenceOf(queue, UnsignedInteger.class);
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        throw new NotImplementedException();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result
                + (listOfRemoteVTSessionIdentifiers == null ? 0 : listOfRemoteVTSessionIdentifiers.hashCode());
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
        final VtCloseRequest other = (VtCloseRequest) obj;
        if (listOfRemoteVTSessionIdentifiers == null) {
            if (other.listOfRemoteVTSessionIdentifiers != null)
                return false;
        } else if (!listOfRemoteVTSessionIdentifiers.equals(other.listOfRemoteVTSessionIdentifiers))
            return false;
        return true;
    }
}
