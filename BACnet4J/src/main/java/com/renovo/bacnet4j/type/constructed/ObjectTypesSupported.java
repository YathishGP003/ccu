
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.primitive.BitString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ObjectTypesSupported extends BitString {
    public ObjectTypesSupported() {
        super(new boolean[60]);
    }

    public ObjectTypesSupported(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public boolean is(final ObjectType objectType) {
        return getArrayValue(objectType.intValue());
    }

    public void set(final ObjectType objectType, final boolean supported) {
        getValue()[objectType.intValue()] = supported;
    }
}
