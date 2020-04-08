
package com.renovo.bacnet4j.type.primitive;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import java.math.BigInteger;

import com.renovo.bacnet4j.util.sero.ByteQueue;

public class Unsigned32 extends UnsignedInteger {
    private static final long MAX = 0xffffffffl;
    private static final BigInteger BIGMAX = BigInteger.valueOf(MAX);

    public Unsigned32(final int value) {
        super(value);
    }

    public Unsigned32(final BigInteger value) {
        super(value);
        if (value.longValue() > MAX)
            throw new IllegalArgumentException("Value cannot be greater than " + MAX);
    }

    public Unsigned32(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
        if (super.isSmallValue()) {
            if (super.intValue() > MAX) {
                throw new BACnetErrorException(ErrorClass.property, ErrorCode.valueOutOfRange);
            }
        } else {
            if (super.bigIntegerValue().compareTo(BIGMAX) > 0) {
                throw new BACnetErrorException(ErrorClass.property, ErrorCode.valueOutOfRange);
            }
        }
    }
}
