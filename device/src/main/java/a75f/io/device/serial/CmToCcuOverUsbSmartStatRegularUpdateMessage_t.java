package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;
/**
 * Created by Anilkumar On 02/05/2019.
 */
public class CmToCcuOverUsbSmartStatRegularUpdateMessage_t extends Struct {


    public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());


    public final SmartStatToCmOverAirSsRegularUpdateMessage_t update = inner(new SmartStatToCmOverAirSsRegularUpdateMessage_t());

    //public final Unsigned8 cmLqi  = new Unsigned8(); /* LQI of this received data packet @ CM */
    //public final Signed8   cmRssi = new Signed8(); /* RSSI of this received data packet @ CM */

    @Override
    public ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
