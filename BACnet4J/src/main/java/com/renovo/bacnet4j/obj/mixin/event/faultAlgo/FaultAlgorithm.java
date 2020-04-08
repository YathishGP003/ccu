
package com.renovo.bacnet4j.obj.mixin.event.faultAlgo;

import java.util.Map;

import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.FaultParameter.AbstractFaultParameter;
import com.renovo.bacnet4j.type.constructed.ObjectPropertyReference;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;

abstract public class FaultAlgorithm {
    // Intrinsic reporting
    abstract public Reliability evaluateIntrinsic(Encodable oldMonitoredValue, Encodable newMonitoredValue,
            BACnetObject bo);

    // Algorithmic reporting
    abstract public Reliability evaluateAlgorithmic(Encodable oldMonitoredValue, Encodable newMonitoredValue,
            Reliability currentReliability, ObjectIdentifier monitoredObjectReference,
            Map<ObjectPropertyReference, Encodable> additionalValues, AbstractFaultParameter parameters);
}
