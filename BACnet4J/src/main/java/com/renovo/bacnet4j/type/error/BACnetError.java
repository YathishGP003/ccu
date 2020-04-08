
package com.renovo.bacnet4j.type.error;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.BaseType;
import com.renovo.bacnet4j.type.constructed.Choice;
import com.renovo.bacnet4j.type.constructed.ChoiceOptions;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class BACnetError extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(127, ErrorClassAndCode.class);
        choiceOptions.addContextual(0, ErrorClassAndCode.class);
        choiceOptions.addContextual(1, ErrorClassAndCode.class);
        choiceOptions.addContextual(31, ErrorClassAndCode.class);
        choiceOptions.addContextual(2, ErrorClassAndCode.class);
        choiceOptions.addContextual(3, ErrorClassAndCode.class);
        choiceOptions.addContextual(4, ErrorClassAndCode.class);
        choiceOptions.addContextual(29, ErrorClassAndCode.class);
        choiceOptions.addContextual(27, ErrorClassAndCode.class);
        choiceOptions.addContextual(5, ErrorClassAndCode.class);
        choiceOptions.addContextual(28, ErrorClassAndCode.class);
        choiceOptions.addContextual(30, SubscribeCovPropertyMultipleError.class);
        choiceOptions.addContextual(6, ErrorClassAndCode.class);
        choiceOptions.addContextual(7, ErrorClassAndCode.class);
        choiceOptions.addContextual(8, ChangeListError.class);
        choiceOptions.addContextual(9, ChangeListError.class);
        choiceOptions.addContextual(10, CreateObjectError.class);
        choiceOptions.addContextual(11, ErrorClassAndCode.class);
        choiceOptions.addContextual(12, ErrorClassAndCode.class);
        choiceOptions.addContextual(14, ErrorClassAndCode.class);
        choiceOptions.addContextual(26, ErrorClassAndCode.class);
        choiceOptions.addContextual(15, ErrorClassAndCode.class);
        choiceOptions.addContextual(16, WritePropertyMultipleError.class);
        choiceOptions.addContextual(17, ErrorClassAndCode.class);
        choiceOptions.addContextual(18, ConfirmedPrivateTransferError.class);
        choiceOptions.addContextual(19, ErrorClassAndCode.class);
        choiceOptions.addContextual(20, ErrorClassAndCode.class);
        choiceOptions.addContextual(21, ErrorClassAndCode.class);
        choiceOptions.addContextual(22, VTCloseError.class);
        choiceOptions.addContextual(23, ErrorClassAndCode.class);
    }

    private final Choice choice;

    public BACnetError(final int contextId, final BaseError error) {
        choice = new Choice(contextId, error, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public BACnetError(final ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    public int getChoice() {
        return choice.getContextId();
    }

    public BaseError getError() {
        return choice.getDatum();
    }
}
