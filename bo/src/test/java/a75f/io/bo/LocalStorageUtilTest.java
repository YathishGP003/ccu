package a75f.io.bo;

import org.junit.Test;

import java.util.UUID;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.Floor;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.Node;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.json.serializers.JsonSerializer;

/**
 * Created by samjithsadasivan isOn 8/28/17.
 */

public class LocalStorageUtilTest
{
	
	@Test
	public void persistCcuAppSettingsTest()
	{
		CCUApplication orgCcu = new CCUApplication();
		orgCcu.getFloors().add(new Floor(0, null, "FirstFloor"));
		orgCcu.getFloors().get(0).mRoomList.add(new Zone("FirstRoom"));
		UUID analog15kUUID = UUID.randomUUID();
		short smartNodeAddress = 5000;
		Node node5K = new Node();
		node5K.setAddress(smartNodeAddress);
		
		
		orgCcu.setTitle("Light Test");
		LightProfile lightProfile5K = new LightProfile();
		Output output5K = new Output();
		output5K.setAddress(node5K.getAddress());
		//output5K.mUniqueID = analog15kUUID;
		output5K.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		//output5K.mOutput = Output.Analog;
		orgCcu.getFloors().get(0).mRoomList.get(0).addOutputCircuit(node5K, lightProfile5K,
                output5K);
		Output relayOneOp = new Output();
		relayOneOp.setAddress(node5K.getAddress());
		//relayOneOp.mUniqueID = UUID.randomUUID();
		//relayOneOp.mOutput = Output.Relay;
		relayOneOp.setName( "Relay1");
		relayOneOp.setPort(Port.RELAY_ONE);
		relayOneOp.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
		relayOneOp.mOutputAnalogActuatorType = OutputAnalogActuatorType.TwoToTenV;
		orgCcu.getFloors().get(0).mRoomList.get(0).addOutputCircuit(node5K, lightProfile5K,
                relayOneOp);
		try
		{
			String jsonString = JsonSerializer.toJson(orgCcu, false);
			System.out.println(jsonString);
			//LocalStorage.getCCUSettings().edit().putString("storagetest", jsonString).commit();
			CCUApplication ccuFrmJson = (CCUApplication) JsonSerializer.fromJson(jsonString, CCUApplication.class);
			System.out.println(ccuFrmJson.getFloors().get(0).mRoomList.size());
		}
		catch (Exception c)
		{
			c.printStackTrace();
		}
	}
}
