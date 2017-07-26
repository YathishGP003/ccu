package a75f.io.bo.serial.CCUtoCM;

import javolution.io.Struct;
import javolution.io.Union;

/**
 * Created by ryanmattison on 7/26/17.
 */

public class SmartNodeSettingsMerge extends Union
{

    public final Unsigned8 mergeInnerUnsignedInt = new Unsigned8();
    public final SmartNodeMergeInner mergeInner = inner(new SmartNodeMergeInner());

    public class SmartNodeMergeInner extends Struct
    {
        public final BitField showCentigrade = new BitField(1);
        public final BitField displayHold = new BitField(1);
        public final BitField militaryTime = new BitField(1);
        public final BitField enableOccupationDetection = new BitField(1);
    }

}
