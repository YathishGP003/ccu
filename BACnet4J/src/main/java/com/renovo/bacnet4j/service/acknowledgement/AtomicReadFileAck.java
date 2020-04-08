
package com.renovo.bacnet4j.service.acknowledgement;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.BaseType;
import com.renovo.bacnet4j.type.constructed.Choice;
import com.renovo.bacnet4j.type.constructed.ChoiceOptions;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.type.primitive.SignedInteger;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AtomicReadFileAck extends AcknowledgementService {
    public static final byte TYPE_ID = 6;

    private static final ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, StreamAccessAck.class);
        choiceOptions.addContextual(1, RecordAccessAck.class);
    }

    private final Boolean endOfFile;
    private final Choice accessMethod;

    public AtomicReadFileAck(final Boolean endOfFile, final StreamAccessAck streamAccess) {
        this.endOfFile = endOfFile;
        this.accessMethod = new Choice(0, streamAccess, choiceOptions);
    }

    public AtomicReadFileAck(final Boolean endOfFile, final RecordAccessAck recordAccess) {
        this.endOfFile = endOfFile;
        this.accessMethod = new Choice(1, recordAccess, choiceOptions);
    }

    public static int getHeaderSize() {
        return 3;
    }
    
    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, endOfFile);
        write(queue, accessMethod);
    }

    AtomicReadFileAck(final ByteQueue queue) throws BACnetException {
        endOfFile = read(queue, Boolean.class);
        accessMethod = readChoice(queue, choiceOptions);
    }

    public Boolean getEndOfFile() {
        return endOfFile;
    }

    public boolean isStreamAccess() {
        return accessMethod.getDatum() instanceof StreamAccessAck;
    }
    
    public StreamAccessAck getStreamAccess() {
        return accessMethod.getDatum();
    }

    public boolean isRecordAccess() {
        return accessMethod.getDatum() instanceof RecordAccessAck;
    }
    
    public RecordAccessAck getRecordAccess() {
        return accessMethod.getDatum();
    }

    @Override
    public String toString() {
        return "AtomicReadFileAck [endOfFile=" + endOfFile + ", accessMethod=" + accessMethod + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (accessMethod == null ? 0 : accessMethod.hashCode());
        result = prime * result + (endOfFile == null ? 0 : endOfFile.hashCode());
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
        final AtomicReadFileAck other = (AtomicReadFileAck) obj;
        if (accessMethod == null) {
            if (other.accessMethod != null)
                return false;
        } else if (!accessMethod.equals(other.accessMethod))
            return false;
        if (endOfFile == null) {
            if (other.endOfFile != null)
                return false;
        } else if (!endOfFile.equals(other.endOfFile))
            return false;
        return true;
    }

    public static class StreamAccessAck extends BaseType {
        private final SignedInteger fileStartPosition;
        private final OctetString fileData;

        public StreamAccessAck(final SignedInteger fileStartPosition, final OctetString fileData) {
            this.fileStartPosition = fileStartPosition;
            this.fileData = fileData;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, fileStartPosition);
            write(queue, fileData);
        }

        public StreamAccessAck(final ByteQueue queue) throws BACnetException {
            fileStartPosition = read(queue, SignedInteger.class);
            fileData = read(queue, OctetString.class);
        }

        public static int getHeaderSize() {
            return 5;
        }
                
        public SignedInteger getFileStartPosition() {
            return fileStartPosition;
        }

        public OctetString getFileData() {
            return fileData;
        }

        @Override
        public String toString() {
            return "StreamAccessAck [fileStartPosition=" + fileStartPosition + ", fileData=" + fileData + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (fileData == null ? 0 : fileData.hashCode());
            result = prime * result + (fileStartPosition == null ? 0 : fileStartPosition.hashCode());
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
            final StreamAccessAck other = (StreamAccessAck) obj;
            if (fileData == null) {
                if (other.fileData != null)
                    return false;
            } else if (!fileData.equals(other.fileData))
                return false;
            if (fileStartPosition == null) {
                if (other.fileStartPosition != null)
                    return false;
            } else if (!fileStartPosition.equals(other.fileStartPosition))
                return false;
            return true;
        }
    }

    public static class RecordAccessAck extends BaseType {
        private final SignedInteger fileStartRecord;
        private final UnsignedInteger returnedRecordCount;
        private final SequenceOf<OctetString> fileRecordData;

        public RecordAccessAck(final SignedInteger fileStartRecord, final UnsignedInteger returnedRecordCount,
                final SequenceOf<OctetString> fileRecordData) {
            this.fileStartRecord = fileStartRecord;
            this.returnedRecordCount = returnedRecordCount;
            this.fileRecordData = fileRecordData;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, fileStartRecord);
            write(queue, returnedRecordCount);
            write(queue, fileRecordData);
        }

        public RecordAccessAck(final ByteQueue queue) throws BACnetException {
            fileStartRecord = read(queue, SignedInteger.class);
            returnedRecordCount = read(queue, UnsignedInteger.class);
            fileRecordData = readSequenceOf(queue, returnedRecordCount.intValue(), OctetString.class);
        }

        public SignedInteger getFileStartRecord() {
            return fileStartRecord;
        }

        public UnsignedInteger getReturnedRecordCount() {
            return returnedRecordCount;
        }

        public SequenceOf<OctetString> getFileRecordData() {
            return fileRecordData;
        }

        @Override
        public String toString() {
            return "RecordAccessAck [fileStartRecord=" + fileStartRecord + ", returnedRecordCount="
                    + returnedRecordCount + ", fileRecordData=" + fileRecordData + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (fileRecordData == null ? 0 : fileRecordData.hashCode());
            result = prime * result + (fileStartRecord == null ? 0 : fileStartRecord.hashCode());
            result = prime * result + (returnedRecordCount == null ? 0 : returnedRecordCount.hashCode());
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
            final RecordAccessAck other = (RecordAccessAck) obj;
            if (fileRecordData == null) {
                if (other.fileRecordData != null)
                    return false;
            } else if (!fileRecordData.equals(other.fileRecordData))
                return false;
            if (fileStartRecord == null) {
                if (other.fileStartRecord != null)
                    return false;
            } else if (!fileStartRecord.equals(other.fileStartRecord))
                return false;
            if (returnedRecordCount == null) {
                if (other.returnedRecordCount != null)
                    return false;
            } else if (!returnedRecordCount.equals(other.returnedRecordCount))
                return false;
            return true;
        }
    }
}
