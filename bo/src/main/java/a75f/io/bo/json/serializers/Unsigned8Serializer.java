package a75f.io.bo.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by Yinten on 8/15/2017.
 */

public class Unsigned8Serializer extends StdSerializer<Struct.Unsigned8>
{
	
	public Unsigned8Serializer()
	{
		this(null);
	}
	
	
	public Unsigned8Serializer(Class<Struct.Unsigned8> t)
	{
		super(t);
	}
	
	
	@Override
	public void serialize(Struct.Unsigned8 value, JsonGenerator gen, SerializerProvider provider) throws IOException
	{
		gen.writeNumber(value.get());
	}
}
