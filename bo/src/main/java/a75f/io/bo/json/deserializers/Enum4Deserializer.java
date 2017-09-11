package a75f.io.bo.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by samjithsadasivan isOn 8/22/17.
 */

public class Enum4Deserializer<HT extends Enum<HT>> extends StdDeserializer<Struct.Enum4<HT>>
{
	public Enum4Deserializer()
	{
		this(null);
	}
	
	
	public Enum4Deserializer(Class<HT> t)
	{
		super(t);
	}
	
	
	@Override
	public Struct.Enum4<HT> deserialize(JsonParser p, DeserializationContext cxt)
	{
		try
		{
			for (HT values : getEmptyValue(cxt).getValues())
			{
				if (values.name().equalsIgnoreCase(p.getValueAsString()))
				{
					Struct.Enum4<HT> emptyValues = getEmptyValue(cxt);
					emptyValues.set(values);
					return emptyValues;
				}
			}
			return null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
