package a75f.io.device.modbus;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

public class CcuToMbOverUsbReadHoldingRegistersRequest_t extends Struct {
    public final Unsigned8 slaveId = new Unsigned8();
    public final Unsigned8 functionCode = new Unsigned8();
    public final Unsigned8 startingAddressHigh = new Unsigned8();
    public final Unsigned8 startingAddressLow = new Unsigned8();
    public final Unsigned8 quantityOfCoilsHigh = new Unsigned8();
    public final Unsigned8 quantityOfCoilsLow = new Unsigned8();
    public final Unsigned8 errorCheckLow = new Unsigned8();
    public final Unsigned8 errorCheckHigh = new Unsigned8();
    @Override
    public ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
