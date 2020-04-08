
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
import com.renovo.bacnet4j.service.acknowledgement.AtomicReadFileAck;
import com.renovo.bacnet4j.service.acknowledgement.AtomicReadFileAck.RecordAccessAck;
import com.renovo.bacnet4j.service.acknowledgement.AtomicReadFileAck.StreamAccessAck;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.BaseType;
import com.renovo.bacnet4j.type.constructed.Choice;
import com.renovo.bacnet4j.type.constructed.ChoiceOptions;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.enumerated.BackupState;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.type.primitive.SignedInteger;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AtomicReadFileRequest extends ConfirmedRequestService {
    static final Logger LOG = LoggerFactory.getLogger(AtomicReadFileRequest.class);

    public static final byte TYPE_ID = 6;

    private static final ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, StreamAccess.class);
        choiceOptions.addContextual(1, RecordAccess.class);
    }

    private final ObjectIdentifier fileIdentifier;
    private final Choice accessMethod;

    public AtomicReadFileRequest(final ObjectIdentifier fileIdentifier, final StreamAccess streamAccess) {
        this.fileIdentifier = fileIdentifier;
        this.accessMethod = new Choice(0, streamAccess, choiceOptions);
    }

    public AtomicReadFileRequest(final ObjectIdentifier fileIdentifier, final RecordAccess recordAccess) {
        this.fileIdentifier = fileIdentifier;
        this.accessMethod = new Choice(1, recordAccess, choiceOptions);
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

    AtomicReadFileRequest(final ByteQueue queue) throws BACnetException {
        fileIdentifier = read(queue, ObjectIdentifier.class);
        accessMethod = readChoice(queue, choiceOptions);
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final AtomicReadFileAck response;

        try {
            // Find the file.
            final BACnetObject obj = localDevice.getObjectRequired(fileIdentifier);
            if (!(obj instanceof FileObject)) {
                throw new BACnetServiceException(ErrorClass.services, ErrorCode.inconsistentObjectType);
            }

            // Check for status (backup/restore)
            final BackupState bsOld = (BackupState) localDevice.get(PropertyIdentifier.backupAndRestoreState);
            if (bsOld.intValue() == BackupState.preparingForBackup.intValue()
                    || bsOld.intValue() == BackupState.preparingForRestore.intValue())
                // Send error: device configuration in progress as response
                throw new BACnetServiceException(ErrorClass.device, ErrorCode.configurationInProgress);

            final FileObject file = (FileObject) obj;

            // Lock to ensure atomicity.
            try {
                file.getLock().lock();
                final FileAccess fileAccess = file.getFileAccess();

                if (accessMethod.isa(StreamAccess.class)) {
                    if (!fileAccess.supportsStreamAccess()) {
                        throw new BACnetServiceException(ErrorClass.services, ErrorCode.invalidFileAccessMethod);
                    }

                    final StreamAccess streamAccess = accessMethod.getDatum();
                    final long start = streamAccess.getFileStartPosition().longValue();
                    final long readLength = streamAccess.getRequestedOctetCount().longValue();
                    final long fileLength = fileAccess.length();

                    // Throw an exception when the following conditions are met
                    //   - start is a negative number
                    //   - start exceeds the length of the file object
                    if (start < 0 || start >= fileLength) {
                        throw new BACnetServiceException(ErrorClass.object, ErrorCode.invalidFileStartPosition);
                    }

                    final OctetString result = fileAccess.readData(start, readLength);

                    response = new AtomicReadFileAck(Boolean.valueOf(fileLength <= start + readLength),
                            new StreamAccessAck(streamAccess.getFileStartPosition(), result));
                } else if (accessMethod.isa(RecordAccess.class)) {
                    if (!fileAccess.supportsRecordAccess()) {
                        throw new BACnetServiceException(ErrorClass.services, ErrorCode.invalidFileAccessMethod);
                    }

                    final RecordAccess recordAccess = accessMethod.getDatum();
                    final long start = recordAccess.getFileStartRecord().longValue();
                    final long readCount = recordAccess.getRequestedRecordCount().longValue();
                    final long fileCount = fileAccess.recordCount();

                    // Throw an exception when the following conditions are met
                    //   - start is a negative number
                    //   - start exceeds the length of the file object
                    if (start < 0 || start >= fileAccess.recordCount()) {
                        throw new BACnetServiceException(ErrorClass.object, ErrorCode.invalidFileStartPosition);
                    }

                    final SequenceOf<OctetString> result = fileAccess.readRecords(start,
                            recordAccess.getRequestedRecordCount().longValue());

                    response = new AtomicReadFileAck(Boolean.valueOf(fileCount <= start + readCount),
                            new RecordAccessAck(recordAccess.getFileStartRecord(), new UnsignedInteger(result.size()),
                                    result));
                } else {
                    // Should not happen
                    throw new RuntimeException("Not implemented: " + accessMethod.getDatum());
                }
            } finally {
                file.getLock().unlock();
            }
        } catch (final IOException e) {
            LOG.error("File read failed for {}", this, e);
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
        final AtomicReadFileRequest other = (AtomicReadFileRequest) obj;
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
        private final UnsignedInteger requestedOctetCount;

        public StreamAccess(final SignedInteger fileStartPosition, final UnsignedInteger requestedOctetCount) {
            this.fileStartPosition = fileStartPosition;
            this.requestedOctetCount = requestedOctetCount;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, fileStartPosition);
            write(queue, requestedOctetCount);
        }

        public StreamAccess(final ByteQueue queue) throws BACnetException {
            fileStartPosition = read(queue, SignedInteger.class);
            requestedOctetCount = read(queue, UnsignedInteger.class);
        }

        public SignedInteger getFileStartPosition() {
            return fileStartPosition;
        }

        public UnsignedInteger getRequestedOctetCount() {
            return requestedOctetCount;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (fileStartPosition == null ? 0 : fileStartPosition.hashCode());
            result = prime * result + (requestedOctetCount == null ? 0 : requestedOctetCount.hashCode());
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
            if (fileStartPosition == null) {
                if (other.fileStartPosition != null)
                    return false;
            } else if (!fileStartPosition.equals(other.fileStartPosition))
                return false;
            if (requestedOctetCount == null) {
                if (other.requestedOctetCount != null)
                    return false;
            } else if (!requestedOctetCount.equals(other.requestedOctetCount))
                return false;
            return true;
        }
    }

    public static class RecordAccess extends BaseType {
        private final SignedInteger fileStartRecord;
        private final UnsignedInteger requestedRecordCount;

        public RecordAccess(final SignedInteger fileStartRecord, final UnsignedInteger requestedRecordCount) {
            this.fileStartRecord = fileStartRecord;
            this.requestedRecordCount = requestedRecordCount;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, fileStartRecord);
            write(queue, requestedRecordCount);
        }

        public RecordAccess(final ByteQueue queue) throws BACnetException {
            fileStartRecord = read(queue, SignedInteger.class);
            requestedRecordCount = read(queue, UnsignedInteger.class);
        }

        public SignedInteger getFileStartRecord() {
            return fileStartRecord;
        }

        public UnsignedInteger getRequestedRecordCount() {
            return requestedRecordCount;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (fileStartRecord == null ? 0 : fileStartRecord.hashCode());
            result = prime * result + (requestedRecordCount == null ? 0 : requestedRecordCount.hashCode());
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
            if (fileStartRecord == null) {
                if (other.fileStartRecord != null)
                    return false;
            } else if (!fileStartRecord.equals(other.fileStartRecord))
                return false;
            if (requestedRecordCount == null) {
                if (other.requestedRecordCount != null)
                    return false;
            } else if (!requestedRecordCount.equals(other.requestedRecordCount))
                return false;
            return true;
        }
    }
}
