package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan on 8/1/17.
 */

public class FirmwareSerialNumber_t extends Struct {

    public class Words extends Struct {
        public final Unsigned32 word0 = new Unsigned32();
        public final Unsigned32 word1 = new Unsigned32();
        public final Unsigned32 word2 = new Unsigned32();
        public final Unsigned32 word3 = new Unsigned32();
    }

    public final Unsigned8[] array = array(new Unsigned8[MessageConstants.MAX_SERIAL_NUMBER_LENGTH]);
}
