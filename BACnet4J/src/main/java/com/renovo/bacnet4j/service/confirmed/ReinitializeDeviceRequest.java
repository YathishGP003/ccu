
package com.renovo.bacnet4j.service.confirmed;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.event.ReinitializeDeviceHandler;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.service.confirmed.DeviceCommunicationControlRequest.EnableDisable;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ReinitializeDeviceRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 20;

    private final ReinitializedStateOfDevice reinitializedStateOfDevice;
    private final CharacterString password;

    public ReinitializeDeviceRequest(final ReinitializedStateOfDevice reinitializedStateOfDevice,
            final CharacterString password) {
        this.reinitializedStateOfDevice = reinitializedStateOfDevice;
        this.password = password;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, reinitializedStateOfDevice, 0);
        writeOptional(queue, password, 1);
    }

    ReinitializeDeviceRequest(final ByteQueue queue) throws BACnetException {
        reinitializedStateOfDevice = read(queue, ReinitializedStateOfDevice.class, 0);
        password = readOptional(queue, CharacterString.class, 1);
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final ReinitializeDeviceHandler handler = localDevice.getReinitializeDeviceHandler();
        if (handler == null) {
            throw new BACnetErrorException(ErrorClass.device, ErrorCode.notConfigured);
        }

        if (reinitializedStateOfDevice.isOneOf( //
                ReinitializedStateOfDevice.startBackup, //
                ReinitializedStateOfDevice.endBackup, //
                ReinitializedStateOfDevice.startRestore, //
                ReinitializedStateOfDevice.endRestore, //
                ReinitializedStateOfDevice.abortRestore)) {
            if (EnableDisable.disable.equals(localDevice.getCommunicationControlState())) {
                throw new BACnetErrorException(ErrorClass.services, ErrorCode.communicationDisabled);
            }
        }

        // Performance of warmstart and coldstart are listed before the password check in 16.4.2, but we'll check
        // the password here anyway.
        String givenPassword = null;
        if (password != null) {
            givenPassword = password.getValue();
        }
        if (!Objects.equals(givenPassword, localDevice.getPassword())) {
            throw new BACnetErrorException(getChoiceId(), ErrorClass.security, ErrorCode.passwordFailure);
        }

        handler.handle(localDevice, from, reinitializedStateOfDevice);

        return null;
    }

    @Override
    public boolean isCommunicationControlOverride() {
        return true;
    }

    public static class ReinitializedStateOfDevice extends Enumerated {
        public static final ReinitializedStateOfDevice coldstart = new ReinitializedStateOfDevice(0);
        public static final ReinitializedStateOfDevice warmstart = new ReinitializedStateOfDevice(1);
        public static final ReinitializedStateOfDevice startBackup = new ReinitializedStateOfDevice(2);
        public static final ReinitializedStateOfDevice endBackup = new ReinitializedStateOfDevice(3);
        public static final ReinitializedStateOfDevice startRestore = new ReinitializedStateOfDevice(4);
        public static final ReinitializedStateOfDevice endRestore = new ReinitializedStateOfDevice(5);
        public static final ReinitializedStateOfDevice abortRestore = new ReinitializedStateOfDevice(6);
        public static final ReinitializedStateOfDevice activateChanges = new ReinitializedStateOfDevice(7);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

        public static ReinitializedStateOfDevice forId(final int id) {
            ReinitializedStateOfDevice e = (ReinitializedStateOfDevice) idMap.get(id);
            if (e == null)
                e = new ReinitializedStateOfDevice(id);
            return e;
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static ReinitializedStateOfDevice forName(final String name) {
            return (ReinitializedStateOfDevice) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private ReinitializedStateOfDevice(final int value) {
            super(value);
        }

        public ReinitializedStateOfDevice(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (password == null ? 0 : password.hashCode());
        result = PRIME * result + (reinitializedStateOfDevice == null ? 0 : reinitializedStateOfDevice.hashCode());
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
        final ReinitializeDeviceRequest other = (ReinitializeDeviceRequest) obj;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (reinitializedStateOfDevice == null) {
            if (other.reinitializedStateOfDevice != null)
                return false;
        } else if (!reinitializedStateOfDevice.equals(other.reinitializedStateOfDevice))
            return false;
        return true;
    }

    public ReinitializedStateOfDevice getReinitializedStateOfDevice(){
        return reinitializedStateOfDevice;
    }

    public String getPasswordRecieved(){
        return password.toString();
    }
}
