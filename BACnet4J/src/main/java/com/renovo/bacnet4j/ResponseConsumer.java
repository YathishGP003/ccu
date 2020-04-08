
package com.renovo.bacnet4j;

import com.renovo.bacnet4j.apdu.AckAPDU;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;

public interface ResponseConsumer {
    void success(AcknowledgementService ack);

    void fail(AckAPDU ack);

    void ex(BACnetException e);
}
