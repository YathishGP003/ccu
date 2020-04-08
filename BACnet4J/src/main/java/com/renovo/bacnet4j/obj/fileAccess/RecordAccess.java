package com.renovo.bacnet4j.obj.fileAccess;

import java.io.IOException;

import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.FileAccessMethod;
import com.renovo.bacnet4j.type.primitive.OctetString;

public interface RecordAccess extends FileAccess {
    @Override
    default FileAccessMethod getAccessMethod() {
        return FileAccessMethod.recordAccess;
    }

    @Override
    default boolean supportsStreamAccess() {
        return false;
    }

    @Override
    default OctetString readData(final long start, final long length) throws IOException, BACnetServiceException {
        throw new BACnetServiceException(ErrorClass.services, ErrorCode.invalidFileAccessMethod);
    }

    @Override
    default long writeData(final long start, final OctetString data) throws IOException, BACnetServiceException {
        throw new BACnetServiceException(ErrorClass.services, ErrorCode.invalidFileAccessMethod);
    }

    @Override
    default boolean supportsRecordAccess() {
        return true;
    }
}
