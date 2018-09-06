package a75f.io.logic.bo.ble;

import org.javolution.io.Struct;

import java.nio.ByteOrder;


/**
 * Created by ryanmattison isOn 7/26/17.
 */

public class StructShort extends Struct {

    public final Unsigned16 smartNodeAddress = new Unsigned16();


    @Override
    public ByteOrder byteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
