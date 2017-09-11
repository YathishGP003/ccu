package a75f.io.bo.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.ReaderBasedJsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.javolution.io.Struct;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Created by samjithsadasivan isOn 8/22/17.
 */


public class Enum8Deserializer<HT extends Enum<HT>> extends StdDeserializer<Struct.Enum8<HT>>
{
	public Enum8Deserializer()
	{
		this(null);
	}
	
	
	public Enum8Deserializer(Class<HT> t)
	{
		super(t);
	}
	
	
	@Override
	public Struct.Enum8<HT> deserialize(JsonParser p, DeserializationContext cxt)
	{
		Object currentValue =
				((ReaderBasedJsonParser) cxt.getParser()).getParsingContext().getCurrentValue();
		try
		{
			String currentName = ((ReaderBasedJsonParser) cxt.getParser()).getCurrentName();
			Field field = currentValue.getClass().getField(currentName);
			
			((Field) field).setAccessible(true);
			Struct.Enum8<HT> enum8 = (Struct.Enum8 <HT>)((Field) field).get(currentValue);
			
			
			
			try
			{
				for (HT values : enum8.getValues())
				{
					if (values.name().equalsIgnoreCase(p.getValueAsString()))
					{
						enum8.set(values);
						return enum8;
					}
				}
				return null;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchFieldException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
