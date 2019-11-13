package a75f.io.device.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.javolution.io.Struct;

import java.io.IOException;

public class Unsigned32Serializer extends StdSerializer<Struct.Unsigned32>
{
    
    public Unsigned32Serializer()
    {
        this(null);
    }
    
    
    public Unsigned32Serializer(Class<Struct.Unsigned32> t)
    {
        super(t);
    }
    
    
    @Override
    public void serialize(Struct.Unsigned32 value, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        gen.writeNumber(value.get());
    }
}