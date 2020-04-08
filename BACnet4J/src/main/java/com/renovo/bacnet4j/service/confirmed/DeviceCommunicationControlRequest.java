
package com.renovo.bacnet4j.service.confirmed;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class DeviceCommunicationControlRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 17;

    private final UnsignedInteger timeDuration;
    private final EnableDisable enableDisable;
    private final CharacterString password;

    public DeviceCommunicationControlRequest(final UnsignedInteger timeDuration, final EnableDisable enableDisable,
            final CharacterString password) {
        super();
        this.timeDuration = timeDuration;
        this.enableDisable = enableDisable;
        this.password = password;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        String givenPassword = null;
        if (password != null)
            givenPassword = password.getValue();

        if (!Objects.equals(givenPassword, localDevice.getPassword())) {
            throw new BACnetErrorException(getChoiceId(), ErrorClass.security, ErrorCode.passwordFailure);
        }

        int minutes = 0;
        if (timeDuration != null)
            minutes = timeDuration.intValue();
        localDevice.setCommunicationControl(enableDisable, minutes);

        return null;
    }

    @Override
    public void write(final ByteQueue queue) {
        writeOptional(queue, timeDuration, 0);
        write(queue, enableDisable, 1);
        writeOptional(queue, password, 2);
    }

    DeviceCommunicationControlRequest(final ByteQueue queue) throws BACnetException {
        timeDuration = readOptional(queue, UnsignedInteger.class, 0);
        enableDisable = read(queue, EnableDisable.class, 1);
        password = readOptional(queue, CharacterString.class, 2);
    }

    @Override
    public boolean isCommunicationControlOverride() {
        return true;
    }

    public static class EnableDisable extends Enumerated {
        public static final EnableDisable enable = new EnableDisable(0);
        public static final EnableDisable disable = new EnableDisable(1);
        public static final EnableDisable disableInitiation = new EnableDisable(2);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

        public static EnableDisable forId(final int id) {
            EnableDisable e = (EnableDisable) idMap.get(id);
            if (e == null)
                e = new EnableDisable(id);
            return e;
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static EnableDisable forName(final String name) {
            return (EnableDisable) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private EnableDisable(final int value) {
            super(value);
        }

        public EnableDisable(final ByteQueue queue) throws BACnetErrorException {
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
        result = PRIME * result + (enableDisable == null ? 0 : enableDisable.hashCode());
        result = PRIME * result + (password == null ? 0 : password.hashCode());
        result = PRIME * result + (timeDuration == null ? 0 : timeDuration.hashCode());
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
        final DeviceCommunicationControlRequest other = (DeviceCommunicationControlRequest) obj;
        if (enableDisable == null) {
            if (other.enableDisable != null)
                return false;
        } else if (!enableDisable.equals(other.enableDisable))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (timeDuration == null) {
            if (other.timeDuration != null)
                return false;
        } else if (!timeDuration.equals(other.timeDuration))
            return false;
        return true;
    }
}
