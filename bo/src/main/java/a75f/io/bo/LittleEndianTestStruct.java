package a75f.io.bo;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

import a75f.io.bo.serial.MessageType;

/**
 * Created by ryanmattison on 7/31/17.
 */




public class LittleEndianTestStruct extends Struct {
    public final Unsigned8 messageType = new Unsigned8();
    public final Unsigned16 smartNodeAddress = new Unsigned16();

    @Override
    public ByteOrder byteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
