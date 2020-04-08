
package com.renovo.bacnet4j.type.primitive;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.math.BigInteger;

public class Unsigned16 extends UnsignedInteger {
    private static final int MAX = 0xffff;
    private static final BigInteger BIGMAX = BigInteger.valueOf(MAX);

    public Unsigned16(final int value) {
        super(value);
        if (value > MAX)
            throw new IllegalArgumentException("Value cannot be greater than " + MAX);
    }

    public Unsigned16(final ByteQueue queue) throws BACnetErrorException {
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
