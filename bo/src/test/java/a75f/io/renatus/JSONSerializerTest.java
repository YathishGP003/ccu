package a75f.io.renatus;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.junit.Test;

import java.io.IOException;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;

/**
 * Created by Yinten on 8/14/2017.
 */

public class JSONSerializerTest
{
	
	@Test
	public void simplePOJOTest()
	{
		String roomName = "Ryans Room";
		CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		seedMessage.settings.roomName.set(roomName);
		seedMessage.settings.ledBitmap.analogIn1.set((short) 1);
		try
		{
			String pojoAsString = JsonSerializer.toJson(seedMessage, true);
			System.out.println("POJO as string:\n" + pojoAsString + "\n");
		}
		catch (JsonGenerationException e)
		{
			e.printStackTrace();
		}
		catch (JsonMappingException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void generateLightObjectsTest()
	{
		int smartNodeAddress = 5000;
		CCUApplication ccuApplication = new CCUApplication();
		SmartNode smartNode5K = new SmartNode();
		smartNode5K.address = smartNodeAddress;
		
		ccuApplication.CCUTitle = "Light Test";
		
		Zone zone5K = new Zone();
		LightProfile lightProfile5K = new LightProfile();
		zone5K.zoneProfiles.add(lightProfile5K);
		ccuApplication.zones.add(zone5K);
		
		zone5K.roomName = "5000 test zone";
		SmartNodeOutput smartNodeOutput5K = new SmartNodeOutput();
		
		
	//	lightProfile5K.smartNodeOutputs.add();
		
		
		
		
	}
}