package a75.io.algos;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 6/4/18.
 */

public class TrimResponseProcessor
{
    public double setPoint;
    
    public SystemTrimResponse trSetting;
    
    public ArrayList<TrimResetListener> trListeners;
    
    public int minuteCounter;//assuming TR processed every minute
    
    public TrimResponseProcessor() {
    
    }
    
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
    
    public void removeTRListener(TrimResetListener l) {
        trListeners.remove(l);
    }
    
    public void processResetResponse() {
        if (++minuteCounter < trSetting.getTd()) {
            trSetting.resetRequest();
            return;
        }
        
        //TODO - requests now get updated every minute, it should change with the time step value here
        if (minuteCounter % trSetting.getT() != 0) {
            return;
        }
    
        double sp = setPoint + trSetting.getSPtrim();
        
        int netRequests = trSetting.getR() - trSetting.getI();
        
        //TR System responds only when the net request count is positive
        if (netRequests > 0) {
            double response = netRequests * trSetting.getSPres();
            if (response < trSetting.getSPresmax() ||
                            response > trSetting.getSPresmax()) {
                response = trSetting.getSPresmax();
            }
            sp += response;
            trSetting.resetRequest(); //Reset request count whenever a response is generated.
            for (TrimResetListener l : trListeners) {
                l.handleSystemReset();
            }
        }
        
        if (sp < trSetting.getSPmin()) {
            sp = trSetting.getSPmin();
        } else if (sp > trSetting.getSPmax()) {
            sp = trSetting.getSPmax();
        }
        
        setPoint = sp;
        Log.d("VAV", "setpoint "+setPoint);
    }
}
