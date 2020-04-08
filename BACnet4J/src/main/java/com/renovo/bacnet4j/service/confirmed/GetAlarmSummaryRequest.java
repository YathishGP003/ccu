
package com.renovo.bacnet4j.service.confirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.service.acknowledgement.GetAlarmSummaryAck;
import com.renovo.bacnet4j.service.acknowledgement.GetAlarmSummaryAck.AlarmSummary;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class GetAlarmSummaryRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 3;

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    public GetAlarmSummaryRequest() {
        // no op
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final SequenceOf<AlarmSummary> summaries = new SequenceOf<>();

        for (final BACnetObject bo : localDevice.getLocalObjects()) {
            final AlarmSummary alarmSummary = bo.getAlarmSummary();
            if (alarmSummary != null)
                summaries.add(alarmSummary);
        }

        return new GetAlarmSummaryAck(summaries);
    }

    @Override
    public void write(final ByteQueue queue) {
        // no op
    }

    GetAlarmSummaryRequest(@SuppressWarnings("unused") final ByteQueue queue) {
        // no op
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
