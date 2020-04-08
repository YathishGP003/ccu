
package com.renovo.bacnet4j.exception;

import com.renovo.bacnet4j.apdu.Error;
import com.renovo.bacnet4j.type.error.ErrorClassAndCode;

public class ErrorAPDUException extends BACnetException {
    private static final long serialVersionUID = -1;

    private final Error apdu;

    public ErrorAPDUException(final Error apdu) {
        super(apdu.toString());
        this.apdu = apdu;
    }

    public Error getApdu() {
        return apdu;
    }

    public ErrorClassAndCode getError() {
        return apdu.getError().getErrorClassAndCode();
    }
}
