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
		
		Object currentValue =
				((ReaderBasedJsonParser) cxt.getParser()).getParsingContext().getCurrentValue();
		try
		{
			String currentName = ((ReaderBasedJsonParser) cxt.getParser()).getCurrentName();
			Field field = currentValue.getClass().getField(currentName);
			
			((Field) field).setAccessible(true);
			Struct.UTF8String utf8String = (Struct.UTF8String)((Field) field).get(currentValue);
			utf8String.set(p.getValueAsString());
			return utf8String;
			
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
