package a75f.io.bo.building;

/**
 * Created by samjithsadasivan isOn 9/7/17.
 */

public class SmartNodeTest
{
	
	//Why aren't tests updating when branch merge.

//	@Test
//	public void testSmartNode() {
//		CCUApplication ccuApplication = new CCUApplication();
//		Node testSN1 = new Node();
//		testSN1.setAddress((short) 7000);
//		//testSN1.mRoomName = "Room1";
//
//		Node testSN2 = new Node();
//		testSN2.setAddress((short) 8000);
//		//testSN2.mRoomName = "Room2";
//
//		Node testSN3 = new Node();
//		testSN3.setAddress((short) 9000);
//		//testSN3.mRoomName = "Room3";
//        Zone z = new Zone("75FRoom1");
//		ccuApplication.setTitle("Test");
//		Floor floor = new Floor(1, "webid", "Floor1");
//
//
//
//        ccuApplication.getFloors().add(floor);
//        floor.mRoomList.add(z);
//
//
//        LightProfile p1 = (LightProfile) z.findProfile(ProfileType.LIGHT);
//
//		//ccuApplication.floors.get(0).mRoomList.get(0).zoneProfiles.add(p1);
//		Output op1 = new Output();
//		op1.setAddress(testSN1.getAddress());
//		op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
//		op1.mName = "Dining Room";
//		z.addOutputCircuit(testSN1, z.findProfile(ProfileType.LIGHT), op1);
//
//		Output op2 = new Output();
//		op2.setAddress(testSN2.getAddress());
//		op2.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
//		op2.mName = "Kitchen";
//        z.addOutputCircuit(testSN2, z.findProfile(ProfileType.LIGHT), op2);
//
//
//
//		Output op3 = new Output();
//		op3.setAddress(testSN2.getAddress());
//		op3.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
//		op3.mName = "Bedroom";
//        z.addOutputCircuit(testSN2, z.findProfile(ProfileType.LIGHT), op3);
//
//
//		try
//		{
//			Assert.assertEquals(testSN2.getAddress(), getSmartNodeAddressFromOpUUID (ccuApplication, op3.getUuid()));
//			Assert.assertEquals(2, getConfiguredOpsforSmartnode (ccuApplication, testSN2));
//
//			String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
//			System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
//
//			//CCUApplication deCcuApp = (CCUApplication) JsonSerializer.fromJson(ccuApplicationJSON ,CCUApplication.class);
//			//Assert.assertEquals(1, deCcuApp.floors.size());
//
//
//
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//			Assert.assertTrue(false);
//		}
//	}
//
//	public int getSmartNodeAddressFromOpUUID(CCUApplication global, UUID opUUID) {
//        return global.getFloors().get(0).mRoomList.get(0).getOutputs().get(opUUID).getAddress();
//	}
//
//	public int getConfiguredOpsforSmartnode(CCUApplication global, Node node) {
//		return global.getFloors().get(0).mRoomList.get(0).getNodes().get(node.getAddress()).getOutputs().size();
//	}
}
