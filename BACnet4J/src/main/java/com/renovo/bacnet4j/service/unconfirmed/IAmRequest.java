
package com.renovo.bacnet4j.service.unconfirmed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Segmentation;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.DiscoveryUtils;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class IAmRequest extends UnconfirmedRequestService {
    static final Logger LOG = LoggerFactory.getLogger(IAmRequest.class);

    public static final byte TYPE_ID = 0;

    private final ObjectIdentifier iAmDeviceIdentifier;
    private final UnsignedInteger maxAPDULengthAccepted;
    private final Segmentation segmentationSupported;
    private final UnsignedInteger vendorId;

    /**
     * This field allows us to properly implement 16.1.2.
     */
    private boolean isResponseToWhoIs;

    public IAmRequest(final ObjectIdentifier iamDeviceIdentifier, final UnsignedInteger maxAPDULengthAccepted,
            final Segmentation segmentationSupported, final UnsignedInteger vendorId) {
        this.iAmDeviceIdentifier = iamDeviceIdentifier;
        this.maxAPDULengthAccepted = maxAPDULengthAccepted;
        this.segmentationSupported = segmentationSupported;
        this.vendorId = vendorId;
    }

    public IAmRequest withIsResponseToWhoIs(final boolean isResponseToWhoIs) {
        this.isResponseToWhoIs = isResponseToWhoIs;
        return this;
    }

    public boolean isResponseToWhoIs() {
        return isResponseToWhoIs;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void handle(final LocalDevice localDevice, final Address from) {
        if (!ObjectType.device.equals(iAmDeviceIdentifier.getObjectType())) {
            LOG.warn("Received IAm from an object that is not a device from {}", from);
            return;
        }

        // Make sure we're not hearing from ourselves.
        final int myDoi = localDevice.getInstanceNumber();
        final int remoteDoi = iAmDeviceIdentifier.getInstanceNumber();
        if (remoteDoi == myDoi) {
            // Get my bacnet address and compare the addresses
            for (final Address addr : localDevice.getAllLocalAddresses()) {
                if (addr.getMacAddress().equals(from.getMacAddress()))
                    // This is a local address, so ignore.
                    return;
            }
            LOG.warn("Another instance with my device instance ID found at {}", from);
            localDevice.notifySameDeviceIdCallback(from);
        }

        localDevice.updateRemoteDevice(remoteDoi, from);

        final RemoteDevice d = localDevice.getCachedRemoteDevice(remoteDoi);
        if (d == null) {
            // Populate the object with discovered values, but do so in a different thread.
            localDevice.execute(() -> {
                LOG.debug("{} received an IAm from {}. Asynchronously creating remote device",
                        localDevice.getInstanceNumber(), remoteDoi);
                try {
                    final RemoteDevice rd = new RemoteDevice(localDevice, remoteDoi, from);
                    rd.setDeviceProperty(PropertyIdentifier.maxApduLengthAccepted, maxAPDULengthAccepted);
                    rd.setDeviceProperty(PropertyIdentifier.segmentationSupported, segmentationSupported);
                    rd.setDeviceProperty(PropertyIdentifier.vendorIdentifier, vendorId);
                    DiscoveryUtils.getExtendedDeviceInformation(localDevice, rd);
                    localDevice.getEventHandler().fireIAmReceived(rd);
                } catch (final BACnetException e) {
                    LOG.warn("Error in {} while discovering extended device information from {} at {}",
                            localDevice.getId(), remoteDoi, from, e);
                }
            });
        } else {
            d.setDeviceProperty(PropertyIdentifier.maxApduLengthAccepted, maxAPDULengthAccepted);
            d.setDeviceProperty(PropertyIdentifier.segmentationSupported, segmentationSupported);
            d.setDeviceProperty(PropertyIdentifier.vendorIdentifier, vendorId);
            localDevice.getEventHandler().fireIAmReceived(d);
        }
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, iAmDeviceIdentifier);
        write(queue, maxAPDULengthAccepted);
        write(queue, segmentationSupported);
        write(queue, vendorId);
    }

    IAmRequest(final ByteQueue queue) throws BACnetException {
        iAmDeviceIdentifier = read(queue, ObjectIdentifier.class);
        maxAPDULengthAccepted = read(queue, UnsignedInteger.class);
        segmentationSupported = read(queue, Segmentation.class);
        vendorId = read(queue, UnsignedInteger.class);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (iAmDeviceIdentifier == null ? 0 : iAmDeviceIdentifier.hashCode());
        result = PRIME * result + (maxAPDULengthAccepted == null ? 0 : maxAPDULengthAccepted.hashCode());
        result = PRIME * result + (segmentationSupported == null ? 0 : segmentationSupported.hashCode());
        result = PRIME * result + (vendorId == null ? 0 : vendorId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final IAmRequest other = (IAmRequest) obj;
        if (iAmDeviceIdentifier == null) {
            if (other.iAmDeviceIdentifier != null)
                return false;
        } else if (!iAmDeviceIdentifier.equals(other.iAmDeviceIdentifier))
            return false;
        if (maxAPDULengthAccepted == null) {
            if (other.maxAPDULengthAccepted != null)
                return false;
        } else if (!maxAPDULengthAccepted.equals(other.maxAPDULengthAccepted))
            return false;
        if (segmentationSupported == null) {
            if (other.segmentationSupported != null)
                return false;
        } else if (!segmentationSupported.equals(other.segmentationSupported))
            return false;
        if (vendorId == null) {
            if (other.vendorId != null)
                return false;
        } else if (!vendorId.equals(other.vendorId))
            return false;
        return true;
    }
}
