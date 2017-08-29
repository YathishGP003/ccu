package a75f.io.util;

import org.junit.Test;

import java.util.UUID;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.Floor;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.json.serializers.JsonSerializer;

/**
 * Created by samjithsadasivan on 8/28/17.
 */

public class LocalStorageUtilTest
{
	
	@Test
	public void persistCcuAppSettingsTest() {
		CCUApplication orgCcu = new CCUApplication();
		orgCcu.floors.add(new Floor(0,null,"FirstFloor"));
		orgCcu.floors.get(0).mRoomList.add(new Zone("FirstRoom"));
		UUID analog15kUUID = UUID.randomUUID();
		short smartNodeAddress = 5000;
		SmartNode smartNode5K = new SmartNode();
		smartNode5K.mAddress = smartNodeAddress;
		smartNode5K.mRoomName = "SmartNode roomName";
		orgCcu.smartNodes.add(smartNode5K);
		orgCcu.CCUTitle = "Light Test";
		
		LightProfile lightProfile5K = new LightProfile("Light Profile");
		//		ccuApplication.floors.get(0).addZone("5000 test zone");
		orgCcu.floors.get(0).mRoomList.get(0).zoneProfiles.add(lightProfile5K);
		SmartNodeOutput smartNodeOutput5K = new SmartNodeOutput();
		smartNodeOutput5K.mSmartNodeAddress = smartNode5K.mAddress;
		smartNodeOutput5K.mUniqueID = analog15kUUID;
		smartNodeOutput5K.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		smartNodeOutput5K.mOutput = Output.Analog;
		
		SmartNodeOutput relayOneOp = new SmartNodeOutput();
		relayOneOp.mSmartNodeAddress = smartNode5K.mAddress;
		relayOneOp.mUniqueID = UUID.randomUUID();
		relayOneOp.mOutput = Output.Relay;
		relayOneOp.mName = "Relay1";
		relayOneOp.mSmartNodePort = Port.RELAY_ONE;
		relayOneOp.mLightOutput=(short)1;
		relayOneOp.mOutputRelayActuatorType= OutputRelayActuatorType.NormallyClose;
		relayOneOp.mOutputAnalogActuatorType= OutputAnalogActuatorType.TwoToTenV;
		
		lightProfile5K.smartNodeOutputs.add(relayOneOp);
		
		try
		{
			String jsonString = JsonSerializer.toJson(orgCcu, false);
			System.out.println(jsonString);
			//LocalStorage.getCCUSettings().edit().putString("storagetest", jsonString).commit();
			
			CCUApplication ccuFrmJson = (CCUApplication) JsonSerializer.fromJson(jsonString, CCUApplication.class);
			System.out.println(ccuFrmJson.smartNodes.size());
		} catch (Exception c){
			c.printStackTrace();
		}
		
		
	}
}
