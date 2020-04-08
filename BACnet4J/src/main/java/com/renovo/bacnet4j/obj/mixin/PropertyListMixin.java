
package com.renovo.bacnet4j.obj.mixin;

import static com.renovo.bacnet4j.type.enumerated.PropertyIdentifier.objectIdentifier;
import static com.renovo.bacnet4j.type.enumerated.PropertyIdentifier.objectName;
import static com.renovo.bacnet4j.type.enumerated.PropertyIdentifier.objectType;
import static com.renovo.bacnet4j.type.enumerated.PropertyIdentifier.propertyList;

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

public class PropertyListMixin extends AbstractMixin {
    public PropertyListMixin(final BACnetObject bo) {
        super(bo);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (propertyList.equals(value.getPropertyIdentifier()))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        return false;
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) {
        if (pid.equals(PropertyIdentifier.propertyList)) {
            final List<PropertyIdentifier> pids = new ArrayList<>();
            for (final PropertyIdentifier p : properties().keySet()) {
                if (!p.isOneOf(objectName, objectType, objectIdentifier, propertyList))
                    pids.add(p);
            }
            writePropertyInternal(propertyList, new BACnetArray<>(pids));
        }
    }
}
