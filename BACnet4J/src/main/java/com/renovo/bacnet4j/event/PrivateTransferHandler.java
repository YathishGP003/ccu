package com.renovo.bacnet4j.event;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.EncodedValue;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public interface PrivateTransferHandler {
    Encodable handle(final LocalDevice localDevice, final Address from, final UnsignedInteger vendorId,
            final UnsignedInteger serviceNumber, final EncodedValue serviceParameters, boolean confirmed)
            throws BACnetErrorException;
}
