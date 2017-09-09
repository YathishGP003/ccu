package a75f.io.bo.building;

import junit.framework.Assert;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import a75f.io.bo.building.definitions.Input;
import a75f.io.bo.building.definitions.InputActuatorType;
import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

/**
 * Created by samjithsadasivan isOn 9/7/17.
 */

public class ProfileTest
{
	@Test
	public void testProfiles() {
		CCUApplication ccuApplication = new CCUApplication();
		SmartNode testSN = new SmartNode();
		testSN.mAddress = 7000;
		testSN.mRoomName = "75F";
		ccuApplication.smartNodes.add(testSN);
		ccuApplication.CCUTitle = "Test";
		Floor floor = new Floor(1, "webid", "Floor1");
		floor.mRoomList.add(new Zone("DefaultZone"));
		ZoneProfile p1 = new ZoneProfile("Test Profile")
		{
			@Override
			public List<CcuToCmOverUsbSnControlsMessage_t> getControlsMessage()
			{
				return null;
			}
		};
		ccuApplication.floors.add(floor);
		ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.add(p1);
		
		SmartNodeInput ip1 = new SmartNodeInput();
		ip1.mSmartNodeAddress = testSN.mAddress;
		ip1.mUniqueID = UUID.randomUUID();
		ip1.mName = "Room1";
		ip1.mInput = Input.Analog1In;
		ip1.mInputActuatorType = InputActuatorType.ZeroTo10ACurrentTransformer;
		p1.smartNodeInputs.add(ip1);
		
		SmartNodeOutput op1 = new SmartNodeOutput();
		op1.mSmartNodeAddress = testSN.mAddress;
		op1.mUniqueID = UUID.randomUUID();
		op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op1.mOutput = Output.Relay;
		op1.mName = "Kitchen";
		p1.smartNodeOutputs.add(op1);
		
		ZoneProfile p2 = new ZoneProfile()
		{
			@Override
			public List<CcuToCmOverUsbSnControlsMessage_t> getControlsMessage()
			{
				return null;
			}
		};
		ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.add(p2);
		
		ZoneProfile p3 = new ZoneProfile()
		{
			@Override
			public List<CcuToCmOverUsbSnControlsMessage_t> getControlsMessage()
			{
				return null;
			}
		};
		ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.add(p3);
		
		try
		{
			//String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
			//System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
			
			//CCUApplication deCcuApp = (CCUApplication) JsonSerializer.fromJson(ccu ,CCUApplication.class);
			Assert.assertEquals(1, ccuApplication.floors.size());
			Assert.assertEquals("DefaultZone", ccuApplication.floors.get(0).mRoomList.get(0).roomName .toString() );
			Assert.assertEquals(3, ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.size());
			Assert.assertEquals(1, ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeOutputs.size());
			Assert.assertEquals(1, ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeInputs.size());
			Assert.assertEquals("Kitchen", ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeOutputs.get(0).mName);
			Assert.assertEquals("Room1", ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeInputs.get(0).mName);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
}
