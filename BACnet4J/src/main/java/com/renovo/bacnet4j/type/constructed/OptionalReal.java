
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class OptionalReal extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addPrimitive(Null.class);
        choiceOptions.addPrimitive(Real.class);
    }

    private final Choice choice;

    public OptionalReal() {
        this.choice = new Choice(Null.instance, choiceOptions);
    }

    public OptionalReal(final Real real) {
        this.choice = new Choice(real, choiceOptions);
    }

    public OptionalReal(final float real) {
        this.choice = new Choice(new Real(real), choiceOptions);
    }

    public Null getNullValue() {
        return choice.getDatum();
    }

    public Real getRealValue() {
        return choice.getDatum();
    }

    public boolean isRealValue() {
        return choice.getDatum() instanceof Real;
    }

    public boolean isNullValue() {
        return choice.getDatum() instanceof Null;
    }

    public Choice getChoice() {
        return choice;
    }

    public <T extends Encodable> T getValue() {
        return choice.getDatum();
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public OptionalReal(final ByteQueue queue) throws BACnetException {
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
        final OptionalReal other = (OptionalReal) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "OptionalReal [choice=" + choice + ']';
    }   
}
