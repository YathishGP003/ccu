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
    SystemRelayOp[] systemRelayArr = new SystemRelayOp[8];
    SystemAnalogOp[] systemAnalogArr = new SystemAnalogOp[5];
    public SystemEquip(String ref) {
        hayStack = CCUHsApi.getInstance();
        equipRef = ref;
        initSystemCache();
    }
    
    
    public void initSystemCache()
    {
        for (int i = 1; i <= 7; i++) {
            HashMap relay = hayStack.read("point and system and cmd and his and relay" + i);
            SystemRelayOp op = new SystemRelayOp();
            if (relay == null || relay.size() == 0) {
                op.setAvailable(false);
            } else {
                double enabled = hayStack.readDefaultVal("point and system and config and output and enabled and relay"+i);
                op.setEnabled(enabled > 0);
                double stageVal = hayStack.readDefaultVal("point and system and config and output and association and relay"+i);
                op.setRelayAssociation(stageVal);
            }
            systemRelayArr[i] = op;
        }
    
        for (int i = 1; i <= 4; i++) {
            HashMap analog = hayStack.read("point and system and config and output and enabled and analog" + i);
            SystemAnalogOp op = new SystemAnalogOp();
            if (analog == null || analog.size() == 0) {
                op.setAvailable(false);
            } else {
                double enabled = hayStack.readDefaultVal("point and system and config and output and enabled and analog"+i);
                op.setEnabled(enabled > 0);
            }
            systemAnalogArr[i] = op;
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
    
    public boolean getConfigEnabled(String config) {
        if (config.contains("relay")) {
            return getRelayOpEnabled(config);
        } else if (config.contains("analog")) {
            return getAnalogOpEnabled(config);
        }
        return false;
    }
    
    public void setConfigEnabled(String config, double val) {
        if (config.contains("relay")) {
            setRelayOpEnabled(config, val > 0);
        } else if (config.contains("analog")) {
            setAnalogOpEnabled(config, val> 0);
        }
        
    }
}
