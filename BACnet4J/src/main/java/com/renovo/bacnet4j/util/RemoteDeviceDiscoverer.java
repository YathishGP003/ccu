
package com.renovo.bacnet4j.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.event.DeviceEventAdapter;
import com.renovo.bacnet4j.service.unconfirmed.WhoIsRequest;

/**
 * A utility for finding devices on the network. Uses a callback to notify the client of found devices. Does not
 * provide duplicates even if multiple IAms are received from the same device. This utility does not time out; it
 * must be explicitly started and stopped by the client.
 *
 * If a callback is not provided - or even if it is - the getRemoteDevices method can be used to get all devices
 * discovered so far in a batch.
 *
 * @author Matthew
 */
public class RemoteDeviceDiscoverer {
    private final LocalDevice localDevice;
    private final Consumer<RemoteDevice> callback;

    private DeviceEventAdapter adapter;
    private final List<RemoteDevice> allDevices = new ArrayList<>();
    private final List<RemoteDevice> latestDevices = new ArrayList<>();

    public RemoteDeviceDiscoverer(final LocalDevice localDevice) {
        this(localDevice, null);
    }

    public RemoteDeviceDiscoverer(final LocalDevice localDevice, final Consumer<RemoteDevice> callback) {
        this.localDevice = localDevice;
        this.callback = callback;
    }

    public void start() {
        adapter = new DeviceEventAdapter() {
            @Override
            public void iAmReceived(final RemoteDevice d) {
                synchronized (allDevices) {
                    // Check if we already know about this device.
                    boolean found = false;
                    for (final RemoteDevice known : allDevices) {
                        if (d.getInstanceNumber() == known.getInstanceNumber()) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // Add to all devices
                        allDevices.add(d);

                        // Add to latest devices
                        latestDevices.add(d);

                        // Notify the callback
                        if (callback != null) {
                            callback.accept(d);
                        }
                    }
                }
            }
        };

        // Register self as an event listener
        localDevice.getEventHandler().addListener(adapter);

        // Send a WhoIs
        localDevice.sendGlobalBroadcast(new WhoIsRequest());
    }

    public void stop() {
        // Unregister as a listener
        localDevice.getEventHandler().removeListener(adapter);
    }

    /**
     * Returns all devices discovered by this discoverer so far.
     */
    public List<RemoteDevice> getRemoteDevices() {
        synchronized (allDevices) {
            return new ArrayList<>(allDevices);
        }
    }

    /**
     * Returns all devices discovered by this discoverer since the last time this method was called.
     */
    public List<RemoteDevice> getLatestRemoteDevices() {
        synchronized (allDevices) {
            final List<RemoteDevice> result = new ArrayList<>(latestDevices);
            latestDevices.clear();
            return result;
        }
    }
}
