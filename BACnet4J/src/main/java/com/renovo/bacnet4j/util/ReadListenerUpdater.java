
package com.renovo.bacnet4j.util;

import java.util.concurrent.atomic.AtomicInteger;

import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class ReadListenerUpdater {
    private final ReadListener callback;
    private final PropertyValues propertyValues;
    private final int max;
    private final AtomicInteger current = new AtomicInteger(0);
    private boolean cancelled;

    public ReadListenerUpdater(final ReadListener callback, final PropertyValues propertyValues, final int max) {
        this.callback = callback;
        this.propertyValues = propertyValues;
        this.max = max;
    }

    public void increment(final int deviceId, final ObjectIdentifier oid, final PropertyIdentifier pid,
            final UnsignedInteger pin, final Encodable value) {
        final int cur = current.incrementAndGet();
        if (callback != null)
            cancelled = callback.progress((double) cur / max, deviceId, oid, pid, pin, value);
        propertyValues.add(oid, pid, pin, value);
    }

    public boolean cancelled() {
        return cancelled;
    }
}
