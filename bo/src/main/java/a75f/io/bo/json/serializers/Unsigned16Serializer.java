package a75f.io.bo.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by Yinten on 8/15/2017.
 */

public class Unsigned16Serializer extends StdSerializer<Struct.Unsigned16>
{
	
	public Unsigned16Serializer()
	{
		this(null);
	}
	
	
	public Unsigned16Serializer(Class<Struct.Unsigned16> t)
	{
		super(t);
	}
	
	
	@Override
	public void serialize(Struct.Unsigned16 value, JsonGenerator gen, SerializerProvider provider) throws IOException
	{
		gen.writeNumber(value.get());
	}
}
