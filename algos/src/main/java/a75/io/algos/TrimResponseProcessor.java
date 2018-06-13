package a75.io.algos;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 6/4/18.
 */

public class TrimResponseProcessor
{
    double setPoint;
    
    SystemTrimResponse trSetting;
    
    public ArrayList<TrimResetListener> trListeners;
    
    int                          minuteCounter;//assuming TR processed every minute
    
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
    
    public void processTrimResponse() {
        if (++minuteCounter < trSetting.getTd()) {
            trSetting.resetRequest();
            return;
        }
        if (minuteCounter % trSetting.getT() != 0) {
            return;
        }
    
        double sp = setPoint + trSetting.getSPtrim();
        Log.d("VAV", "trimmed sp "+sp);
        
        int netRequests = trSetting.getR() - trSetting.getI();
        
        //TR System responds only when the net request count is positive
        if (netRequests > 0) {
            double response = netRequests * trSetting.getSPres();
            if (response < trSetting.getSPresmax() ||
                            response > trSetting.getSPresmax()) {
                response = trSetting.getSPresmax();
            }
            sp += response;
            Log.d("VAV", "sp with response "+sp);
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
    
        Log.d("VAV", "sp normalized "+sp);
        
        setPoint = sp;
        Log.d("VAV", "setpoint "+setPoint);
    }
}
