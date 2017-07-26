package a75f.io.bo.serial.CCUtoCM;

import javolution.io.Struct;

/**
 * Created by ryanmattison on 7/25/17.
 */

public class SystemTime_t extends Struct {

    public final Unsigned8 day = new Unsigned8();
    public final Unsigned8 hours = new Unsigned8();
    public final Unsigned8 minutes = new Unsigned8();
}
