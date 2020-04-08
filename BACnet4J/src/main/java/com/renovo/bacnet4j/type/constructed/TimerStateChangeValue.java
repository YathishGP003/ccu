
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
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.SignedInteger;
import com.renovo.bacnet4j.type.primitive.Time;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class TimerStateChangeValue extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addPrimitive(Null.class);
        choiceOptions.addPrimitive(Boolean.class);
        choiceOptions.addPrimitive(UnsignedInteger.class);
        choiceOptions.addPrimitive(SignedInteger.class);
        choiceOptions.addPrimitive(Real.class);
        choiceOptions.addPrimitive(Double.class);
        choiceOptions.addPrimitive(OctetString.class);
        choiceOptions.addPrimitive(CharacterString.class);
        choiceOptions.addPrimitive(BitString.class);
        choiceOptions.addPrimitive(Enumerated.class);
        choiceOptions.addPrimitive(Date.class);
        choiceOptions.addPrimitive(Time.class);
        choiceOptions.addPrimitive(ObjectIdentifier.class);
        choiceOptions.addContextual(0, Null.class);
        choiceOptions.addContextual(1, AmbiguousValue.class);
        choiceOptions.addContextual(2, DateTime.class);
        choiceOptions.addContextual(3, LightingCommand.class);
    }

    private final Choice choice;

    public TimerStateChangeValue(final Null nullValue) {
        this.choice = new Choice(nullValue, choiceOptions);
    }

    public TimerStateChangeValue(final Boolean booleanValue) {
        this.choice = new Choice(booleanValue, choiceOptions);
    }

    public TimerStateChangeValue(final UnsignedInteger unsignedValue) {
        this.choice = new Choice(unsignedValue, choiceOptions);
    }

    public TimerStateChangeValue(final SignedInteger signedValue) {
        this.choice = new Choice(signedValue, choiceOptions);
    }

    public TimerStateChangeValue(final Real realValue) {
        this.choice = new Choice(realValue, choiceOptions);
    }

    public TimerStateChangeValue(final Double doubleValue) {
        this.choice = new Choice(doubleValue, choiceOptions);
    }

    public TimerStateChangeValue(final OctetString octetStringValue) {
        this.choice = new Choice(octetStringValue, choiceOptions);
    }

    public TimerStateChangeValue(final CharacterString characterStringValue) {
        this.choice = new Choice(characterStringValue, choiceOptions);
    }

    public TimerStateChangeValue(final BitString bitStringValue) {
        this.choice = new Choice(bitStringValue, choiceOptions);
    }

    public TimerStateChangeValue(final Enumerated enumeratedValue) {
        this.choice = new Choice(enumeratedValue, choiceOptions);
    }

    public TimerStateChangeValue(final Date dateValue) {
        this.choice = new Choice(dateValue, choiceOptions);
    }

    public TimerStateChangeValue(final Time timeValue) {
        this.choice = new Choice(timeValue, choiceOptions);
    }

    public TimerStateChangeValue(final ObjectIdentifier oidValue) {
        this.choice = new Choice(oidValue, choiceOptions);
    }

    public TimerStateChangeValue() {
        this.choice = new Choice(0, Null.instance, choiceOptions);
    }

    public TimerStateChangeValue(final BaseType constructedValue) {
        this.choice = new Choice(1, constructedValue, choiceOptions);
    }

    public TimerStateChangeValue(final DateTime dateTimeValue) {
        this.choice = new Choice(2, dateTimeValue, choiceOptions);
    }

    public TimerStateChangeValue(final LightingCommand lightingCommandValue) {
        this.choice = new Choice(3, lightingCommandValue, choiceOptions);
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

    public TimerStateChangeValue(final ByteQueue queue) throws BACnetException {
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
        final TimerStateChangeValue other = (TimerStateChangeValue) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TimerStateChangeValue [choice=" + choice + ']';
    }
}
