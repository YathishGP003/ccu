package a75f.io.bo.building;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

import static org.junit.Assert.*;

/**
 * Created by samjithsadasivan on 9/6/17.
 */
public class CCUApplicationTest
{
	@Test
	public void testCcuApplication() {
		
		CCUApplication ccuApplication = new CCUApplication();
		SmartNode testSN = new SmartNode();
		testSN.mAddress = 7000;
		testSN.mRoomName = "75F";
		ccuApplication.smartNodes.add(testSN);
		ccuApplication.CCUTitle = "Light Test";
		Floor floor = new Floor(1, "webid", "Floor1");
		floor.mRoomList.add(new Zone("75FRoom1"));
		LightProfile p1 = new LightProfile("Test Profile")
		{
			@Override
			public List<CcuToCmOverUsbSnControlsMessage_t> getControlsMessage()
			{
				return null;
			}
		};
		ccuApplication.floors.add(floor);
		ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.add(p1);
		LightSmartNodeOutput op1 = new LightSmartNodeOutput();
		op1.mSmartNodeAddress = testSN.mAddress;
		op1.mUniqueID = UUID.randomUUID();
		op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op1.mOutput = Output.Analog;
		op1.mName = "Dining Room";
		p1.smartNodeOutputs.add(op1);
		
		LightSmartNodeOutput op2 = new LightSmartNodeOutput();
		op1.mSmartNodeAddress = testSN.mAddress;
		op1.mUniqueID = UUID.randomUUID();
		op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op1.mOutput = Output.Relay;
		op1.mName = "Kitchen";
		p1.smartNodeOutputs.add(op2);
		
		try
		{
			String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
			System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
			
			CCUApplication deCcuApp = (CCUApplication) JsonSerializer.fromJson(ccuApplicationJSON ,CCUApplication.class);
			Assert.assertEquals(1, deCcuApp.floors.size());
			Assert.assertEquals("75FRoom1", deCcuApp.floors.get(0).mRoomList.get(0).roomName .toString() );
			Assert.assertEquals(2, deCcuApp.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeOutputs.size());
			Assert.assertEquals("Kitchen", deCcuApp.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeOutputs.get(1).mName);
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
}