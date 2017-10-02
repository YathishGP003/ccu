package a75f.io.bo.building;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;

import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.json.serializers.JsonSerializer;

/**
 * Created by samjithsadasivan isOn 9/6/17.
 */
public class CCUApplicationTest
{
    
    @Test
    public void testCcuApplication()
    {
        CCUApplication ccuApplication = new CCUApplication();
        short nodeAddress = 7000;
        ccuApplication.setTitle("Light Test");
        Floor floor = new Floor(1, "webid", "Floor1");
        Zone zone = new Zone("75FRoom1");
        floor.mRoomList.add(zone);
        LightProfile p1 = (LightProfile) zone.findProfile(ProfileType.LIGHT);
        LightProfileConfiguration lightProfileConfiguration = new LightProfileConfiguration();
        p1.getProfileConfiguration().put(nodeAddress, lightProfileConfiguration);
        ccuApplication.getFloors().add(floor);
        //ccuApplication.floors.get(0).mRoomList.get(0).add(p1);
        Output op1 = new Output();
        op1.setAddress(nodeAddress);
        op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
        op1.mName = "Dining Room";
        lightProfileConfiguration.getOutputs().add(op1);
        Output op2 = new Output();
        op2.setAddress(nodeAddress);
        op2.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
        op2.mName = "Kitchen";
        lightProfileConfiguration.getOutputs().add(op2);
        try
        {
            String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
            System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
            CCUApplication deCcuApp = (CCUApplication) JsonSerializer
                                                               .fromJson(ccuApplicationJSON, CCUApplication.class);
            Assert.assertEquals(1, deCcuApp.getFloors().size());
            Assert.assertEquals("75FRoom1", deCcuApp.getFloors().get(0).mRoomList.get(0).roomName
                                                    .toString());
            Assert.assertEquals(2, ((LightProfile) deCcuApp.getFloors().get(0).mRoomList.get(0)
                                                                                        .findProfile(ProfileType.LIGHT))
                                           .getProfileConfiguration().get(nodeAddress)
                                           .getOutputs().size());
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