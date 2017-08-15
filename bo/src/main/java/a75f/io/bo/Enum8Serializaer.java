package a75f.io.bo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.javolution.io.Struct;

import java.io.IOException;

import a75f.io.bo.serial.MessageType;

/**
 * Created by Yinten on 8/14/2017.
 */

public class Enum8Serializaer extends StdSerializer<Struct.Enum8<MessageType>>
{
	
	public Enum8Serializaer()
	{
		this(null);
	}
	
	
	protected Enum8Serializaer(Class<Struct.Enum8<MessageType>> t)
	{
		super(t);
	}
	
	
	@Override
	public void serialize(Struct.Enum8<MessageType> value, JsonGenerator gen, SerializerProvider provider) throws IOException
	{
		MessageType messageType = value.get();
		gen.writeString(messageType.name());
	}
}
