package a75f.io.bo.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.javolution.io.Struct;

import java.io.IOException;

/**
 * Created by samjithsadasivan isOn 8/15/17.
 */

public class Enum4Serializer<HT extends Struct.Enum4> extends StdSerializer<HT>
{
	
	public Enum4Serializer()
	{
		this(null);
	}
	
	
	public Enum4Serializer(Class<HT> t)
	{
		super(t);
	}
	
	
	@Override
	public void serialize(HT value, JsonGenerator gen, SerializerProvider provider) throws IOException
	{
		
		gen.writeString(value.get().name());
	}
}
