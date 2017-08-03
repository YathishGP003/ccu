package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan on 8/1/17.
 */

public class SnSetTemperatureUpdateMessage_t extends Struct {

    public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
    public final Unsigned16 smartNodeAddress = new Unsigned16(); /* LW Mesh Address of the Smart Node sending the message */
    public final Unsigned8 setTemperature = new Unsigned8(); /* in 2x degrees Fahrenheit */
    @Override
    public ByteOrder byteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
