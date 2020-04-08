
package com.renovo.bacnet4j.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ExceptionDispatcher {
    private final List<ExceptionListener> listeners = new CopyOnWriteArrayList<>();
    private final ExceptionListener defaultExceptionListener = new DefaultExceptionListener();

    public ExceptionDispatcher() {
        listeners.add(defaultExceptionListener);
    }

    public void addListener(final ExceptionListener l) {
        listeners.add(l);
    }

    public void removeListener(final ExceptionListener l) {
        listeners.remove(l);
    }

    public void removeDefaultExceptionListener() {
        listeners.remove(defaultExceptionListener);
    }

    public void fireUnimplementedVendorService(final UnsignedInteger vendorId, final UnsignedInteger serviceNumber,
            final ByteQueue queue) {
        for (final ExceptionListener l : listeners)
            l.unimplementedVendorService(vendorId, serviceNumber, queue);
    }

    public void fireReceivedException(final Exception e) {
        for (final ExceptionListener l : listeners)
            l.receivedException(e);
    }

    public void fireReceivedThrowable(final Throwable t) {
        for (final ExceptionListener l : listeners)
            l.receivedThrowable(t);
    }
}
