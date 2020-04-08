
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class HostAddress extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, Null.class); // 0
        choiceOptions.addContextual(1, OctetString.class); // 1
        choiceOptions.addContextual(2, CharacterString.class); // 2
    }

    private final Choice state;

    public HostAddress(final Null none) {
        state = new Choice(0, none, choiceOptions);
    }

    public HostAddress(final OctetString ipAddress) {
        state = new Choice(1, ipAddress, choiceOptions);
    }

    public HostAddress(final CharacterString name) {
        state = new Choice(2, name, choiceOptions);
    }
    
    public boolean isIpAddress() {
        return this.state.getDatum() instanceof OctetString;
    }
    
    public OctetString getIpAddress() {
        return this.state.getDatum();
    }

    public boolean isName() {
        return this.state.getDatum() instanceof CharacterString;
    }
    
    public CharacterString getName() {
        return this.state.getDatum();
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Encodable> T getAddress() {
        return (T) state.getDatum();
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, state);
    }

    public HostAddress(final ByteQueue queue) throws BACnetException {
        state = new Choice(queue, choiceOptions);
    }

    @Override
    public String toString() {
        return "HostAddress [state=" + state + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (state == null ? 0 : state.hashCode());
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
        final HostAddress other = (HostAddress) obj;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        return true;
    }
}
