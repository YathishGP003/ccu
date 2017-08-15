package a75f.io.bo.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by Yinten on 8/14/2017.
 */

public class Enum8Serializaer<HT extends Struct.Enum8> extends StdSerializer<HT>
{
	
	public Enum8Serializaer()
	{
		this(null);
	}
	
	
	public Enum8Serializaer(Class<HT> t)
	{
		super(t);
	}
	
	
	@Override
	public void serialize(HT value, JsonGenerator gen, SerializerProvider provider) throws IOException
	{
		
		gen.writeString(value.get().name());
	}
}
//}
//{
//
//	public Enum8Serializaer(JavaType type)
//	{
//		super(type);
//	}
//
//
//	@Override
//	public void serialize(Struct.Enum8<E> value, JsonGenerator gen, SerializerProvider provider) throws IOException
//	{
//		value.get()
//	}
//
//
//	@Override
//	public void serialize(Struct.Enum8<T> value, JsonGenerator gen, SerializerProvider provider) throws IOException
//	{
//
//		T enu  = value.get();
//
//	}
//
//
//	public Enum8Serializaer(Class<T> t)
//	{
//		super(t);
//	}
//
//
//	@Override
//	public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException
//	{
//
//
//	}
//}