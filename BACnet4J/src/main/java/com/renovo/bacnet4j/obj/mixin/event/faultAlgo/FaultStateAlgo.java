
package com.renovo.bacnet4j.obj.mixin.event.faultAlgo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.FaultParameter.AbstractFaultParameter;
import com.renovo.bacnet4j.type.constructed.FaultParameter.FaultState;
import com.renovo.bacnet4j.type.constructed.ObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.PropertyStates;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;

// 13.4.5
public class FaultStateAlgo extends FaultAlgorithm {
    static final Logger LOG = LoggerFactory.getLogger(FaultStateAlgo.class);

    private final PropertyIdentifier currentReliabilityProperty;
    private final PropertyIdentifier faultValuesProperty;

    public FaultStateAlgo() {
        this(null, null);
    }

    public FaultStateAlgo(final PropertyIdentifier currentReliabilityProperty,
            final PropertyIdentifier faultValuesProperty) {
        this.currentReliabilityProperty = currentReliabilityProperty;
        this.faultValuesProperty = faultValuesProperty;
    }

    @Override
    public Reliability evaluateIntrinsic(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            final BACnetObject bo) {
        return evaluate( //
                oldMonitoredValue, //
                newMonitoredValue, //
                bo.get(currentReliabilityProperty), //
                bo.get(faultValuesProperty));
    }

    @Override
    public Reliability evaluateAlgorithmic(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            final Reliability currentReliability, final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractFaultParameter parameters) {
        final FaultState p = (FaultState) parameters;
        return evaluate( //
                oldMonitoredValue, //
                newMonitoredValue, //
                currentReliability, //
                p.getListOfFaultValues());
    }

    private static Reliability evaluate(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            Reliability currentReliability, final SequenceOf<? extends Encodable> faultValues) {
        if (currentReliability == null)
            currentReliability = Reliability.noFaultDetected;

        Reliability newReliability = null;

        // If this is intrinsic reporting the fault values will be a sequence of encodables (unsigned for MV).
        // If this is algorithmic, the fault values will be a sequence of PropertyStates
        boolean isFaultValue = false;
        for (final Encodable e : faultValues) {
            if (e.equals(newMonitoredValue)) {
                isFaultValue = true;
                break;
            } else if (e instanceof PropertyStates) {
                if (((PropertyStates) e).getState().equals(e)) {
                    isFaultValue = true;
                    break;
                }
            }
        }

        if (currentReliability.equals(Reliability.noFaultDetected) && isFaultValue)
            newReliability = Reliability.multiStateFault;
        else if (currentReliability.equals(Reliability.multiStateFault) && !isFaultValue)
            newReliability = Reliability.noFaultDetected;
        else if (currentReliability.equals(Reliability.multiStateFault) && isFaultValue
                && !faultValues.equals(oldMonitoredValue))
            newReliability = Reliability.multiStateFault;

        if (newReliability != null)
            LOG.debug("FaultState evaluated new reliability: {}", newReliability);

        return newReliability;
    }
}
