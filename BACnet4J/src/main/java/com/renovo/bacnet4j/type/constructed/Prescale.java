
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class Prescale extends BaseType {
    private final UnsignedInteger multiplier;
    private final UnsignedInteger moduloDivide;

    public Prescale(final UnsignedInteger multiplier, final UnsignedInteger moduloDivide) {
        this.multiplier = multiplier;
        this.moduloDivide = moduloDivide;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, multiplier, 0);
        write(queue, moduloDivide, 1);
    }

    public Prescale(final ByteQueue queue) throws BACnetException {
        multiplier = read(queue, UnsignedInteger.class, 0);
        moduloDivide = read(queue, UnsignedInteger.class, 1);
    }

    public UnsignedInteger getMultiplier() {
        return multiplier;
    }

    public UnsignedInteger getModuloDivide() {
        return moduloDivide;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (moduloDivide == null ? 0 : moduloDivide.hashCode());
        result = PRIME * result + (multiplier == null ? 0 : multiplier.hashCode());
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
        final Prescale other = (Prescale) obj;
        if (moduloDivide == null) {
            if (other.moduloDivide != null)
                return false;
        } else if (!moduloDivide.equals(other.moduloDivide))
            return false;
        if (multiplier == null) {
            if (other.multiplier != null)
                return false;
        } else if (!multiplier.equals(other.multiplier))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Prescale [multiplier=" + multiplier + ", moduloDivide=" + moduloDivide + ']';
    }
}
