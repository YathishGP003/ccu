package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

public class CcuToCmOverUsbCmResetMessage_t extends Struct
{
    //MessageType is 50
    public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());

    public final Unsigned8 reset = new Unsigned8(1);

    @Override
    public ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }

    public static byte[] getResetBytes()
    {
        CcuToCmOverUsbCmResetMessage_t ccuToCmOverUsbCmResetMessage_t = new CcuToCmOverUsbCmResetMessage_t();
        ccuToCmOverUsbCmResetMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_CM_RESET_MESSAGE);
        ccuToCmOverUsbCmResetMessage_t.reset.set((short) 1);
        return ccuToCmOverUsbCmResetMessage_t.getOrderedBuffer();
    }
}
