package a75f.io.logic.bo.haystack.device;

import a75f.io.logic.bo.haystack.Device;
import a75f.io.logic.bo.haystack.RawPoint;

/**
 * Created by samjithsadasivan on 9/5/18.
 */

public class SmartNode
{
    int smartNodeAddress;
    
    public RawPoint analog1In;
    public RawPoint analog2In;
    public RawPoint analog1Out;
    public RawPoint analog2Out;
    public RawPoint relay1;
    public RawPoint relay2;
    
    public String deviceRef;
    
    public SmartNode(int address) {
        deviceRef = new Device.Builder()
                .setDisplayName("SN-"+address)
                .addMarker("network")
                .build();
        
        createPoints();
    }
    
    public void createPoints() {
        analog1In = new RawPoint.Builder()
                                .setDisplayName("Analog1In-"+smartNodeAddress)
                                .setDeviceRef(deviceRef)
                                .setPort("Analog1In")
                                .setType("2-10v")
                                .build();
    
        analog2In = new RawPoint.Builder()
                            .setDisplayName("Analog2In-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setPort("Analog2In")
                            .setType("2-10v")
                            .build();
    
        analog1Out = new RawPoint.Builder()
                            .setDisplayName("Analog1Out-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setPort("Analog1Out")
                            .setType("2-10v")
                            .build();
    
        analog2Out = new RawPoint.Builder()
                             .setDisplayName("Analog2Out-"+smartNodeAddress)
                             .setDeviceRef(deviceRef)
                             .setPort("Analog2Out")
                             .setType("2-10v")
                             .build();
    
        relay1 = new RawPoint.Builder()
                             .setDisplayName("relay1-"+smartNodeAddress)
                             .setDeviceRef(deviceRef)
                             .setPort("relay1")
                             .setType("NO")
                             .build();
    
        relay2 = new RawPoint.Builder()
                         .setDisplayName("relay2-"+smartNodeAddress)
                         .setDeviceRef(deviceRef)
                         .setPort("relay2")
                         .setType("NO")
                         .build();
    }
    

}
