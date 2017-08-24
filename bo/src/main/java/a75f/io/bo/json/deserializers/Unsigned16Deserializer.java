package a75f.io.bo.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by samjithsadasivan on 8/22/17.
 */

public class Unsigned16Deserializer extends StdDeserializer<Struct.Unsigned16>
{
	public Unsigned16Deserializer()
	{
		this(null);
	}
	
	public Unsigned16Deserializer(Class<Struct.Unsigned8> t)
	{
		super(t);
	}
	
	@Override
	public Struct.Unsigned16 deserialize(JsonParser p, DeserializationContext cxt)
	{
		try
		{
			Struct.Unsigned16 unsignedStruct = new Struct().new Unsigned16();
			unsignedStruct.set(p.getIntValue());
			return unsignedStruct;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
