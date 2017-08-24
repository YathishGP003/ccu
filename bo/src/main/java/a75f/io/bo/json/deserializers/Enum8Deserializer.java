package a75f.io.bo.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by samjithsadasivan on 8/22/17.
 */

public class Enum8Deserializer<T> extends StdDeserializer<T>
{
	public Enum8Deserializer()
	{
		this(null);
	}
	
	public Enum8Deserializer(Class<T> t)
	{
		super(t);
	}
	
	@Override
	public T deserialize(JsonParser p, DeserializationContext cxt)
	{
		try
		{
			
			Struct.Enum8 enumStruct = new Struct().new Enum8(((Struct.Enum8) _valueClass.newInstance()).getValues());
			enumStruct.set(((Struct.Enum8) _valueClass.newInstance()).getValues()[p.getValueAsInt()]);
			return (T) enumStruct;
			
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
