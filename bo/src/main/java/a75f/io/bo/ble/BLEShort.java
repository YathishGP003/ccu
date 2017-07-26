package a75f.io.bo.ble;

import java.nio.ByteOrder;

import javolution.io.Struct;

/**
 * Created by ryanmattison on 7/26/17.
 */

public class BLEShort extends Struct {
    public final Unsigned16 smartNodeAddress = new Unsigned16();

    public static byte[] getBytes(BLEShort bleShort)
    {
        byte[] retVal = new byte[bleShort.size()];
        for(int i = 0; i < bleShort.size(); i++)
        {
            retVal[i] = bleShort.getByteBuffer().get(i);
        }
        return retVal;
    }

    public static BLEShort get(short bleShortPrim)
    {
        BLEShort bleShort = new BLEShort();
        bleShort.smartNodeAddress.set(bleShortPrim);
        return bleShort;
    }

    @Override
    public ByteOrder byteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
