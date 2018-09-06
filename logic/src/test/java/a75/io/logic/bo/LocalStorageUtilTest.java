package a75.io.logic.bo;

import java.util.UUID;

import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.Floor;
import a75f.io.logic.bo.building.lights.LightProfile;
import a75f.io.logic.bo.building.lights.LightProfileConfiguration;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.device.json.serializers.JsonSerializer;

/**
 * Created by samjithsadasivan isOn 8/28/17.
 */

public class LocalStorageUtilTest
{
	
	//@Test
	public void persistCcuAppSettingsTest()
	{
		CCUApplication orgCcu = new CCUApplication();
		orgCcu.getFloors().add(new Floor(0, null, "FirstFloor"));
		orgCcu.getFloors().get(0).mRoomList.add(new Zone("FirstRoom"));
		UUID analog15kUUID = UUID.randomUUID();
		short smartNodeAddress = 5000;
		LightProfileConfiguration lightProfileConfiguration = new LightProfileConfiguration();
		
		orgCcu.setTitle("Light Test");
		LightProfile lightProfile5K = new LightProfile();
		Output output5K = new Output();
		output5K.setAddress(smartNodeAddress);
		//output5K.mUniqueID = analog15kUUID;
		output5K.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		//output5K.mOutput = Output.Analog;
		lightProfileConfiguration.getOutputs().add(output5K);
		
		
		Output relayOneOp = new Output();
		relayOneOp.setAddress(smartNodeAddress);
		//relayOneOp.mUniqueID = UUID.randomUUID();
		//relayOneOp.mOutput = Output.Relay;
		relayOneOp.setName( "Relay1");
		relayOneOp.setPort(Port.RELAY_ONE);
		relayOneOp.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
		relayOneOp.mOutputAnalogActuatorType = OutputAnalogActuatorType.TwoToTenV;
		lightProfileConfiguration.getOutputs().add(relayOneOp);
		
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
