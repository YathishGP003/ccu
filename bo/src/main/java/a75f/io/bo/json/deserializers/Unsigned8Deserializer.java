package a75f.io.bo.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by samjithsadasivan isOn 8/22/17.
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
			Struct.Unsigned8 unsigned8 = (Struct.Unsigned8)cxt.getParser().getEmbeddedObject();
			unsigned8.set(p.getShortValue());
			return unsigned8;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
