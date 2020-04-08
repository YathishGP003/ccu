
package com.renovo.bacnet4j.obj;

import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;

@FunctionalInterface
public interface BACnetObjectListener {
    void propertyChange(PropertyIdentifier pid, Encodable oldValue, Encodable newValue);
}
