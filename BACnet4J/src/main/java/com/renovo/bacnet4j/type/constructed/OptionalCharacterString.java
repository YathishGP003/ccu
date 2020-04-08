
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class OptionalCharacterString extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addPrimitive(Null.class);
        choiceOptions.addPrimitive(CharacterString.class);
    }

    private final Choice choice;

    public OptionalCharacterString() {
        this.choice = new Choice(Null.instance, choiceOptions);
    }

    public OptionalCharacterString(final CharacterString characterString) {
        this.choice = new Choice(characterString, choiceOptions);
    }

    public OptionalCharacterString(final String string) {
        this.choice = new Choice(new CharacterString(string), choiceOptions);
    }

    public Null getNullValue() {
        return choice.getDatum();
    }

    public CharacterString getCharacterStringValue() {
        return choice.getDatum();
    }

    public boolean isCharacterStringValue() {
        return choice.getDatum() instanceof CharacterString;
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

    public OptionalCharacterString(final ByteQueue queue) throws BACnetException {
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
        final OptionalCharacterString other = (OptionalCharacterString) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "OptionalCharacterString [choice=" + choice + ']';
    }   
}
