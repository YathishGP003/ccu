
package com.renovo.bacnet4j.exception;

public class BACnetTimeoutException extends BACnetException {
    private static final long serialVersionUID = -1;

    public BACnetTimeoutException() {
        super();
    }

    public BACnetTimeoutException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public BACnetTimeoutException(final String message) {
        super(message);
    }

    public BACnetTimeoutException(final Throwable cause) {
        super(cause);
    }
}
