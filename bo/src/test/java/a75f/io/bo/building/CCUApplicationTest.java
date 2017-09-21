package a75f.io.bo.building;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.json.serializers.JsonSerializer;

/**
 * Created by samjithsadasivan isOn 9/6/17.
 */
public class CCUApplicationTest
{


	@Test
	public void testCcuApplication() {

		CCUApplication ccuApplication = new CCUApplication();
		Node testSN = new Node();
		testSN.setAddress((short) 7000);

		ccuApplication.setTitle("Light Test");
		Floor floor = new Floor(1, "webid", "Floor1");
        Zone zone = new Zone("75FRoom1");

        zone.getNodes().put((short)7000, testSN);
		floor.mRoomList.add(zone);


		LightProfile p1 = (LightProfile) zone.findProfile(ProfileType.LIGHT);
		ccuApplication.getFloors().add(floor);

		//ccuApplication.floors.get(0).mRoomList.get(0).add(p1);
		Output op1 = new Output();
		op1.setAddress(testSN.getAddress());
		UUID op1UD = UUID.randomUUID();
		op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op1.mName = "Dining Room";
		zone.addOutputCircuit(testSN, p1, op1);


		Output op2 = new Output();
		op2.setAddress(testSN.getAddress());
		op2.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		op2.mName = "Kitchen";
		zone.addOutputCircuit(testSN, p1, op2);


		try
		{
			String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
			System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");

			CCUApplication deCcuApp = (CCUApplication) JsonSerializer.fromJson(ccuApplicationJSON ,CCUApplication.class);
			Assert.assertEquals(1, deCcuApp.getFloors().size());
			Assert.assertEquals("75FRoom1", deCcuApp.getFloors().get(0).mRoomList.get(0).roomName .toString() );
			Assert.assertEquals(2, deCcuApp.getFloors().get(0).mRoomList.get(0)
                                           .findProfile(ProfileType.LIGHT).getOutputs().size());
            Assert.assertEquals(2, deCcuApp.getFloors().get(0).mRoomList.get(0).getNodes().get(
                    (short)7000).getOutputs().size());


			//Assert.assertEquals("Kitchen", deCcuApp.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeOutputs.get(1).mName);
			//Assert.assertEquals(op1UD, deCcuApp.floors.get(0).mRoomList.get(0).zoneProfiles.get(0).smartNodeOutputs.get(0).mUniqueID);

		}
		catch (IOException e)
		{
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}

}