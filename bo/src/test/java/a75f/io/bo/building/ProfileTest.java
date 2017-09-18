package a75f.io.bo.building;

import junit.framework.Assert;

import org.junit.Test;

import a75f.io.bo.building.definitions.InputActuatorType;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;

/**
 * Created by samjithsadasivan isOn 9/7/17.
 */

public class ProfileTest
{
	@Test
	public void testProfiles() {
		CCUApplication ccuApplication = new CCUApplication();
		Node testSN = new Node();
		testSN.setAddress((short) 7000);
		//testSN.mRoomName = "75F";
		
		ccuApplication.setTitle("Test");
		Floor floor = new Floor(1, "webid", "Floor1");
		Zone zone = new Zone("DefaultZone");
        zone.getNodes().put(testSN.getAddress(), testSN);
		floor.mRoomList.add(zone);
		ZoneProfile p1 = new ZoneProfile()
		{
			@Override
			public short mapCircuit(Output output)
			{
				return 0;
			}
			
			
			@Override
			public void mapSeed(CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
			{
			}
		};
		ccuApplication.getFloors().add(floor);
		//ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.add(p1);
		
		Input ip1 = new Input();
		ip1.setAddress(testSN.getAddress());
		ip1.mName = "Room1";
		ip1.mInput = a75f.io.bo.building.definitions.Input.Analog1In;
		ip1.mInputActuatorType = InputActuatorType.ZeroTo10ACurrentTransformer;
		p1.getOutputs().add(ip1.getUuid());
		zone.addInputCircuit(testSN, p1, ip1);
		
		
		
		
		
		Output op1 = new Output();
		op1.setAddress(testSN.getAddress());
		op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op1.mName = "Kitchen";
		zone.addOutputCircuit(testSN, p1, op1);
		
		
		ZoneProfile p2 = new ZoneProfile()
		{
			@Override
			public short mapCircuit(Output output)
			{
				return 0;
			}
			
			
			@Override
			public void mapSeed(CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
			{
			}
		};
		//ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.add(p2);
		
		ZoneProfile p3 = new ZoneProfile()
		{
			
			@Override
			public short mapCircuit(Output output)
			{
				return 0;
			}
			
			
			@Override
			public void mapSeed(CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
			{
			}
		};
		//ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.add(p3);
		
		try
		{
			//String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
			//System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
			
			//CCUApplication deCcuApp = (CCUApplication) JsonSerializer.fromJson(ccu ,CCUApplication.class);
			Assert.assertEquals(1, ccuApplication.getFloors().size());
			Assert.assertEquals("DefaultZone", ccuApplication.getFloors().get(0).mRoomList.get(0).roomName .toString() );
			/*Assert.assertEquals(3, ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.size());
			Assert.assertEquals(1, ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeOutputs.size());
			Assert.assertEquals(1, ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeInputs.size());
			Assert.assertEquals("Kitchen", ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeOutputs.get(0).mName);
			Assert.assertEquals("Room1", ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeInputs.get(0).mName);*/
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
}
