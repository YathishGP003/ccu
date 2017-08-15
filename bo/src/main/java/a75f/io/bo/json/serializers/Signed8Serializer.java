package a75f.io.bo.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by Yinten on 8/15/2017.
 */

public class Signed8Serializer extends StdSerializer<Struct.Signed8>
{
	
	public Signed8Serializer()
	{
		this(null);
	}
	
	
	public Signed8Serializer(Class<Struct.Signed8> t)
	{
		super(t);
	}
	
	
	@Override
	public void serialize(Struct.Signed8 value, JsonGenerator gen, SerializerProvider provider) throws IOException
	{
		gen.writeNumber(value.get());
	}
}
