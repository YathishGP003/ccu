package a75f.io.bo;

import org.javolution.io.Struct;

import a75f.io.bo.serial.MessageType;

/**
 * Created by ryanmattison on 7/31/17.
 */

public class TestStruct extends Struct {
    public final Struct.Enum8<MessageType> messageType = new Struct.Enum8<>(MessageType.values());
    public final Struct.Unsigned16 smartNodeAddress = new Struct.Unsigned16();

    public Enum8<MessageType> getMessageType() {
        return messageType;
    }
}
