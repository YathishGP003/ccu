
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Unsigned8;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class SecurityKeySet extends BaseType {
    private final Unsigned8 keyRevision;
    private final DateTime activationTime;
    private final DateTime expirationTime;
    private final SequenceOf<KeyIdentifier> keyIds;

    public SecurityKeySet(final Unsigned8 keyRevision, final DateTime activationTime, final DateTime expirationTime,
            final SequenceOf<KeyIdentifier> keyIds) {
        this.keyRevision = keyRevision;
        this.activationTime = activationTime;
        this.expirationTime = expirationTime;
        this.keyIds = keyIds;
    }

    public Unsigned8 getKeyRevision() {
        return keyRevision;
    }

    public DateTime getActivationTime() {
        return activationTime;
    }

    public DateTime getExpirationTime() {
        return expirationTime;
    }

    public SequenceOf<KeyIdentifier> getKeyIds() {
        return keyIds;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, keyRevision, 0);
        write(queue, activationTime, 1);
        write(queue, expirationTime, 2);
        write(queue, keyIds, 3);
    }

    @Override
    public String toString() {
        return "SecurityKeySet [keyRevision=" + keyRevision + ", activationTime=" + activationTime + ", expirationTime="
                + expirationTime + ", keyIds=" + keyIds + "]";
    }

    public SecurityKeySet(final ByteQueue queue) throws BACnetException {
        keyRevision = read(queue, Unsigned8.class, 0);
        activationTime = read(queue, DateTime.class, 1);
        expirationTime = read(queue, DateTime.class, 2);
        keyIds = readSequenceOf(queue, KeyIdentifier.class, 3);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (activationTime == null ? 0 : activationTime.hashCode());
        result = prime * result + (expirationTime == null ? 0 : expirationTime.hashCode());
        result = prime * result + (keyIds == null ? 0 : keyIds.hashCode());
        result = prime * result + (keyRevision == null ? 0 : keyRevision.hashCode());
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
        final SecurityKeySet other = (SecurityKeySet) obj;
        if (activationTime == null) {
            if (other.activationTime != null)
                return false;
        } else if (!activationTime.equals(other.activationTime))
            return false;
        if (expirationTime == null) {
            if (other.expirationTime != null)
                return false;
        } else if (!expirationTime.equals(other.expirationTime))
            return false;
        if (keyIds == null) {
            if (other.keyIds != null)
                return false;
        } else if (!keyIds.equals(other.keyIds))
            return false;
        if (keyRevision == null) {
            if (other.keyRevision != null)
                return false;
        } else if (!keyRevision.equals(other.keyRevision))
            return false;
        return true;
    }
}
