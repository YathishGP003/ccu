package a75f.io.bo.ble;

import java.nio.ByteOrder;

import javolution.io.Struct;

/**
 * Created by ryanmattison on 7/26/17.
 */

public class BLEShort extends Struct {

    public final Unsigned16 smartNodeAddress = new Unsigned16();



    @Override
    public ByteOrder byteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
