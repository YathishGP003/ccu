package a75f.io.logic.bo.haystack.device;

import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.RawPoint;

/**
 * Created by samjithsadasivan on 9/5/18.
 */

public class SmartNode
{
    int smartNodeAddress;
    
    public RawPoint analog1In;
    public RawPoint analog2In;
    public RawPoint th1In;
    public RawPoint th2In;
    public RawPoint analog1Out;
    public RawPoint analog2Out;
    public RawPoint relay1;
    public RawPoint relay2;
    public RawPoint currentTemp;
    
    public String deviceRef;
    public String siteRef;
    
    public SmartNode(int address, String site) {
        deviceRef = new Device.Builder()
                .setDisplayName("SN-"+address)
                .addMarker("network")
                .setAddr(address)
                .setSiteRef(site)
                .build();
        smartNodeAddress = address;
        siteRef = site;
        createPoints();
    }
    
    private void createPoints() {
        analog1In = new RawPoint.Builder()
                                .setDisplayName("Analog1In-"+smartNodeAddress)
                                .setDeviceRef(deviceRef)
                                .setSiteRef(siteRef)
                                .setPort(Port.ANALOG_IN_ONE.toString())
                                .setType("0-10v")
                                .addMarker("input").addMarker("his")
                                .setTz("Chicago")
                                .build();
    
        
        analog2In = new RawPoint.Builder()
                            .setDisplayName("Analog2In-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.ANALOG_IN_TWO.toString())
                            .setType("0-10v")
                            .addMarker("input").addMarker("his")
                            .setTz("Chicago")
                            .build();
    
        th1In = new RawPoint.Builder()
                            .setDisplayName("Th1In-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.TH1_IN.toString())
                            .addMarker("input").addMarker("his")
                            .setTz("Chicago")
                            .build();
    
        th2In = new RawPoint.Builder()
                        .setDisplayName("Th2In-"+smartNodeAddress)
                        .setDeviceRef(deviceRef)
                        .setSiteRef(siteRef)
                        .setPort(Port.TH2_IN.toString())
                        .addMarker("input").addMarker("his")
                        .setTz("Chicago")
                        .build();
    
        analog1Out = new RawPoint.Builder()
                            .setDisplayName("Analog1Out-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.ANALOG_OUT_ONE.toString())
                            .setType("0-10v")
                            .addMarker("output").addMarker("his")
                            .setTz("Chicago")
                            .build();
    
        analog2Out = new RawPoint.Builder()
                             .setDisplayName("Analog2Out-"+smartNodeAddress)
                             .setDeviceRef(deviceRef)
                             .setSiteRef(siteRef)
                             .setPort(Port.ANALOG_OUT_TWO.toString())
                             .setType("0-10v")
                             .addMarker("output").addMarker("his")
                             .setTz("Chicago")
                             .build();
    
        relay1 = new RawPoint.Builder()
                             .setDisplayName("relay1-"+smartNodeAddress)
                             .setDeviceRef(deviceRef)
                             .setSiteRef(siteRef)
                             .setPort(Port.RELAY_ONE.toString())
                             .setType("NO")
                             .addMarker("output").addMarker("his")
                             .setTz("Chicago")
                             .build();
    
        relay2 = new RawPoint.Builder()
                         .setDisplayName("relay2-"+smartNodeAddress)
                         .setDeviceRef(deviceRef)
                         .setSiteRef(siteRef)
                         .setPort(Port.RELAY_TWO.toString())
                         .setType("NO")
                         .addMarker("output").addMarker("his")
                         .setTz("Chicago")
                         .build();
    
        currentTemp = new RawPoint.Builder()
                         .setDisplayName("currentTemp-"+smartNodeAddress)
                         .setDeviceRef(deviceRef)
                         .setSiteRef(siteRef)
                         .addMarker("input").addMarker("his")
                         .setPort(Port.RTH.toString())
                         .setTz("Chicago")
                         .build();
    }

}
