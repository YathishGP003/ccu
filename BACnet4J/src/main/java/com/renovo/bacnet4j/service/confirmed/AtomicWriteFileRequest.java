
package com.renovo.bacnet4j.service.confirmed;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.FileObject;
import com.renovo.bacnet4j.obj.fileAccess.FileAccess;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.service.acknowledgement.AtomicWriteFileAck;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.BaseType;
import com.renovo.bacnet4j.type.constructed.Choice;
import com.renovo.bacnet4j.type.constructed.ChoiceOptions;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.type.primitive.SignedInteger;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AtomicWriteFileRequest extends ConfirmedRequestService {
    static final Logger LOG = LoggerFactory.getLogger(AtomicWriteFileRequest.class);

    public static final byte TYPE_ID = 7;

    private static final ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, StreamAccess.class);
        choiceOptions.addContextual(1, RecordAccess.class);
    }

    private final ObjectIdentifier fileIdentifier;
    private final Choice accessMethod;

    public AtomicWriteFileRequest(final ObjectIdentifier fileIdentifier, final StreamAccess streamAccess) {
        this.fileIdentifier = fileIdentifier;
        this.accessMethod = new Choice(0, streamAccess, choiceOptions);
    }

    public AtomicWriteFileRequest(final ObjectIdentifier fileIdentifier, final RecordAccess recordAccess) {
        this.fileIdentifier = fileIdentifier;
        this.accessMethod = new Choice(1, recordAccess, choiceOptions);
    }

    AtomicWriteFileRequest(final ByteQueue queue) throws BACnetException {
        fileIdentifier = read(queue, ObjectIdentifier.class);
        accessMethod = readChoice(queue, choiceOptions);
    }
    
    public ObjectIdentifier getFileIdentifier() {
        return this.fileIdentifier;
    }

    public boolean isRecordAccess() {
        return accessMethod.getDatum() instanceof RecordAccess;
    }
    
    public RecordAccess getRecordAccess() {
        return accessMethod.getDatum();
    }
    
    public boolean isStreamAccess() {
        return accessMethod.getDatum() instanceof StreamAccess;
    }
    
    public StreamAccess getStreamAccess() {
        return accessMethod.getDatum();
    }
    
    @Override
    public void write(final ByteQueue queue) {
        write(queue, fileIdentifier);
        write(queue, accessMethod);
    }

    public static int getHeaderSize() {
        return 7;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final AtomicWriteFileAck response;

        try {
            // Find the file.
            final BACnetObject obj = localDevice.getObjectRequired(fileIdentifier);
            if (!(obj instanceof FileObject)) {
                throw new BACnetServiceException(ErrorClass.services, ErrorCode.inconsistentObjectType);
            }

            final FileObject file = (FileObject) obj;

            // Lock to ensure atomicity.
            try {
                file.getLock().lock();
                final FileAccess fileAccess = file.getFileAccess();

                if (!fileAccess.canWrite())
                    throw new BACnetServiceException(ErrorClass.services, ErrorCode.fileAccessDenied);

                if (accessMethod.isa(StreamAccess.class)) {
                    if (!fileAccess.supportsStreamAccess()) {
                        throw new BACnetServiceException(ErrorClass.services, ErrorCode.invalidFileAccessMethod);
                    }

                    final StreamAccess streamAccess = accessMethod.getDatum();
                    final long start = streamAccess.getFileStartPosition().longValue();
                    final OctetString data = streamAccess.getFileData();

                    if (start < -1) {
                        throw new BACnetErrorException(getChoiceId(), ErrorClass.object,
                                ErrorCode.invalidFileStartPosition);
                    }

                    final long actualStart = fileAccess.writeData(start, data);

                    response = new AtomicWriteFileAck(false, new SignedInteger(actualStart));
                } else if (accessMethod.isa(RecordAccess.class)) {
                    if (!fileAccess.supportsRecordAccess()) {
                        throw new BACnetServiceException(ErrorClass.services, ErrorCode.invalidFileAccessMethod);
                    }

                    final RecordAccess recordAccess = accessMethod.getDatum();
                    final long start = recordAccess.getFileStartRecord().longValue();
                    final SequenceOf<OctetString> records = recordAccess.getFileRecordData();

                    if (start < -1) {
                        throw new BACnetErrorException(getChoiceId(), ErrorClass.object,
                                ErrorCode.invalidFileStartPosition);
                    }

                    final long actualStart = fileAccess.writeRecords(start, records);

                    response = new AtomicWriteFileAck(true, new SignedInteger(actualStart));
                } else {
                    // Should not happen
                    throw new RuntimeException("Not implemented: " + accessMethod.getDatum());
                }
            } finally {
                file.getLock().unlock();
            }
        } catch (final IOException e) {
            LOG.error("File write failed for {}", this, e);
            throw new BACnetErrorException(getChoiceId(), ErrorClass.object, ErrorCode.fileAccessDenied);
        } catch (final BACnetServiceException e) {
            throw new BACnetErrorException(getChoiceId(), e);
        }

        return response;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (accessMethod == null ? 0 : accessMethod.hashCode());
        result = prime * result + (fileIdentifier == null ? 0 : fileIdentifier.hashCode());
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
        final AtomicWriteFileRequest other = (AtomicWriteFileRequest) obj;
        if (accessMethod == null) {
            if (other.accessMethod != null)
                return false;
        } else if (!accessMethod.equals(other.accessMethod))
            return false;
        if (fileIdentifier == null) {
            if (other.fileIdentifier != null)
                return false;
        } else if (!fileIdentifier.equals(other.fileIdentifier))
            return false;
        return true;
    }

    public static class StreamAccess extends BaseType {
        private final SignedInteger fileStartPosition;
        private final OctetString fileData;

        public StreamAccess(final SignedInteger fileStartPosition, final OctetString fileData) {
            this.fileStartPosition = fileStartPosition;
            this.fileData = fileData;
        }

        public StreamAccess(final ByteQueue queue) throws BACnetException {
            fileStartPosition = read(queue, SignedInteger.class);
            fileData = read(queue, OctetString.class);
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, fileStartPosition);
            write(queue, fileData);
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
            final StreamAccess other = (StreamAccess) obj;
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

    public static class RecordAccess extends BaseType {
        private final SignedInteger fileStartRecord;
        private final UnsignedInteger recordCount;
        private final SequenceOf<OctetString> fileRecordData;

        public RecordAccess(final SignedInteger fileStartRecord, final UnsignedInteger recordCount,
                final SequenceOf<OctetString> fileRecordData) {
            this.fileStartRecord = fileStartRecord;
            this.recordCount = recordCount;
            this.fileRecordData = fileRecordData;
        }

        public RecordAccess(final ByteQueue queue) throws BACnetException {
            fileStartRecord = read(queue, SignedInteger.class);
            recordCount = read(queue, UnsignedInteger.class);
            fileRecordData = readSequenceOf(queue, recordCount.intValue(), OctetString.class);
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, fileStartRecord);
            write(queue, recordCount);
            write(queue, fileRecordData);
        }

        public SignedInteger getFileStartRecord() {
            return fileStartRecord;
        }

        public UnsignedInteger getRecordCount() {
            return recordCount;
        }

        public SequenceOf<OctetString> getFileRecordData() {
            return fileRecordData;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (fileRecordData == null ? 0 : fileRecordData.hashCode());
            result = prime * result + (fileStartRecord == null ? 0 : fileStartRecord.hashCode());
            result = prime * result + (recordCount == null ? 0 : recordCount.hashCode());
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
            final RecordAccess other = (RecordAccess) obj;
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
            if (recordCount == null) {
                if (other.recordCount != null)
                    return false;
            } else if (!recordCount.equals(other.recordCount))
                return false;
            return true;
        }
    }
}
