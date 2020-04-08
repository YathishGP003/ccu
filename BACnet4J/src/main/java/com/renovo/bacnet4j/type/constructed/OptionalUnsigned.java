
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class OptionalUnsigned extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addPrimitive(Null.class);
        choiceOptions.addPrimitive(UnsignedInteger.class);
    }

    private final Choice choice;

    public OptionalUnsigned() {
        this.choice = new Choice(Null.instance, choiceOptions);
    }

    public OptionalUnsigned(final UnsignedInteger unsigned) {
        this.choice = new Choice(unsigned, choiceOptions);
    }

    public OptionalUnsigned(final int unsigned) {
        this.choice = new Choice(new UnsignedInteger(unsigned), choiceOptions);
    }

    public boolean isNull() {
        return choice.getContextId() == 0;
    }

    public Null getNullValue() {
        return choice.getDatum();
    }

    public UnsignedInteger getUnsignedIntegerValue() {
        return choice.getDatum();
    }

    public boolean isUnsignedIntegerValue() {
        return choice.getDatum() instanceof UnsignedInteger;
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

    public OptionalUnsigned(final ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    @Override
    public String toString() {
        return "OptionalUnsigned [choice=" + choice + "]";
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
        final OptionalUnsigned other = (OptionalUnsigned) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }
}
