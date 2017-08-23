package a75f.io.bo.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by samjithsadasivan on 8/22/17.
 */

public class Unsigned8Deserializer extends StdDeserializer<Struct.Unsigned8>
{
	public Unsigned8Deserializer()
	{
		this(null);
	}
	
	public Unsigned8Deserializer(Class<Struct.Unsigned8> t)
	{
		super(t);
	}
	
	@Override
	public Struct.Unsigned8 deserialize(JsonParser p, DeserializationContext cxt)
	{
		try
		{
			Struct.Unsigned8 unsignedStruct = new Struct().new Unsigned8();
			unsignedStruct.set(p.getShortValue());
			return unsignedStruct;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
