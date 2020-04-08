
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.primitive.BitString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class LimitEnable extends BitString {
    public LimitEnable(final boolean lowLimitEnable, final boolean highLimitEnable) {
        super(new boolean[] { lowLimitEnable, highLimitEnable });
    }

    public LimitEnable(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public boolean isLowLimitEnable() {
        return getValue()[0];
    }

    public boolean isHighLimitEnable() {
        return getValue()[1];
    }
    
    @Override
    public String toString() {
        return "LimitEnable [low-limit-enable=" + isLowLimitEnable() + ", high-limit-enable=" + isHighLimitEnable() + "]";
    }
}
