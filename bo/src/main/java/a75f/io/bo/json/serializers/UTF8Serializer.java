package a75f.io.bo.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by Yinten on 8/15/2017.
 */

public class UTF8Serializer extends StdSerializer<Struct.UTF8String>
{
	public UTF8Serializer()
	{
		this(null);
	}
	
	
	protected UTF8Serializer(Class<Struct.UTF8String> t)
	{
		super(t);
	}
	
	
	@Override
	public void serialize(Struct.UTF8String value, JsonGenerator gen, SerializerProvider provider) throws IOException
	{
		gen.writeString(value.get());
	}
}