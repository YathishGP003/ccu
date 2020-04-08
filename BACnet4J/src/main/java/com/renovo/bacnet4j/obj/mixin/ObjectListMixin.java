
package com.renovo.bacnet4j.obj.mixin;

import static com.renovo.bacnet4j.type.enumerated.PropertyIdentifier.objectList;

import java.util.ArrayList;
import java.util.List;

import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.AbstractMixin;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.type.constructed.BACnetArray;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;

public class ObjectListMixin extends AbstractMixin {
    public ObjectListMixin(final BACnetObject bo) {
        super(bo);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (objectList.equals(value.getPropertyIdentifier()))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        return false;
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) {
        if (objectList.equals(pid)) {
            final List<ObjectIdentifier> oids = new ArrayList<>();
            for (final BACnetObject bo : getLocalDevice().getLocalObjects()) {
                oids.add(bo.getId());
            }
            writePropertyInternal(objectList, new BACnetArray<>(oids));
        }
    }
}
