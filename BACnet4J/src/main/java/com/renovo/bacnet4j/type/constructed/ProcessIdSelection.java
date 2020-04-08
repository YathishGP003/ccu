
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.type.primitive.Unsigned32;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ProcessIdSelection extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addPrimitive(Unsigned32.class);
        choiceOptions.addPrimitive(Null.class);
    }

    private final Choice choice;

    public ProcessIdSelection(final Unsigned32 processIdentifier) {
        this.choice = new Choice(processIdentifier, choiceOptions);
    }

    public ProcessIdSelection(final Null nullValue) {
        this.choice = new Choice(nullValue, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public ProcessIdSelection(final ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    public boolean isNullValue() {
        return choice.getDatum() instanceof Null;
    }

    public Null getNullValue() {
        return choice.getDatum();
    }

    public boolean isProcessIdentifier() {
        return choice.getDatum() instanceof Unsigned32;
    }

    public Unsigned32 getProcessIdentifier() {
        return choice.getDatum();
    }

    public Choice getChoice() {
        return choice;
    }

    public <T extends Encodable> T getValue() {
        return choice.getDatum();
    }

    @Override
    public String toString() {
        return "ProcessIdSelection [choice=" + choice + "]";
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
        final ProcessIdSelection other = (ProcessIdSelection) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }
}
