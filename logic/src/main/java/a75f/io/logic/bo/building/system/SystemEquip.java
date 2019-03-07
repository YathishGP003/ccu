package a75f.io.logic.bo.building.system;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

/**
 * Created by samjithsadasivan on 11/19/18.
 */
// Caches system equips frequently used his config points.
public class SystemEquip
{
    private String equipRef;
    CCUHsApi hayStack;
    SystemRelayOp[] systemRelayArr = new SystemRelayOp[7];
    SystemAnalogOp[] systemAnalogArr = new SystemAnalogOp[4];
    public SystemEquip(String ref) {
        hayStack = CCUHsApi.getInstance();
        equipRef = ref;
        initSystemCache();
    }
    
    
    public void initSystemCache()
    {
        for (int i = 1; i <= 7; i++) {
            HashMap relay = hayStack.read("point and system and cmd and his and relay" + i);
            if (relay == null || relay.size() == 0) {
                systemRelayArr[i].setAvailable(false);
            } else {
                double enabled = hayStack.readDefaultVal("point and system and config and output and enabled and relay"+i);
                systemRelayArr[i].setEnabled(enabled > 0);
                double stageVal = hayStack.readDefaultVal("point and system and config and output and association and relay"+i);
                systemRelayArr[i].setRelayAssociation(stageVal);
            }
        }
    
        for (int i = 1; i <= 4; i++) {
            HashMap analog = hayStack.read("point and system and config and output and enabled and analog" + i);
            if (analog == null || analog.size() == 0) {
                systemAnalogArr[i].setAvailable(false);
            } else {
                double enabled = hayStack.readDefaultVal("point and system and config and output and enabled and analog"+i);
                systemAnalogArr[i].setEnabled(enabled > 0);
            }
        }
        
    }
    
    public String getEquipRef() {
        return equipRef;
    }
    
    public boolean getRelayOpEnabled(String relay) {
        return systemRelayArr[Integer.parseInt(relay.substring(relay.length()-1))].isEnabled();
    }
    
    public void setRelayOpEnabled(String relay, boolean enabled) {
        systemRelayArr[Integer.parseInt(relay.substring(relay.length()-1))].setEnabled(enabled);
    }
    
    public double getRelayOpAssociation(String relay) {
        return systemRelayArr[Integer.parseInt(relay.substring(relay.length()-1))].getRelayAssociation();
    }
    
    public void setRelayOpAssociation(String relay, double val) {
        systemRelayArr[Integer.parseInt(relay.substring(relay.length()-1))].setRelayAssociation(val);
    }
    
    public boolean getAnalogOpEnabled(String analog) {
        return systemAnalogArr[Integer.parseInt(analog.substring(analog.length()-1))].isEnabled();
    }
    
    public void setAnalogOpEnabled(String analog, boolean enabled) {
        systemAnalogArr[Integer.parseInt(analog.substring(analog.length()-1))].setEnabled(enabled);
    }
}
