package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

public class CcuToCmOverUsbSettingsMessage_t extends Struct
{
    //MessageType is 60
    public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
}
