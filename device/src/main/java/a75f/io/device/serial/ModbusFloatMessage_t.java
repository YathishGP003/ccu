package a75f.io.device.serial;
import org.javolution.io.Struct;
import java.nio.ByteOrder;

public class ModbusFloatMessage_t extends Struct {

    public final Struct.Enum8<MessageType> messageType = new Struct.Enum8<>(MessageType.values());
    public final Unsigned8 slaveId = new Unsigned8();
    public final Unsigned8 functionCode = new Unsigned8();
    public final Unsigned8 startingAddressHigh = new Unsigned8();
    public final Unsigned8 startingAddressLow = new Unsigned8();
    public final Unsigned8 quantityOfCoilsHigh = new Unsigned8();
    public final Unsigned8 quantityOfCoilsLow = new Unsigned8();
    public final Unsigned8 byteCount = new Unsigned8();
    public final Unsigned8 registerVal0 = new Unsigned8();
    public final Unsigned8 registerVal1 = new Unsigned8();
    public final Unsigned8 registerVal2 = new Unsigned8();
    public final Unsigned8 registerVal3 = new Unsigned8();
    public final Unsigned8 errorCheckLow = new Unsigned8();
    public final Unsigned8 errorCheckHigh = new Unsigned8();


    @Override
    public ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }
}

