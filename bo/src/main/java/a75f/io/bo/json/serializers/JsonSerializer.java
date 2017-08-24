package a75f.io.bo.json.serializers;
/**
 * Created by Yinten on 8/14/2017.
 */

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

public class JsonSerializer
{
	
	private static ObjectMapper m  = new ObjectMapper();
	private static JsonFactory  jf = new JsonFactory();
	
	
	
	
	public static <T> Object fromJson(String jsonAsString, Class<T> pojoClass) throws JsonMappingException, JsonParseException, IOException
	{
		m.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		m.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		return m.readValue(jsonAsString, pojoClass);
	}
	
	
	public static <T> Object fromJson(FileReader fr, Class<T> pojoClass) throws JsonParseException, IOException
	{
		m.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		m.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		return m.readValue(fr, pojoClass);
	}
	
	
	public static String toJson(Object pojo, boolean prettyPrint) throws JsonMappingException, JsonGenerationException, IOException
	{
		StringWriter sw = new StringWriter();
		JsonGenerator jg = jf.createJsonGenerator(sw);
		m.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		m.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		
		if (prettyPrint)
		{
			jg.useDefaultPrettyPrinter();
		}
		m.writeValue(jg, pojo);
		return sw.toString();
	}
	
	
	public static void toJson(Object pojo, FileWriter fw, boolean prettyPrint) throws JsonMappingException, JsonGenerationException, IOException
	{
		JsonGenerator jg = jf.createJsonGenerator(fw);
		if (prettyPrint)
		{
			jg.useDefaultPrettyPrinter();
		}
		m.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		m.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		m.writeValue(jg, pojo);
	}
}
