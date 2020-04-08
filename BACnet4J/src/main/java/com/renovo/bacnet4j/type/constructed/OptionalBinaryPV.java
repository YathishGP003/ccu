
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.enumerated.BinaryPV;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class OptionalBinaryPV extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addPrimitive(Null.class);
        choiceOptions.addPrimitive(BinaryPV.class);
    }

    private final Choice choice;

    public OptionalBinaryPV() {
        this.choice = new Choice(Null.instance, choiceOptions);
    }

    public OptionalBinaryPV(final BinaryPV binaryPV) {
        this.choice = new Choice(binaryPV, choiceOptions);
    }

    public boolean isNullValue() {
        return choice.isa(Null.class);
    }
    
    public Null getNullValue() {
        return choice.getDatum();
    }

    public boolean isBinaryPVValue() {
        return choice.isa(BinaryPV.class);
    }
    
    public BinaryPV getBinaryPVValue() {
        return choice.getDatum();
    }
    
    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public OptionalBinaryPV(final ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (choice == null ? 0 : choice.hashCode());
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
        final OptionalBinaryPV other = (OptionalBinaryPV) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "OptionalBinaryPV [choice=" + choice + ']';
    }
}
