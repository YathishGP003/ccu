package a75f.io.renatus;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.OutputAnalogActuatorType;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;

import static a75f.io.bo.json.serializers.JsonSerializer.toJson;

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
			String pojoAsString = toJson(seedMessage, true);
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
	public void buildingPOJOTest()
	{
		CCUApplication ccuApp = new CCUApplication();
		try
		{
			String pojoAsString = JsonSerializer.toJson(ccuApp, true);
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
		UUID analog15kUUID = UUID.randomUUID();
		int smartNodeAddress = 5000;
		CCUApplication ccuApplication = new CCUApplication();
		SmartNode smartNode5K = new SmartNode();
		smartNode5K.address = smartNodeAddress;
		smartNode5K.analog1OutId = analog15kUUID;
		smartNode5K.roomName = "SmartNode roomName";
		ccuApplication.smartNodes.add(smartNode5K);
		ccuApplication.CCUTitle = "Light Test";
		Zone zone5K = new Zone();
		zone5K.roomName = "5000 test zone";
		LightProfile lightProfile5K = new LightProfile();
		zone5K.zoneProfiles.add(lightProfile5K);
		ccuApplication.zones.add(zone5K);
		SmartNodeOutput smartNodeOutput5K = new SmartNodeOutput();
		smartNodeOutput5K.uniqueID = analog15kUUID;
		smartNodeOutput5K.outputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		smartNodeOutput5K.output = Output.Analog;
		smartNodeOutput5K.name = "Dining Room";
		lightProfile5K.smartNodeOutputs.add(smartNodeOutput5K);
		//	lightProfile5K.smartNodeOutputs.add();
		try
		{
			String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
			System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		SmartNode smartNode = ccuApplication.findSmartNodeByIOUUID(ccuApplication.zones.get(0).zoneProfiles.get(0).smartNodeOutputs.get(0).uniqueID);
		CcuToCmOverUsbDatabaseSeedSnMessage_t ccuToCmOverUsbDatabaseSeedSnMessage_t = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		ccuToCmOverUsbDatabaseSeedSnMessage_t.smartNodeAddress.set(smartNode.address);
		//ccuToCmOverUsbDatabaseSeedSnMessage_t.putEncrptionKey(Encryp);
		ZoneProfile zoneProfile = ccuApplication.zones.get(0).zoneProfiles.get(0);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.controls.analogOut1.set((short) 0);
		
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.profileBitmap.lightingControl.set((short) 1);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.ledBitmap.analogIn1.set((short)1);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.lightingIntensityForOccupantDetected.set((short) 100);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.minLightingControlOverrideTimeInMinutes.set((short)1);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.roomName.set(smartNode.roomName);
		
		
		try
		{
			String ccuToCMSeedMessage  = JsonSerializer.toJson(ccuToCmOverUsbDatabaseSeedSnMessage_t, true);
			System.out.println("CCU seedMessage:\n" + ccuToCMSeedMessage + "\n");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}