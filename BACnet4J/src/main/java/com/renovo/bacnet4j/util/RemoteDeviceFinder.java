
package com.renovo.bacnet4j.util;

import org.threeten.bp.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.event.DeviceEventAdapter;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetTimeoutException;
import com.renovo.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ThreadUtils;

/**
 * A utility for finding a specific device by id. Generally this should not be used directly. It is better to use the
 * LocalDevice.getRemoteDevice method - which in turn uses this utility - which will also add the remote device to the
 * local cache. Use this if you specifically do *not* want the remote device to be cached.
 */
public class RemoteDeviceFinder {
    static final Logger LOG = LoggerFactory.getLogger(RemoteDeviceFinder.class);

    public static void findDevice(final LocalDevice localDevice, final int instanceId,
            final Consumer<RemoteDevice> callback, final Runnable timeoutCallback, final Runnable finallyCallback,
            final long timeout, final TimeUnit unit) {
        Objects.requireNonNull(localDevice);
        Objects.requireNonNull(callback);
        Objects.requireNonNull(unit);

        final DeviceEventAdapter listener = new DeviceEventAdapter() {
            boolean done = false;
            ScheduledFuture<?> timeoutFuture;

            {
                // Schedule a timeout that will
                timeoutFuture = localDevice.schedule(() -> {
                    synchronized (this) {
                        if (!done) {
                            // Timeout occurred. Cancel the listener and notify the timeout callback.
                            done = true;
                            localDevice.getEventHandler().removeListener(this);
                            if (timeoutCallback != null)
                                timeoutCallback.run();
                            if (finallyCallback != null)
                                finallyCallback.run();
                        }
                    }
                }, timeout, unit);
            }

            @Override
            public void iAmReceived(final RemoteDevice remoteDevice) {
                if (remoteDevice.getInstanceNumber() == instanceId) {
                    synchronized (this) {
                        if (!done) {
                            // Found the device. Cancel the timeout future and notify the callback.
                            done = true;
                            localDevice.getEventHandler().removeListener(this);
                            timeoutFuture.cancel(false);
                            callback.accept(remoteDevice);
                            if (finallyCallback != null)
                                finallyCallback.run();
                        }
                    }
                }
            }
        };

        // Register as an event listener
        localDevice.getEventHandler().addListener(listener);

        // Send a WhoIs with the device id.
        localDevice.sendGlobalBroadcast(
                new WhoIsRequest(new UnsignedInteger(instanceId), new UnsignedInteger(instanceId)));
    }

    public static RemoteDeviceFuture findDevice(final LocalDevice localDevice, final int instanceId) {
        return new DeviceFutureImpl(localDevice, instanceId);
    }

    public interface RemoteDeviceFuture {
        static final long DEFAULT_TIMEOUT = Duration.ofSeconds(10).toMillis();

        default RemoteDevice get() throws BACnetException, CancellationException {
            return get(DEFAULT_TIMEOUT);
        }

        RemoteDevice get(long timeoutMillis) throws BACnetException, CancellationException;

        void cancel();
    }

    static class DeviceFutureImpl implements RemoteDeviceFuture {
        private final LocalDevice localDevice;
        private final int instanceId;

        private final DeviceEventAdapter listener;
        private RemoteDevice remoteDevice;
        private volatile boolean cancelled;

        public DeviceFutureImpl(final LocalDevice localDevice, final int instanceId) {
            this.localDevice = localDevice;
            this.instanceId = instanceId;

            // Register as an event listener
            listener = new DeviceEventAdapter() {
                @Override
                public void iAmReceived(final RemoteDevice remoteDevice) {
                    if (remoteDevice.getInstanceNumber() == instanceId) {
                        LOG.debug("Found device {}", instanceId);
                        setRemoteDevice(remoteDevice);
                    }
                }
            };

            localDevice.getEventHandler().addListener(listener);

            // Send a WhoIs with the device id.
            localDevice.sendGlobalBroadcast(
                    new WhoIsRequest(new UnsignedInteger(instanceId), new UnsignedInteger(instanceId)));
        }

        @Override
        public RemoteDevice get(final long timeoutMillis) throws BACnetException, CancellationException {
            synchronized (this) {
                if (cancelled)
                    throw new CancellationException();
                if (remoteDevice != null)
                    return remoteDevice;

                LOG.debug("Waiting {} ms for something to happen", timeoutMillis);
                ThreadUtils.wait(this, timeoutMillis);
                LOG.debug("Done waiting");

                if (cancelled) {
                    LOG.debug("Future was cancelled");
                    throw new CancellationException();
                }
                if (remoteDevice != null) {
                    LOG.debug("Remote device was found");
                    return remoteDevice;
                }

                // done() was not call, so ensure that the listener is removed from the event handler
                localDevice.getEventHandler().removeListener(listener);

                LOG.debug("Throwing timeout");
                throw new BACnetTimeoutException("No response from instanceId " + instanceId);
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            done();
        }

        private void setRemoteDevice(final RemoteDevice remoteDevice) {
            this.remoteDevice = remoteDevice;
            done();
        }

        private void done() {
            localDevice.getEventHandler().removeListener(listener);
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
