
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

/**
 * @author Suresh Kumar
 */
public class ShedLevel extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, UnsignedInteger.class);
        choiceOptions.addContextual(1, UnsignedInteger.class);
        choiceOptions.addContextual(2, Real.class);
    }

    private final Choice choice;

    public ShedLevel(final UnsignedInteger datum, final boolean percent) {
        if (percent)
            choice = new Choice(0, datum, choiceOptions);
        else
            choice = new Choice(1, datum, choiceOptions);
    }

    public ShedLevel(final Real amount) {
        choice = new Choice(2, amount, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public UnsignedInteger getPercent() {
        return (UnsignedInteger) choice.getDatum();
    }

    public UnsignedInteger getLevel() {
        return (UnsignedInteger) choice.getDatum();
    }

    public Real getAmount() {
        return (Real) choice.getDatum();
    }

    public boolean isPercent() {
        return choice.getContextId() == 0;
    }

    public boolean isLevel() {
        return choice.getContextId() == 1;
    }

    public boolean isAmount() {
        return choice.getContextId() == 2;
    }

    public Choice getChoice() {
        return choice;
    }

    public <T extends Encodable> T getValue() {
        return choice.getDatum();
    }
    
    public ShedLevel(final ByteQueue queue) throws BACnetException {
        choice = new Choice(queue, choiceOptions);
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
        final ShedLevel other = (ShedLevel) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ShedLevel [choice=" + choice + ']';
    }
}
