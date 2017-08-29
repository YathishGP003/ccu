package a75f.io.renatus;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.Floor;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.MessageType;

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
		seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SETTINGS);
		
		seedMessage.settings.roomName.set(roomName);
		seedMessage.settings.ledBitmap.analogIn1.set((short) 1);
		
		seedMessage.smartNodeAddress.set(5); 
		try
		{
			String pojoAsString = toJson(seedMessage, true);
			System.out.println("POJO as string:\n" + pojoAsString + "\n");
			CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessagePostSerilizer =
					(CcuToCmOverUsbDatabaseSeedSnMessage_t) JsonSerializer.fromJson
							                                                                 (pojoAsString,CcuToCmOverUsbDatabaseSeedSnMessage_t.class);
			
			System.out.println("seedMessage: " + seedMessagePostSerilizer.messageType);
			
			System.out.println("roomname: " + seedMessage.settings.roomName.get());
			System.out.println("roomname: " + seedMessagePostSerilizer.settings.roomName.get());
			System.out.println("adddress: " + seedMessagePostSerilizer.smartNodeAddress);
			Assert.assertTrue(seedMessage.settings.roomName.get().equals(seedMessagePostSerilizer
					                                                       .settings.roomName.get()));
			
			
			System.out.println("Before: " + seedMessage.toString());
			System.out.println("After: " +  seedMessagePostSerilizer.toString
					                                                                             ());
			Assert.assertArrayEquals(seedMessage.getOrderedBuffer(), seedMessagePostSerilizer.getOrderedBuffer());
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
		short smartNodeAddress = 5000;
		CCUApplication ccuApplication = new CCUApplication();
		SmartNode smartNode5K = new SmartNode();
		smartNode5K.mAddress = smartNodeAddress;
		smartNode5K.mRoomName = "SmartNode roomName";
		ccuApplication.smartNodes.add(smartNode5K);
		ccuApplication.CCUTitle = "Light Test";
		Floor floor = new Floor(1, "webid", "Floor1");
		LightProfile lightProfile5K = new LightProfile("Light Profile");
//		ccuApplication.floors.get(0).addZone("5000 test zone");
//		ccuApplication.floors.get(0).getRoomList().get(0).zoneProfiles.add(lightProfile5K);
		SmartNodeOutput smartNodeOutput5K = new SmartNodeOutput();
		smartNodeOutput5K.mSmartNodeAddress = smartNode5K.mAddress;
		smartNodeOutput5K.mUniqueID = analog15kUUID;
		smartNodeOutput5K.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		smartNodeOutput5K.mOutput = Output.Analog;
		smartNodeOutput5K.mName = "Dining Room";
		lightProfile5K.smartNodeOutputs.add(smartNodeOutput5K);
		try
		{
			String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
			System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		//SmartNode smartNode = ccuApplication.findSmartNodeByAddress(ccuApplication.floors.get(0)
//		                                                                                  .getRoomList().get(0).zoneProfiles.get(0).smartNodeOutputs.get(0).mSmartNodeAddress);
//		CcuToCmOverUsbDatabaseSeedSnMessage_t ccuToCmOverUsbDatabaseSeedSnMessage_t = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
//		ccuToCmOverUsbDatabaseSeedSnMessage_t.smartNodeAddress.set(smartNode.mAddress);
//		ZoneProfile zoneProfile = ccuApplication.floors.get(0).getRoomList().get(0).zoneProfiles.get(0);
//		ccuToCmOverUsbDatabaseSeedSnMessage_t.controls.analogOut1.set((short) 0);
//		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.profileBitmap.lightingControl.set((short) 1);
//		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.ledBitmap.analogIn1.set((short) 1);
//		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.lightingIntensityForOccupantDetected.set((short) 100);
//		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.minLightingControlOverrideTimeInMinutes.set((short) 1);
//		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.roomName.set(smartNode.mRoomName);
//		try
//		{ioi
//			String ccuToCMSeedMessage = JsonSerializer.toJson(ccuToCmOverUsbDatabaseSeedSnMessage_t, true);
//			System.out.println("CCU seedMessage:\n" + ccuToCMSeedMessage + "\n");
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
	}
	
}