
package com.renovo.bacnet4j.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.ResponseConsumer;
import com.renovo.bacnet4j.ServiceFuture;
import com.renovo.bacnet4j.apdu.Abort;
import com.renovo.bacnet4j.apdu.AckAPDU;
import com.renovo.bacnet4j.apdu.Reject;
import com.renovo.bacnet4j.exception.AbortAPDUException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetTimeoutException;
import com.renovo.bacnet4j.exception.ErrorAPDUException;
import com.renovo.bacnet4j.exception.RejectAPDUException;
import com.renovo.bacnet4j.exception.ServiceTooBigException;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.util.sero.ThreadUtils;

public class ServiceFutureImpl implements ServiceFuture, ResponseConsumer {
    static final Logger LOG = LoggerFactory.getLogger(ServiceFutureImpl.class);

    private AcknowledgementService ack;
    private AckAPDU fail;
    private BACnetException ex;
    private volatile boolean done;

    @Override
    public synchronized <T extends AcknowledgementService> T get() throws BACnetException {
        if (done) {
            return result();
        }

        ThreadUtils.wait(this);

        return result();
    }

    @SuppressWarnings("unchecked")
    private <T extends AcknowledgementService> T result() throws BACnetException {
        if (ex != null) {
            // We want to preserve the original type of the exception, but not have
            // to have a big if/then/else chain to handle all of the exception types.
            // Timeout is probably the only one most clients really care to handle,
            // so only that one is currently handled.
            if (ex instanceof BACnetTimeoutException) {
                throw new BACnetTimeoutException(ex.getMessage(), ex);
            }else if(ex instanceof ServiceTooBigException) {
                throw new ServiceTooBigException(ex.getMessage());
            }
            throw new BACnetException(ex.getMessage(), ex);
        }
        if (fail != null) {
            if (fail instanceof com.renovo.bacnet4j.apdu.Error)
                throw new ErrorAPDUException((com.renovo.bacnet4j.apdu.Error) fail);
            else if (fail instanceof Reject)
                throw new RejectAPDUException((Reject) fail);
            else if (fail instanceof Abort)
                throw new AbortAPDUException((Abort) fail);
        }
        return (T) ack;
    }

    @Override
    public synchronized void success(final AcknowledgementService ack) {
        this.ack = ack;
        complete();
    }

    @Override
    public synchronized void fail(final AckAPDU ack) {
        if (ack == null) {
            LOG.warn("ServiceFuture fail called with null argument", new Exception());
        }
        fail = ack;
        complete();
    }

    @Override
    public synchronized void ex(final BACnetException e) {
        if (e == null) {
            LOG.warn("ServiceFuture ex called with null argument", new Exception());
        }
        ex = e;
        complete();
    }

    private void complete() {
        done = true;
        notify();
    }
}
