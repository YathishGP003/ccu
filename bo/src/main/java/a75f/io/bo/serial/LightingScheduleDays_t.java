package a75f.io.bo.serial;

import org.javolution.io.Struct;
import org.javolution.io.Union;

/**
 * Created by samjithsadasivan on 8/2/17.
 */

public class LightingScheduleDays_t extends Union {

    public final class LightingDaysInner extends Struct {

        public final BitField monday = new BitField(1);

        public final BitField tuesday = new BitField(1);

        public final BitField wednesday = new BitField(1);

        public final BitField thursday = new BitField(1);

        public final BitField friday = new BitField(1);

        public final BitField saturday = new BitField(1);

        public final BitField sunday = new BitField(1);

    }

    public final Unsigned8 bitmap = new Unsigned8();
}
