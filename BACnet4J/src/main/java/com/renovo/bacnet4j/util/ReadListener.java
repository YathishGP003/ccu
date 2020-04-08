
package com.renovo.bacnet4j.util;

import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

@FunctionalInterface
public interface ReadListener {
    /**
     * Provides the current progress from 0 (which is a value that is never actually sent to the method) up to 1
     * (finished and will not called again) as well as an opportunity for the client to cancel the request. Other
     * parameters represent the property that was just received.
     *
     * @param progress
     *            the current progress amount
     * @param deviceId
     *            the id of the device from which the property was received
     * @param oid
     *            the oid of the property that was received
     * @param pid
     *            the property id of the property that was received
     * @param pin
     *            the index of the property that was received
     * @param value
     *            the value of the property that was received
     * @return true if the request should be cancelled.
     */
    boolean progress(double progress, int deviceId, ObjectIdentifier oid, PropertyIdentifier pid,
            UnsignedInteger pin, Encodable value);
}
