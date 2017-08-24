package a75f.io.bo.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by samjithsadasivan on 8/22/17.
 */

public class Enum4Deserializer<HT extends Struct.Enum4> extends StdDeserializer<HT>
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
	public HT deserialize(JsonParser p, DeserializationContext cxt)
	{
		try
		{
			Struct.Enum4 enumStruct = new Struct().new Enum4(((Struct.Enum4) _valueClass.newInstance()).getValues());
			enumStruct.set(((Struct.Enum4) _valueClass.newInstance()).getValues()[p.getValueAsInt()]);
			return (HT) enumStruct;
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
