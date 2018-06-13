package a75f.io.bo.building;

import android.util.Log;

import a75.io.algos.SystemTrimResponse;
import a75.io.algos.SystemTrimResponseBuilder;
import a75.io.algos.TrimResponseProcessor;
import a75.io.algos.TrimResponseRequest;

/**
 * Created by samjithsadasivan on 6/4/18.
 */

public class VAVSystemProfile extends SystemProfile
{
    
    TrimResponseProcessor satTRProcessor;
    SystemTrimResponse    satTRResponse;
    
    //TODO - temp for Testing
    public int minuteCounter = 0;
    public CSVLogger csvLogger;
   
    public VAVSystemProfile()
    {
        buildSATTRSystem();
        
        //TODO - temp
        csvLogger = new CSVLogger("VavSystem.csv");
        String[] header = {"Minutes", "Z1 RH", "Z2 RH", "SAT" ,};
        csvLogger.writeHeader(header);
    }
    
    /**
     * SP0 	SPmax
     * SPmin 	Design SAT (55ºF typ)
     * SPmax 	65ºF (or less for dehumidification)
     * Td 	10 minutes
     * T 	2 minutes
     * I 	2
     * R 	Zone Cooling SAT Requests
     * SPtrim 	+0.2ºF
     * SPres 	-0.3ºF
     * SPres-max 	-1.0ºF
     * <p>
     * During Setup or Cool-Down Modes: Setpoint shall be T-min.
     * During Warm-Up and Setback Modes: Setpoint shall be 95°F.
     */

    private void buildSATTRSystem()
    {
        satTRResponse = new SystemTrimResponseBuilder().setSP0(60).setSPmin(55).setSPmax(65).setTd(2)//TODO- TEST
                                                       .setT(2).setI(2).setSPtrim(0.2).setSPres(-0.3).setSPresmax(-1.0).buidlTRSystem();
        satTRProcessor = new TrimResponseProcessor(satTRResponse);
    }
    
    @Override
    public void doSystemControl()
    {
        satTRProcessor.processTrimResponse();
        Log.d("VAV ", "SAT : " + satTRProcessor.getSetPoint());
        
        
    }
    
    public void updateSATRequest(TrimResponseRequest req)
    {
        satTRResponse.updateRequest(req);
        satTRProcessor.getTrSetting().dump();
        Log.d("VAV"," Updated request params requestHours : "+req.requestHours+" cum Request hours "+req.cumulativeRequestHoursPercent);
    }
    
    public int getCurrentSAT()
    {
        return (int) satTRProcessor.getSetPoint();
    }
    
    @Override
    public TrimResponseProcessor getSystemTRProcessor() {
        return satTRProcessor;
    }
}
    

