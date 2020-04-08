
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.AmbiguousValue;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.BitString;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Date;
import com.renovo.bacnet4j.type.primitive.Double;
import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.type.primitive.Primitive;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.SignedInteger;
import com.renovo.bacnet4j.type.primitive.Time;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class PriorityValue extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addPrimitive(Null.class);
        choiceOptions.addPrimitive(Real.class);
        choiceOptions.addPrimitive(Enumerated.class);
        choiceOptions.addPrimitive(UnsignedInteger.class);
        choiceOptions.addPrimitive(Boolean.class);
        choiceOptions.addPrimitive(SignedInteger.class);
        choiceOptions.addPrimitive(Double.class);
        choiceOptions.addPrimitive(Time.class);
        choiceOptions.addPrimitive(CharacterString.class);
        choiceOptions.addPrimitive(OctetString.class);
        choiceOptions.addPrimitive(BitString.class);
        choiceOptions.addPrimitive(Date.class);
        choiceOptions.addPrimitive(ObjectIdentifier.class);

        choiceOptions.addContextual(0, AmbiguousValue.class);
        choiceOptions.addContextual(1, DateTime.class);
    }

    private final Choice choice;

    public PriorityValue(final Encodable value) {
        if (value instanceof Primitive)
            choice = new Choice(value, choiceOptions);
        else if (value instanceof DateTime)
            choice = new Choice(1, value, choiceOptions);
        else
            choice = new Choice(0, value, choiceOptions);
    }

    public Null getNullValue() {
        return choice.getDatum();
    }

    public Real getRealValue() {
        return choice.getDatum();
    }

    public Enumerated getEnumeratedValue() {
        return choice.getDatum();
    }

    public UnsignedInteger getUnsignedValue() {
        return choice.getDatum();
    }

    public Boolean getBooleanValue() {
        return choice.getDatum();
    }

    public SignedInteger getSignedValue() {
        return choice.getDatum();
    }

    public Double getDoubleValue() {
        return choice.getDatum();
    }

    public Time getTimeValue() {
        return choice.getDatum();
    }

    public CharacterString getCharacterStringValue() {
        return choice.getDatum();
    }

    public OctetString getOctetStringValue() {
        return choice.getDatum();
    }

    public BitString getBitStringValue() {
        return choice.getDatum();
    }

    public Date getDateValue() {
        return choice.getDatum();
    }

    public ObjectIdentifier getOidValue() {
        return choice.getDatum();
    }

    public DateTime getDateTimeValue() {
        return choice.getDatum();
    }

    public Encodable getConstructedValue() {
        return choice.getDatum();
    }

    public <T extends Encodable> T getValue() {
        return choice.getDatum();
    }

    public boolean isa(final Class<?> clazz) {
        return choice.isa(clazz);
    }

    @Override
    public String toString() {
        return "PriorityValue(" + choice + ")";
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public PriorityValue(final ByteQueue queue) throws BACnetException {
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
        final PriorityValue other = (PriorityValue) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }
}
