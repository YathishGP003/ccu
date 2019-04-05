package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

public class CmToCcuOverUsbSmartStatLocalControlsOverrideMessage_t extends Struct {
    public final Struct.Enum8<MessageType> messageType      = new Enum8<>(MessageType.values());
    public final Unsigned16         smartNodeAddress = new Unsigned16(); /* LW Mesh Address of the Smart Node sending the message */
    public final Unsigned8          setTemperature   = new Unsigned8(); /* in 2x degrees Fahrenheit */
    public final Enum4<SmartStatFanSpeed_t> fanSpeed = new Enum4<>(SmartStatFanSpeed_t.values());
    @Override
    public ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
