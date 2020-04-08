
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.enumerated.AuthenticationFactorType;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AuthenticationFactor extends BaseType {
    private final AuthenticationFactorType formatType;
    private final UnsignedInteger formatClass;
    private final OctetString value;

    public AuthenticationFactor(final AuthenticationFactorType formatType, final UnsignedInteger formatClass,
            final OctetString value) {
        this.formatType = formatType;
        this.formatClass = formatClass;
        this.value = value;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, formatType, 0);
        write(queue, formatClass, 1);
        write(queue, value, 2);
    }

    public AuthenticationFactor(final ByteQueue queue) throws BACnetException {
        formatType = read(queue, AuthenticationFactorType.class, 0);
        formatClass = read(queue, UnsignedInteger.class, 1);
        value = read(queue, OctetString.class, 2);
    }

    public AuthenticationFactorType getFormatType() {
        return formatType;
    }

    public UnsignedInteger getFormatClass() {
        return formatClass;
    }

    public OctetString getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (formatClass == null ? 0 : formatClass.hashCode());
        result = prime * result + (formatType == null ? 0 : formatType.hashCode());
        result = prime * result + (value == null ? 0 : value.hashCode());
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
        final AuthenticationFactor other = (AuthenticationFactor) obj;
        if (formatClass == null) {
            if (other.formatClass != null)
                return false;
        } else if (!formatClass.equals(other.formatClass))
            return false;
        if (formatType == null) {
            if (other.formatType != null)
                return false;
        } else if (!formatType.equals(other.formatType))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AuthenticationFactor [formatType=" + formatType + ", formatClass=" + formatClass + ", value=" + value + ']';
    }  
}
