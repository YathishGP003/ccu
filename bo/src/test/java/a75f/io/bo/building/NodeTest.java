package a75f.io.bo.building;

import android.provider.ContactsContract;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;

/**
 * Created by samjithsadasivan isOn 9/7/17.
 */

public class NodeTest
{

    @Test
    public void testSmartNode()
    {
        CCUApplication ccuApplication = new CCUApplication();
        Node testSN1 = new Node();
        testSN1.setAddress((short) 7000);
        Node testSN2 = new Node();
        testSN2.setAddress((short) 8000);
        Node testSN3 = new Node();
        testSN3.setAddress((short) 9000);
        ccuApplication.setTitle("Test");
        Floor floor = new Floor(1, "webid", "Floor1");
        Zone z = new Zone("75FRoom1");
        floor.mRoomList.add(z);
        LightProfile p1 = (LightProfile) z.findProfile(ProfileType.LIGHT);

        z.getNodes().put(testSN1.getAddress(), testSN1);
        z.getNodes().put(testSN2.getAddress(), testSN2);
        z.getNodes().put(testSN3.getAddress(), testSN3);
        ccuApplication.getFloors().add(floor);
        Output op1 = new Output();
        op1.setAddress(testSN1.getAddress());
        op1.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
        op1.mName = "Dining Room";
        z.addOutputCircuit(testSN1, p1, op1);
        Output op2 = new Output();
        op2.setAddress(testSN2.getAddress());
        op2.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
        op2.mName = "Kitchen";
        z.addOutputCircuit(testSN2, p1, op2);
        Output op3 = new Output();
        op3.setAddress(testSN2.getAddress());
        op3.mOutputAnalogActuatorType = OutputAnalogActuatorType.TwoToTenV;
        op3.mName = "Bedroom";
        op3.setPort(Port.ANALOG_OUT_ONE);
        z.addOutputCircuit(testSN2, p1, op3);
        short lightProfile = z.findProfile(ProfileType.LIGHT).mapCircuit(op3);
        Assert.assertEquals(lightProfile, 20);
        System.out.println("Mapping zone profile: " + z.findProfile(ProfileType.LIGHT).mapCircuit(op3));

        short zoneProfile = z.findProfile(ProfileType.LIGHT).mapCircuit(op3);
        System.out.println("Mapping zone profile: " + z.findProfile(ProfileType.LIGHT).mapCircuit(op3));
        Assert.assertEquals(zoneProfile, 100);
        try
        {
            Assert.assertEquals(testSN2.getAddress(), getSmartNodeAddressFromOpUUID(ccuApplication, op3.getUuid()));
            Assert.assertEquals(2, getConfiguredOpsforSmartnode(ccuApplication, testSN2));
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


    public int getSmartNodeAddressFromOpUUID(CCUApplication global, UUID opUUID)
    {
        Zone profile = global.getFloors().get(0).mRoomList.get(0);
        return profile.getOutputs().get(opUUID).getAddress();
    }


    public int getConfiguredOpsforSmartnode(CCUApplication global, Node node)
    {
        Zone profile = global.getFloors().get(0).mRoomList.get(0);
        int count = 0;
        for (UUID output : node.getOutputs())
        {
            if (profile.getOutputs().get(output).getAddress() == node.getAddress())
            {
                count++;
            }
        }
        return count;
    }
}
