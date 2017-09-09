package a75f.io.bo.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by samjithsadasivan isOn 8/22/17.
 */

public class Signed8Deserializer extends StdDeserializer<Struct.Signed8>
{
	public Signed8Deserializer()
	{
		this(null);
	}
	
	public Signed8Deserializer(Class<Struct.Signed8> t)
	{
		super(t);
	}
	
	@Override
	public Struct.Signed8 deserialize(JsonParser p, DeserializationContext cxt)
	{
		try
		{
			Struct.Signed8 signedStruct = new Struct().new Signed8();
			signedStruct.set((byte)p.getValueAsInt());
			return signedStruct;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
