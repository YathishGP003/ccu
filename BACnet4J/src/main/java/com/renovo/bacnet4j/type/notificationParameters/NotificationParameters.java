
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.BaseType;
import com.renovo.bacnet4j.type.constructed.Choice;
import com.renovo.bacnet4j.type.constructed.ChoiceOptions;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class NotificationParameters extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(ChangeOfBitStringNotif.TYPE_ID & 0xff, ChangeOfBitStringNotif.class); // 0
        choiceOptions.addContextual(ChangeOfStateNotif.TYPE_ID & 0xff, ChangeOfStateNotif.class); // 1
        choiceOptions.addContextual(ChangeOfValueNotif.TYPE_ID & 0xff, ChangeOfValueNotif.class); // 2
        choiceOptions.addContextual(CommandFailureNotif.TYPE_ID & 0xff, CommandFailureNotif.class); // 3
        choiceOptions.addContextual(FloatingLimitNotif.TYPE_ID & 0xff, FloatingLimitNotif.class); // 4
        choiceOptions.addContextual(OutOfRangeNotif.TYPE_ID & 0xff, OutOfRangeNotif.class); // 5
        choiceOptions.addContextual(ComplexEventTypeNotif.TYPE_ID & 0xff, ComplexEventTypeNotif.class); // 6
        choiceOptions.addContextual(ChangeOfLifeSafetyNotif.TYPE_ID & 0xff, ChangeOfLifeSafetyNotif.class); // 8
        choiceOptions.addContextual(ExtendedNotif.TYPE_ID & 0xff, ExtendedNotif.class); // 9
        choiceOptions.addContextual(BufferReadyNotif.TYPE_ID & 0xff, BufferReadyNotif.class); // 10
        choiceOptions.addContextual(UnsignedRangeNotif.TYPE_ID & 0xff, UnsignedRangeNotif.class); // 11
        choiceOptions.addContextual(AccessEventNotif.TYPE_ID & 0xff, AccessEventNotif.class); // 13
        choiceOptions.addContextual(DoubleOutOfRangeNotif.TYPE_ID & 0xff, DoubleOutOfRangeNotif.class); // 14
        choiceOptions.addContextual(SignedOutOfRangeNotif.TYPE_ID & 0xff, SignedOutOfRangeNotif.class); // 15
        choiceOptions.addContextual(UnsignedOutOfRangeNotif.TYPE_ID & 0xff, UnsignedOutOfRangeNotif.class); // 16
        choiceOptions.addContextual(ChangeOfCharacterStringNotif.TYPE_ID & 0xff, ChangeOfCharacterStringNotif.class); // 17
        choiceOptions.addContextual(ChangeOfStatusFlagsNotif.TYPE_ID & 0xff, ChangeOfStatusFlagsNotif.class); // 18
        choiceOptions.addContextual(ChangeOfReliabilityNotif.TYPE_ID & 0xff, ChangeOfReliabilityNotif.class); // 19
        choiceOptions.addContextual(ChangeOfDiscreteValueNotif.TYPE_ID & 0xff, ChangeOfDiscreteValueNotif.class); // 21
        choiceOptions.addContextual(ChangeOfTimerNotif.TYPE_ID & 0xff, ChangeOfTimerNotif.class); // 22
    }

    private final Choice choice;

    public NotificationParameters(final AbstractNotificationParameter parameters) {
        choice = new Choice(choiceOptions.getContextId(parameters.getClass(), false), parameters, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public NotificationParameters(final ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractNotificationParameter> T getParameter() {
        return (T) choice.getDatum();
    }

    @Override
    public String toString() {
        return "NotificationParameters [choice=" + choice + "]";
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
        final NotificationParameters other = (NotificationParameters) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }
}
