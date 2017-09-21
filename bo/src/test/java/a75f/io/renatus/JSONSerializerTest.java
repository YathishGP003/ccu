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
import a75f.io.bo.building.Node;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.SingleStageProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.MessageType;

import static a75f.io.bo.json.serializers.JsonSerializer.toJson;

/**
 * Created by Yinten isOn 8/14/2017.
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
		short smartNodeAddress = 0;
		CCUApplication ccuApplication = new CCUApplication();

		Node node5K = new Node();
		node5K.setAddress(smartNodeAddress);

		ccuApplication.setTitle("Light Test");
		Floor floor = new Floor(1, "webid", "Floor1");
		Zone zone = new Zone("Zone1");
		LightProfile lightProfile5K = (LightProfile) zone.findProfile(ProfileType.LIGHT);

		zone.getNodes().put(node5K.getAddress(), node5K);
        Output output5K = new Output();
		output5K.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		output5K.setName("Dining Room");
        ccuApplication.getFloors().add(floor);
        ccuApplication.getFloors().get(0).mRoomList.add(zone);
		zone.addOutputCircuit(node5K, lightProfile5K, output5K);

		try
		{
			String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
			System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
            CCUApplication ccu =
                    (CCUApplication) JsonSerializer.fromJson(ccuApplicationJSON, CCUApplication.class);
            Assert.assertTrue((ccu.getFloors().get(0).mRoomList.get(0).findProfile(ProfileType.LIGHT)instanceof
                                      LightProfile));
            Assert.assertFalse((ccu.getFloors().get(0).mRoomList.get(0).findProfile(ProfileType.SSE) instanceof SingleStageProfile));
        }
		catch (IOException e)
		{
			e.printStackTrace();
		}
		//Node smartNode = ccuApplication.findSmartNodeByAddress(ccuApplication.floors.get(0)
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