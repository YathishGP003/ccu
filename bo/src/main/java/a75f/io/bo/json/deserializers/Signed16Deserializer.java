package a75f.io.bo.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by samjithsadasivan on 8/22/17.
 */

public class Signed16Deserializer extends StdDeserializer<Struct.Signed16>
{
	public Signed16Deserializer()
	{
		this(null);
	}
	
	public Signed16Deserializer(Class<Struct.Signed16> t)
	{
		super(t);
	}
	
	@Override
	public Struct.Signed16 deserialize(JsonParser p, DeserializationContext cxt)
	{
		try
		{
			Struct.Signed16 signedStruct = new Struct().new Signed16();
			signedStruct.set((short)p.getValueAsInt());
			return signedStruct;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
