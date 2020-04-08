
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.BaseType;
import com.renovo.bacnet4j.type.constructed.Choice;
import com.renovo.bacnet4j.type.constructed.ChoiceOptions;
import com.renovo.bacnet4j.type.enumerated.EventType;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class EventParameter extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(ChangeOfBitString.TYPE_ID & 0xff, ChangeOfBitString.class); // 0
        choiceOptions.addContextual(ChangeOfState.TYPE_ID & 0xff, ChangeOfState.class); // 1
        choiceOptions.addContextual(ChangeOfValue.TYPE_ID & 0xff, ChangeOfValue.class); // 2
        choiceOptions.addContextual(CommandFailure.TYPE_ID & 0xff, CommandFailure.class); // 3
        choiceOptions.addContextual(FloatingLimit.TYPE_ID & 0xff, FloatingLimit.class); // 4
        choiceOptions.addContextual(OutOfRange.TYPE_ID & 0xff, OutOfRange.class); // 5
        choiceOptions.addContextual(ChangeOfLifeSafety.TYPE_ID & 0xff, ChangeOfLifeSafety.class); // 8
        choiceOptions.addContextual(Extended.TYPE_ID & 0xff, Extended.class); // 9
        choiceOptions.addContextual(BufferReady.TYPE_ID & 0xff, BufferReady.class); // 10
        choiceOptions.addContextual(UnsignedRange.TYPE_ID & 0xff, UnsignedRange.class); // 11
        choiceOptions.addContextual(AccessEvent.TYPE_ID & 0xff, AccessEvent.class); // 13
        choiceOptions.addContextual(DoubleOutOfRange.TYPE_ID & 0xff, DoubleOutOfRange.class); // 14
        choiceOptions.addContextual(SignedOutOfRange.TYPE_ID & 0xff, SignedOutOfRange.class); // 15
        choiceOptions.addContextual(UnsignedOutOfRange.TYPE_ID & 0xff, UnsignedOutOfRange.class); // 16
        choiceOptions.addContextual(ChangeOfCharacterString.TYPE_ID & 0xff, ChangeOfCharacterString.class); // 17
        choiceOptions.addContextual(ChangeOfStatusFlags.TYPE_ID & 0xff, ChangeOfStatusFlags.class); // 18
        choiceOptions.addContextual(20, Null.class); // 20
        choiceOptions.addContextual(ChangeOfDiscreteValue.TYPE_ID & 0xff, ChangeOfDiscreteValue.class); // 21
        choiceOptions.addContextual(ChangeOfTimer.TYPE_ID & 0xff, ChangeOfTimer.class); // 22
    }

    private final Choice choice;

    public EventParameter(final AbstractEventParameter parameters) {
        choice = new Choice(choiceOptions.getContextId(parameters.getClass(), false), parameters, choiceOptions);
    }

    public EventParameter() {
        choice = new Choice(choiceOptions.getContextId(Null.instance.getClass(), false), Null.instance, choiceOptions);
    }
        
    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public EventParameter(final ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    public Choice getChoice() {
        return choice;
    }

    public EventType getEventType() {
        return EventType.forId(choice.getContextId());
    }

    @Override
    public String toString() {
        return "EventParameter[ choice=" + choice + ']';
    }
}
