
package com.renovo.bacnet4j.util;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.service.acknowledgement.ReadPropertyAck;
import com.renovo.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;

public class DiscoveryUtils {
    public static void getExtendedDeviceInformation(final LocalDevice localDevice, final RemoteDevice d)
            throws BACnetException {
        final ObjectIdentifier oid = d.getObjectIdentifier();

        // Get the device's supported services
        if (d.getServicesSupported() == null) {
            final ReadPropertyAck supportedServicesAck = (ReadPropertyAck) localDevice.send(d, new ReadPropertyRequest(oid, PropertyIdentifier.protocolServicesSupported)).get();
            d.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, supportedServicesAck.getValue());
        }

        // Uses the readProperties method here because this list will probably be extended.
        final PropertyReferences properties = new PropertyReferences();
        addIfMissing(d, properties, PropertyIdentifier.objectName);
        addIfMissing(d, properties, PropertyIdentifier.protocolVersion);
        addIfMissing(d, properties, PropertyIdentifier.vendorIdentifier);
        addIfMissing(d, properties, PropertyIdentifier.modelName);
        addIfMissing(d, properties, PropertyIdentifier.maxSegmentsAccepted);

        if (properties.size() > 0) {
            // Only send a request if we have to.
            final PropertyValues values = RequestUtils.readProperties(localDevice, d, properties, false, null);

            values.forEach((opr) -> {
                final Encodable value = values.getNullOnError(oid, opr.getPropertyIdentifier());
                d.setDeviceProperty(opr.getPropertyIdentifier(), value);
            });
        }
    }

    private static void addIfMissing(final RemoteDevice d, final PropertyReferences properties,
            final PropertyIdentifier pid) {
        if (d.getDeviceProperty(pid) == null)
            properties.add(d.getObjectIdentifier(), pid);
    }
}
