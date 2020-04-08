
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ClientCov extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addPrimitive(Real.class);
        choiceOptions.addPrimitive(Null.class);
    }

    private final Choice entry;

    public ClientCov(final Real realIncrement) {
        entry = new Choice(realIncrement, choiceOptions);
    }

    public ClientCov(final Null defaultIncrement) {
        entry = new Choice(defaultIncrement, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, entry);
    }

    public ClientCov(final ByteQueue queue) throws BACnetException {
        entry = readChoice(queue, choiceOptions);
    }

    public boolean isRealIncrement() {
        return entry.isa(Real.class);
    }

    public boolean isDefaultIncrement() {
        return entry.isa(Null.class);
    }

    public Real getRealIncrement() {
        return (Real) entry.getDatum();
    }

    public Null getDefaultIncrement() {
        return (Null) entry.getDatum();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (entry == null ? 0 : entry.hashCode());
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
        final ClientCov other = (ClientCov) obj;
        if (entry == null) {
            if (other.entry != null)
                return false;
        } else if (!entry.equals(other.entry))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ClientCov [entry=" + entry + ']';
    }
}
