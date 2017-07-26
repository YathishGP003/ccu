package a75f.io.bo.serial.CCUtoCM;

import javolution.io.Struct;
import javolution.io.Union;

/**
 * Created by ryanmattison on 7/25/17.
 */

public class SmartNodeLedBitmap_t extends Union {


    public final Unsigned16 bitmap = new Unsigned16();
    public final SmartNodeLedBitmap_t_extras smartNodeLedBitmap_t_extras = inner(new SmartNodeLedBitmap_t_extras());

    public class SmartNodeLedBitmap_t_extras extends Struct
    {
        public final BitField powerIn = new BitField(1);
        public final BitField powerOut = new BitField(1);
        public final BitField analogIn1 = new BitField(1);
        public final BitField analogIn2 = new BitField(1);
        public final BitField thermistor1 = new BitField(1);
        public final BitField thermistor2 = new BitField(1);
        public final BitField analogOut1 = new BitField(1);
        public final BitField analogOut2 = new BitField(1);
        public final BitField digitalOut1 = new BitField(1);
        public final BitField digitalOut2 = new BitField(1);
        public final BitField analogIn24v = new BitField(1);
        public final BitField analogOut24v = new BitField(1);
        public final BitField logo = new BitField(1);
        public final BitField reserved = new BitField(3);


    }
}
