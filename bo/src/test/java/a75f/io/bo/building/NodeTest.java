package a75f.io.bo.building;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.OverrideType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.json.serializers.JsonSerializer;

/**
 * Created by samjithsadasivan isOn 9/7/17.
 */

public class NodeTest
{


    private Schedule schedule;


    @Before
    public void setUpMockSchedule()
    {
        //Mock schedule M-F, 8AM - 5:30PM turn isOn lights to value 100.
        schedule = new Schedule();
        int[] ints = {1, 2, 3, 4, 5};
        ArrayList<Integer> intsaslist = new ArrayList<Integer>();
        for(int i : ints)
        { //as
            int z = 0;
            intsaslist.add(i);
        }
        schedule.setDays(intsaslist);
        schedule.setSt(8, 00);
        schedule.setEt(17,30);
        schedule.setVal((short) 100);
    }


    @Test
    public void testSmartNode()
    {
        CCUApplication ccuApplication = new CCUApplication();
        short testSN1 = 7000;
        short testSN2 = 8000;
        short testSN3 = 9000;
        
        ccuApplication.setTitle("Test");
        Floor floor = new Floor(1, "webid", "Floor1");
        Zone z = new Zone("75FRoom1");
        floor.mRoomList.add(z);
        LightProfile p1 = (LightProfile) z.findProfile(ProfileType.LIGHT);

        LightProfileConfiguration testSn1Config = new LightProfileConfiguration();
        LightProfileConfiguration testSN2Config = new LightProfileConfiguration();
        ccuApplication.getFloors().add(floor);
        Output op1 = new Output();
        op1.setAddress(testSN1);
        op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
        op1.mName = "Dining Room";
        p1.getLightProfileConfiguration().put(testSN1, testSn1Config);
        
        Output op2 = new Output();
        op2.setAddress(testSN2);
        op2.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
        op2.mName = "Kitchen";
        testSN2Config.getOutputs().add(op2);
        
        Output op3 = new Output();
        op3.setAddress(testSN2);
        op3.mOutputAnalogActuatorType = OutputAnalogActuatorType.TwoToTenV;
        op3.mName = "Bedroom";
        op3.setPort(Port.ANALOG_OUT_ONE);
        
        testSN2Config.getOutputs().add(op3);
        
        p1.getLightProfileConfiguration().put(testSN2, testSN2Config);
        
        short lightProfile = z.findProfile(ProfileType.LIGHT).mapCircuit(op3);
        Assert.assertEquals(lightProfile, 20);
        System.out.println("Mapping zone profile: " + z.findProfile(ProfileType.LIGHT).mapCircuit(op3));

        z.findProfile(ProfileType.LIGHT).addSchedule(schedule);
        op3.setOverride(System.currentTimeMillis(), OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND, (short) 100);

        short zoneProfile = z.findProfile(ProfileType.LIGHT).mapCircuit(op3);
        System.out.println("Mapping zone profile: " + z.findProfile(ProfileType.LIGHT).mapCircuit(op3));
        Assert.assertEquals(zoneProfile, 100);
        try
        {
            Assert.assertEquals(testSN2, op3.getAddress());
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

}
