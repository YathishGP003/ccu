package a75f.io.bo.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by samjithsadasivan on 8/22/17.
 */

public class UTF8Deserializer extends StdDeserializer<Struct.UTF8String>
{
	public UTF8Deserializer()
	{
		this(null);
	}
	
	public UTF8Deserializer(Class<Struct.Unsigned8> t)
	{
		super(t);
	}
	
	@Override
	public Struct.UTF8String deserialize(JsonParser p, DeserializationContext cxt)
	{
		try
		{
			Struct.UTF8String utfString = new Struct().new UTF8String(p.getValueAsString().length());
			utfString.set(p.getValueAsString());
			return utfString;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
