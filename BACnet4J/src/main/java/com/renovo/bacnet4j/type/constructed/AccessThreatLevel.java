
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class AccessThreatLevel extends UnsignedInteger {
    public AccessThreatLevel(final int value) {
        super(value);
        if (value < 0 || value > 100)
            throw new IllegalArgumentException("value must be between 0 and 100 inclusive. Given " + value);
    }
}
