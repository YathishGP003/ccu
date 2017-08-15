package a75f.io.bo.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by Yinten on 8/15/2017.
 */

public class Signed16Serializer extends StdSerializer<Struct.Signed16>
{
	
	public Signed16Serializer()
	{
		this(null);
	}
	
	
	public Signed16Serializer(Class<Struct.Signed16> t)
	{
		super(t);
	}
	
	
	@Override
	public void serialize(Struct.Signed16 value, JsonGenerator gen, SerializerProvider provider) throws IOException
	{
		gen.writeNumber(value.get());
	}
}
