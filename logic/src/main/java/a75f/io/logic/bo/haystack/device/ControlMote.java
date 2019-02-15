package a75f.io.logic.bo.haystack.device;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

/**
 * Created by samjithsadasivan on 12/6/18.
 */

public class ControlMote
{
    private Site   site;
    private String deviceRef;
    public ControlMote(String siteRef) {
        
        HashMap device = CCUHsApi.getInstance().read("device and cm");
        if (device != null && device.size() > 0) {
            CcuLog.d(L.TAG_CCU_DEVICE," CM device exists");
            return;
        }
        site = new Site.Builder().setHashMap(CCUHsApi.getInstance().read(Tags.SITE)).build();
        
        Device d = new Device.Builder()
                           .setDisplayName("CM-device")
                           .addMarker("network")
                           .addMarker("cm")
                           .setSiteRef(site.getId())
                           .build();
        deviceRef = CCUHsApi.getInstance().addDevice(d);
        createPoints();
    }
    
    public void createPoints() {
        addRelayStatePoint("relay1");
        addRelayStatePoint("relay2");
        addRelayStatePoint("relay3");
        addRelayStatePoint("relay4");
        addRelayStatePoint("relay5");
        addRelayStatePoint("relay6");
        addRelayStatePoint("relay7");
        addRelayStatePoint("relay8");
    
        addAnalogOutValPoint("analog1");
        addAnalogOutValPoint("analog2");
        addAnalogOutValPoint("analog3");
        addAnalogOutValPoint("analog4");
    
        setRelayState("relay1",0);
        setRelayState("relay2",0);
        setRelayState("relay3",0);
        setRelayState("relay4",0);
        setRelayState("relay5",0);
        setRelayState("relay6",0);
        setRelayState("relay7",0);
        setRelayState("relay8",0);
        
    
        setAnalogOut("analog1",0);
        setAnalogOut("analog2",0);
        setAnalogOut("analog3",0);
        setAnalogOut("analog4",0);
    }
    
    
    private void addAnalogOutValPoint(String analog) {
        
        RawPoint p = new RawPoint.Builder()
                          .setDisplayName(site.getDisplayName()+"-CM-"+analog+"Out")
                          .setDeviceRef(deviceRef)
                          .setSiteRef(site.getId())
                          .addMarker(analog).addMarker("his").addMarker("system").addMarker("out")
                          .setTz(site.getTz())
                          .build();
        CCUHsApi.getInstance().addPoint(p);
    }
    
    public static double getAnalogOut(String analog)
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and his and system and out and "+analog);
    }
    
    public static void setAnalogOut(String analog, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and his and system and out and "+analog, val);
    }
    
    private void addRelayStatePoint(String relay){
        RawPoint p = new RawPoint.Builder()
                             .setDisplayName(site.getDisplayName()+"-"+relay+"State")
                             .setDeviceRef(deviceRef)
                             .setSiteRef(site.getId())
                             .addMarker(relay).addMarker("his").addMarker("system").addMarker("state")
                             .setTz(site.getTz())
                             .build();
        CCUHsApi.getInstance().addPoint(p);
    }
    
    public static double getRelayState(String relay)
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and his and system and state and "+relay);
    }
    public static void setRelayState(String relay, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and his and system and state and "+relay, val);
    }
    
}
