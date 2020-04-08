package com.renovo.bacnet4j.event;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.service.confirmed.ReinitializeDeviceRequest.ReinitializedStateOfDevice;
import com.renovo.bacnet4j.type.constructed.Address;

/**
 * Reinitialize device handler. By the time this object is called, the local device's password has already been
 * validated, and the indicated action should be carried out.
 *
 * @author Matthew
 */
public interface ReinitializeDeviceHandler {
    void handle(final LocalDevice localDevice, final Address from,
            final ReinitializedStateOfDevice reinitializedStateOfDevice) throws BACnetErrorException;
}
