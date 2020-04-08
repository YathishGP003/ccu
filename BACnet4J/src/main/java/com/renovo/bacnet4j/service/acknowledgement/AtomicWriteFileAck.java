
package com.renovo.bacnet4j.service.acknowledgement;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.SignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AtomicWriteFileAck extends AcknowledgementService {
    public static final byte TYPE_ID = 7;

    private final boolean recordAccess;
    private final SignedInteger fileStart;

    public AtomicWriteFileAck(final boolean recordAccess, final SignedInteger fileStart) {
        this.recordAccess = recordAccess;
        this.fileStart = fileStart;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, fileStart, recordAccess ? 1 : 0);
    }

    AtomicWriteFileAck(final ByteQueue queue) throws BACnetException {
        recordAccess = peekTagNumber(queue) == 1;
        fileStart = read(queue, SignedInteger.class, recordAccess ? 1 : 0);
    }

    public boolean isRecordAccess() {
        return recordAccess;
    }

    public SignedInteger getFileStart() {
        return fileStart;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (fileStart == null ? 0 : fileStart.hashCode());
        result = PRIME * result + (recordAccess ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final AtomicWriteFileAck other = (AtomicWriteFileAck) obj;
        if (fileStart == null) {
            if (other.fileStart != null)
                return false;
        } else if (!fileStart.equals(other.fileStart))
            return false;
        if (recordAccess != other.recordAccess)
            return false;
        return true;
    }
}
