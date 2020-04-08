
package com.renovo.bacnet4j.obj;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetRuntimeException;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.fileAccess.FileAccess;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.DateTime;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

/**
 * @author Suresh Kumar
 */
public class FileObject extends BACnetObject {
    /**
     * The actual file that this object represents.
     */
    private FileAccess fileAccess;

    /**
     * The lock is used by AtomicReadFileRequest and AtomicWriteFileRequest services to lock the file object during
     * the course of the service, thus ensuring the "atomic" thing.
     */
    private final ReentrantLock lock = new ReentrantLock();

    public FileObject(final LocalDevice localDevice, final int instanceNumber, final String fileType,
            final FileAccess fileAccess) throws BACnetServiceException {
        super(localDevice, ObjectType.file, instanceNumber, fileAccess.getName());
        this.fileAccess = fileAccess;

        if (!fileAccess.exists())
            throw new BACnetRuntimeException("File does not exist");
        if (fileAccess.isDirectory())
            throw new BACnetRuntimeException("File is a directory");

        Objects.requireNonNull(fileType);
        Objects.requireNonNull(fileAccess);

        writePropertyInternal(PropertyIdentifier.fileType, new CharacterString(fileType));
        writePropertyInternal(PropertyIdentifier.fileAccessMethod, fileAccess.getAccessMethod());
        writePropertyInternal(PropertyIdentifier.archive, Boolean.FALSE);

        localDevice.addObject(this);
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public FileAccess getFileAccess() {
        return fileAccess;
    }

    public void setFileAccess(final FileAccess fileAccess) {
        this.fileAccess = fileAccess;
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) throws BACnetServiceException {
        if (PropertyIdentifier.fileSize.equals(pid)) {
            set(PropertyIdentifier.fileSize, new UnsignedInteger(fileAccess.length()));
        } else if (PropertyIdentifier.modificationDate.equals(pid)) {
            set(PropertyIdentifier.modificationDate, new DateTime(fileAccess.lastModified()));
        } else if (PropertyIdentifier.readOnly.equals(pid)) {
            set(PropertyIdentifier.readOnly, Boolean.valueOf(!fileAccess.canWrite()));
        } else if (PropertyIdentifier.recordCount.equals(pid)) {
            if (fileAccess.supportsRecordAccess())
                set(PropertyIdentifier.recordCount, new UnsignedInteger(fileAccess.recordCount()));
            else {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.readAccessDenied);
            }
        }
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.fileSize.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger fileSize = value.getValue();
            fileAccess.validateFileSizeWrite(fileSize.longValue());
        } else if (PropertyIdentifier.recordCount.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger recordCount = value.getValue();
            fileAccess.validateRecordCountWrite(recordCount.longValue());
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (pid.equals(PropertyIdentifier.fileSize)) {
        fileAccess.writeFileSize(((UnsignedInteger) newValue).longValue());
        } else if (pid.equals(PropertyIdentifier.recordCount)) {
            fileAccess.writeRecordCount(((UnsignedInteger) newValue).longValue());
        }
    }
}
