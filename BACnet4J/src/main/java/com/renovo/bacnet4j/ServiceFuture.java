
package com.renovo.bacnet4j;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;

public interface ServiceFuture {
    <T extends AcknowledgementService> T get() throws BACnetException;
}
