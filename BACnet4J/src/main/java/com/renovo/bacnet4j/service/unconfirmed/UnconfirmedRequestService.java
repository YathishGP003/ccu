
package com.renovo.bacnet4j.service.unconfirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.service.Service;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.ServicesSupported;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.util.sero.ByteQueue;

abstract public class UnconfirmedRequestService extends Service {
    public static void checkUnconfirmedRequestService(final ServicesSupported services, final byte type)
            throws BACnetException {
        if (type == IAmRequest.TYPE_ID && services.isIAm()) // 0
            return;
        if (type == IHaveRequest.TYPE_ID && services.isIHave()) // 1
            return;
        if (type == UnconfirmedCovNotificationRequest.TYPE_ID && services.isUnconfirmedCovNotification()) // 2
            return;
        if (type == UnconfirmedEventNotificationRequest.TYPE_ID && services.isUnconfirmedEventNotification()) // 3
            return;
        if (type == UnconfirmedPrivateTransferRequest.TYPE_ID && services.isUnconfirmedPrivateTransfer()) // 4
            return;
        if (type == UnconfirmedTextMessageRequest.TYPE_ID && services.isUnconfirmedTextMessage()) // 5
            return;
        if (type == TimeSynchronizationRequest.TYPE_ID && services.isTimeSynchronization()) // 6
            return;
        if (type == WhoHasRequest.TYPE_ID && services.isWhoHas()) // 7
            return;
        if (type == WhoIsRequest.TYPE_ID && services.isWhoIs()) // 8
            return;
        if (type == UTCTimeSynchronizationRequest.TYPE_ID && services.isUtcTimeSynchronization()) // 9
            return;
        if (type == WriteGroupRequest.TYPE_ID && services.isWriteGroup()) // 10
            return;
        if (type == UnconfirmedCovNotificationMultipleRequest.TYPE_ID
                && services.isUnconfirmedCovNotificationMultiple()) // 11
            return;

        throw new BACnetErrorException(ErrorClass.device, ErrorCode.serviceRequestDenied);
    }

    public static UnconfirmedRequestService createUnconfirmedRequestService(final byte type, final ByteQueue queue)
            throws BACnetException {
        if (type == IAmRequest.TYPE_ID)
            return new IAmRequest(queue);
        if (type == IHaveRequest.TYPE_ID)
            return new IHaveRequest(queue);
        if (type == UnconfirmedCovNotificationRequest.TYPE_ID)
            return new UnconfirmedCovNotificationRequest(queue);
        if (type == UnconfirmedEventNotificationRequest.TYPE_ID)
            return new UnconfirmedEventNotificationRequest(queue);
        if (type == UnconfirmedPrivateTransferRequest.TYPE_ID)
            return new UnconfirmedPrivateTransferRequest(queue);
        if (type == UnconfirmedTextMessageRequest.TYPE_ID)
            return new UnconfirmedTextMessageRequest(queue);
        if (type == TimeSynchronizationRequest.TYPE_ID)
            return new TimeSynchronizationRequest(queue);
        if (type == WhoHasRequest.TYPE_ID)
            return new WhoHasRequest(queue);
        if (type == WhoIsRequest.TYPE_ID)
            return new WhoIsRequest(queue);
        if (type == UTCTimeSynchronizationRequest.TYPE_ID)
            return new UTCTimeSynchronizationRequest(queue);
        if (type == WriteGroupRequest.TYPE_ID)
            return new WriteGroupRequest(queue);
        if (type == UnconfirmedCovNotificationMultipleRequest.TYPE_ID)
            return new UnconfirmedCovNotificationMultipleRequest(queue);

        throw new BACnetException("Unsupported unconfirmed service: " + (type & 0xff));
    }

    abstract public void handle(LocalDevice localDevice, Address from) throws BACnetException;
}
