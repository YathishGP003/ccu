package a75f.io.bo.building;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

/**
 * Created by samjithsadasivan on 9/7/17.
 */

public class SmartNodeTest
{
	
	@Test
	public void testSmartNode() {
		CCUApplication ccuApplication = new CCUApplication();
		SmartNode testSN1 = new SmartNode();
		testSN1.mAddress = 7000;
		testSN1.mRoomName = "Room1";
		
		SmartNode testSN2 = new SmartNode();
		testSN2.mAddress = 8000;
		testSN2.mRoomName = "Room2";
		
		SmartNode testSN3 = new SmartNode();
		testSN3.mAddress = 9000;
		testSN3.mRoomName = "Room3";
		
		
		ccuApplication.smartNodes.add(testSN1);
		ccuApplication.smartNodes.add(testSN2);
		ccuApplication.smartNodes.add(testSN3);
		
		ccuApplication.CCUTitle = "Test";
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
		op1.mSmartNodeAddress = testSN1.mAddress;
		UUID op1UD = UUID.randomUUID();
		op1.mUniqueID = op1UD;
		op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op1.mOutput = Output.Analog;
		op1.mName = "Dining Room";
		p1.smartNodeOutputs.add(op1);
		
		LightSmartNodeOutput op2 = new LightSmartNodeOutput();
		op2.mSmartNodeAddress = testSN2.mAddress;
		UUID op2UD = UUID.randomUUID();
		op2.mUniqueID = op2UD;
		op2.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op2.mOutput = Output.Relay;
		op2.mName = "Kitchen";
		p1.smartNodeOutputs.add(op2);
		
		LightSmartNodeOutput op3 = new LightSmartNodeOutput();
		op3.mSmartNodeAddress = testSN2.mAddress;
		UUID op3UD = UUID.randomUUID();
		op3.mUniqueID = op3UD;
		op3.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op3.mOutput = Output.Relay;
		op3.mName = "Bedroom";
		p1.smartNodeOutputs.add(op3);
		
		
		try
		{
			Assert.assertEquals(testSN2.mAddress, getSmartNodeAddressFromOpUUID (ccuApplication, op3UD));
			Assert.assertEquals(2, getConfiguredOpsforSmartnode (ccuApplication, testSN2));
			
			String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
			System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
			
			//CCUApplication deCcuApp = (CCUApplication) JsonSerializer.fromJson(ccuApplicationJSON ,CCUApplication.class);
			//Assert.assertEquals(1, deCcuApp.floors.size());
			
			
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	public int getSmartNodeAddressFromOpUUID(CCUApplication global, UUID opUUID) {
		LightProfile profile = (LightProfile) global.floors.get(0).mRoomList.get(0).zoneProfiles.get(0);
		for (LightSmartNodeOutput op : profile.smartNodeOutputs) {
			if (opUUID.equals(op.mUniqueID)) {
				return  op.mSmartNodeAddress;
			}
		}
		return 0;
	}
	
	public int getConfiguredOpsforSmartnode(CCUApplication global, SmartNode node) {
		LightProfile profile = (LightProfile) global.floors.get(0).mRoomList.get(0).zoneProfiles.get(0);
		int count = 0;
		for (LightSmartNodeOutput op : profile.smartNodeOutputs) {
			if (op.mSmartNodeAddress == node.mAddress)
				count++;
		}
		return count;
	}
}
