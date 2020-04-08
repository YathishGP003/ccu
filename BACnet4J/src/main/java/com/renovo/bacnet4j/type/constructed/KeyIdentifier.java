
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Unsigned8;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class KeyIdentifier extends BaseType {
    private final Unsigned8 algorithm;
    private final Unsigned8 keyId;

    public KeyIdentifier(final Unsigned8 algorithm, final Unsigned8 keyId) {
        this.algorithm = algorithm;
        this.keyId = keyId;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, algorithm);
        write(queue, keyId);
    }

    public KeyIdentifier(final ByteQueue queue) throws BACnetException {
        algorithm = read(queue, Unsigned8.class, 0);
        keyId = read(queue, Unsigned8.class, 1);
    }

    public Unsigned8 getAlgorithm() {
        return algorithm;
    }

    public Unsigned8 getKeyId() {
        return keyId;
    }

    @Override
    public String toString() {
        return "KeyIdentifier [algorithm=" + algorithm + ", keyId=" + keyId + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (algorithm == null ? 0 : algorithm.hashCode());
        result = prime * result + (keyId == null ? 0 : keyId.hashCode());
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
        final KeyIdentifier other = (KeyIdentifier) obj;
        if (algorithm == null) {
            if (other.algorithm != null)
                return false;
        } else if (!algorithm.equals(other.algorithm))
            return false;
        if (keyId == null) {
            if (other.keyId != null)
                return false;
        } else if (!keyId.equals(other.keyId))
            return false;
        return true;
    }
}
