package a75f.io.device.serial;

import org.javolution.io.Struct;

public class UnusedMessage_t extends Struct {

	public final Enum8<MessageType> unusedMessageType = new Enum8<>(MessageType.values());
	
}
