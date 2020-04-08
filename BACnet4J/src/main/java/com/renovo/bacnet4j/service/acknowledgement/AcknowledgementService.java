
package com.renovo.bacnet4j.service.acknowledgement;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.service.Service;
import com.renovo.bacnet4j.util.sero.ByteQueue;

abstract public class AcknowledgementService extends Service {
    public static byte typeofAck;
    public static AcknowledgementService createAcknowledgementService(final byte type, final ByteQueue queue)
            throws BACnetException {
        typeofAck = type;
        if (type == GetAlarmSummaryAck.TYPE_ID) // 3
            return new GetAlarmSummaryAck(queue);
        if (type == GetEnrollmentSummaryAck.TYPE_ID) // 4
            return new GetEnrollmentSummaryAck(queue);
        if (type == AtomicReadFileAck.TYPE_ID) // 6
            return new AtomicReadFileAck(queue);
        if (type == AtomicWriteFileAck.TYPE_ID) // 7
            return new AtomicWriteFileAck(queue);
        if (type == CreateObjectAck.TYPE_ID) // 10
            return new CreateObjectAck(queue);
        if (type == ReadPropertyAck.TYPE_ID) // 12
            return new ReadPropertyAck(queue);
        if (type == ReadPropertyMultipleAck.TYPE_ID) // 14
            return new ReadPropertyMultipleAck(queue);
        if (type == ConfirmedPrivateTransferAck.TYPE_ID) // 18
            return new ConfirmedPrivateTransferAck(queue);
        if (type == VtOpenAck.TYPE_ID) // 21
            return new VtOpenAck(queue);
        if (type == VtDataAck.TYPE_ID) // 23
            return new VtDataAck(queue);
        if (type == ReadRangeAck.TYPE_ID) // 26
            return new ReadRangeAck(queue);
        if (type == GetEventInformationAck.TYPE_ID) // 29
            return new GetEventInformationAck(queue);

        throw new BACnetException("Unsupported service acknowledgement: " + (type & 0xff));
    }

    public static byte getTypeofAck() {
        return typeofAck;
    }
}
