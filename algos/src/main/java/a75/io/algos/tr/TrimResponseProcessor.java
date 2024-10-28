package a75.io.algos.tr;


import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 6/4/18.
 */

public class TrimResponseProcessor
{
    public double setPoint;
    
    public SystemTrimResponse trSetting;
    
    public ArrayList<TrimResetListener> trListeners;
    
    public int minuteCounter;//assuming TR processed every minute

    public TrimResponseProcessor(SystemTrimResponse tr) {
        trSetting = tr;
        setPoint = tr.getSP0();
        trListeners = new ArrayList<>();
    }
    
    
    public double getSetPoint() {
        return setPoint;
    }
    
    public SystemTrimResponse getTrSetting() {
        return trSetting;
    }
    
    public void addTRListener(TrimResetListener l) {
        trListeners.add(l);
    }

    public void processResetResponse() {
        if (++minuteCounter < trSetting.getTd() || !isSystemCooling()) {
            trSetting.resetRequest();
            return;
        }
    
        double sp = setPoint;
        
        double netRequests = trSetting.getR() - trSetting.getI();
        
        //TR System responds only when the net request count is positive
        if (netRequests > 0) {
            double response = netRequests * trSetting.getSPres();
            if ((response < 0 && response < trSetting.getSPresmax()) ||
                    (response > 0 && response > trSetting.getSPresmax())) {
                response = trSetting.getSPresmax();
            }
            sp += response;
            for (TrimResetListener l : trListeners) {
                l.handleSystemReset();
            }
        } else {
            sp += trSetting.getSPtrim();
        }
        trSetting.resetRequest();
        
        if (sp < trSetting.getSPmin()) {
            sp = trSetting.getSPmin();
        } else if (sp > trSetting.getSPmax()) {
            sp = trSetting.getSPmax();
        }
        
        setPoint = sp;
        CcuLog.d("CCU_SYSTEM", "setpoint "+setPoint);
    }

    private boolean isSystemCooling() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object, Object> systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule");
        return CCUHsApi.getInstance().readHisValByQuery("point and (domainName == \""+DomainName.operatingMode+"\") " +
                "or (operating and mode) and equipRef == \"" + systemEquip.get("id").toString() + "\"").intValue() == 1;
    }
}
