
package com.renovo.bacnet4j.exception;

import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.error.BACnetError;
import com.renovo.bacnet4j.type.error.BaseError;
import com.renovo.bacnet4j.type.error.ErrorClassAndCode;

public class BACnetErrorException extends BACnetException {
    private static final long serialVersionUID = -1;

    private final BACnetError bacnetError;

    public BACnetErrorException(final byte choice, final BaseError baseError) {
        super(getBaseMessage(baseError.getErrorClassAndCode().getErrorClass(),
                baseError.getErrorClassAndCode().getErrorCode(), null));
        bacnetError = new BACnetError(choice, baseError);
    }

    public BACnetErrorException(final byte choice, final BACnetErrorException cause) {
        super(cause.getMessage(), cause);
        bacnetError = new BACnetError(choice, cause.getBacnetError().getError());
    }

    public BACnetErrorException(final byte choice, final ErrorClass errorClass, final ErrorCode errorCode) {
        super(getBaseMessage(errorClass, errorCode, null));
        bacnetError = new BACnetError(choice, new ErrorClassAndCode(errorClass, errorCode));
    }

    public BACnetErrorException(final byte choice, final BACnetServiceException e) {
        super(e);
        bacnetError = new BACnetError(choice, new ErrorClassAndCode(e.getErrorClass(), e.getErrorCode()));
    }

    public BACnetErrorException(final ErrorClass errorClass, final ErrorCode errorCode) {
        super(getBaseMessage(errorClass, errorCode, null));
        bacnetError = new BACnetError(127, new ErrorClassAndCode(errorClass, errorCode));
    }

    public BACnetErrorException(final BACnetServiceException e) {
        super(e.getMessage());
        bacnetError = new BACnetError(127, new ErrorClassAndCode(e.getErrorClass(), e.getErrorCode()));
    }

    public BACnetErrorException(final ErrorClass errorClass, final ErrorCode errorCode, final String message) {
        super(getBaseMessage(errorClass, errorCode, message));
        bacnetError = new BACnetError(127, new ErrorClassAndCode(errorClass, errorCode));
    }

    public BACnetErrorException(final BACnetError bacnetError) {
        this.bacnetError = bacnetError;
    }

    public BACnetError getBacnetError() {
        return bacnetError;
    }

    private static String getBaseMessage(final ErrorClass errorClass, final ErrorCode errorCode, final String message) {
        final StringBuilder sb = new StringBuilder();
        sb.append(errorClass.toString());
        sb.append(": ");
        sb.append(errorCode.toString());
        if (message != null)
            sb.append(" '").append(message).append("'");
        return sb.toString();
    }
}
