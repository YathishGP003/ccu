package a75.io.bo.haystack;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.json.serializers.JsonSerializer;
import a75f.io.device.mesh.LSmartNode;
import a75f.io.device.mesh.ThermistorUtil;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.vav.VAVLogicalMap;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class MeshUpdateTest
{
    
    @Test
    public void testMeshUpdate() {
        CCUHsApi api = new CCUHsApi();
        VAVLogicalMap m = new VAVLogicalMap(ProfileType.VAV_REHEAT, 7000);
        m.createHaystackPoints(new VavProfileConfiguration(), null, null);
        ArrayList points = CCUHsApi.getInstance().readAll("point");
        for (Object a : points) {
            System.out.println(a);
        }
    
        short nodeAddr = 7000;
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+nodeAddr+"\"");
        if (device != null && device.size() > 0)
        {
            HashMap currentTemp = CCUHsApi.getInstance().read("point and physical and current and deviceRef == \""+device.get("id")+"\"");
            System.out.println(currentTemp);
            HashMap logicalTemp = CCUHsApi.getInstance().read("point and id=="+currentTemp.get("pointRef"));
            
            CCUHsApi.getInstance().writeDefaultValById(currentTemp.get("id").toString(), 75.0);
            double newTemp = CCUHsApi.getInstance().readDefaultValById(currentTemp.get("id").toString());
            System.out.println(newTemp );
    
            System.out.println(CCUHsApi.getInstance().readDefaultValById(logicalTemp.get("id").toString() ));
            CCUHsApi.getInstance().writeDefaultValById(logicalTemp.get("id").toString(), 75.0);
            System.out.println(CCUHsApi.getInstance().readDefaultValById(logicalTemp.get("id").toString() ));
        }
    }
    
    @Test
    public void testControlMessage(){
        CCUHsApi hayStack = new CCUHsApi();
        VAVLogicalMap m = new VAVLogicalMap(ProfileType.VAV_REHEAT, 7000);
        m.createHaystackPoints(new VavProfileConfiguration(), null, null);
        short node = 7000;
        HashMap device = hayStack.read("device and addr == \""+node+"\"");
    
        CcuToCmOverUsbSnControlsMessage_t controlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
        controlsMessage_t.smartNodeAddress.set(node);
        controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
        
        if (device != null && device.size() > 0)
        {
            ArrayList<HashMap> physicalOpPoints= hayStack.readAll("point and physical and output and deviceRef == \""+device.get("id")+"\"");
            
            for (Object a : physicalOpPoints) {
                System.out.println(a);
            }
            for (HashMap opPoint : physicalOpPoints) {
                
                HashMap logicalOpPoint = hayStack.read("point and id=="+opPoint.get("pointRef"));
                double logicalVal = hayStack.readDefaultValById(logicalOpPoint.get("id").toString());
                
                String port = opPoint.get("port").toString();
            
                short mappedVal = (LSmartNode.isAnalog(port) ? LSmartNode.mapAnalogOut(opPoint.get("type").toString(), (short)logicalVal)
                                           : LSmartNode.mapDigitalOut(opPoint.get("type").toString(), logicalVal > 0));
                hayStack.writeDefaultValById(opPoint.get("id").toString(), (double)mappedVal);
            
                LSmartNode.getSmartNodePort(controlsMessage_t.controls, port)
                          .set(mappedVal);
    
                try
                {
                    String structString = JsonSerializer.toJson(controlsMessage_t, true);
                    System.out.println(structString);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                
                
            }
        }
    }
    
    @Test
    public void testThermistor() {
        System.out.println(ThermistorUtil.getThermistorValueToTemp(14000));
    }
}
