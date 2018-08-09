package a75f.io.bo.building;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.SystemTrimResponseBuilder;
import a75.io.algos.TrimResponseProcessor;
import a75.io.algos.TrimResponseRequest;

/**
 * Created by samjithsadasivan on 6/4/18.
 */
public class VAVSystemProfile extends SystemProfile
{
    
    //TODO - temp for Testing
    //public int minuteCounter = 0;
    //public CSVLogger csvLogger;
    
    public VAVSystemProfile()
    {
        buildSATTRSystem();
        
        buildCO2TRSystem();
        
        //TODO - temp
        //csvLogger = new CSVLogger("VavSystem.csv");
        //String[] header = {"Minutes", "Z1 RH", "Z2 RH", "SAT" ,};
        //csvLogger.writeHeader(header);
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
                                                       .setT(2).setI(2).setSPtrim(0.2).setSPres(-0.3).setSPresmax(-1.0).buildTRSystem();
        satTRProcessor = new TrimResponseProcessor(satTRResponse);
    }
    
    /**
     * Variable 	 Value
     * SP0 	800
     * SPmin 	800
     * SPmax 	1000
     * Td 	 10 minutes
     * T 	2 minutes
     * I	2
     * R 	Zone CO2 Requests
     * SPtrim 	+2 ppm
     * SPres 	 -5 ppm
     * SPres-max -10 ppm
     *
     * */
    private void buildCO2TRSystem() {
       co2TRResponse = new SystemTrimResponseBuilder().setSP0(800).setSPmin(800).setSPmax(1000).setTd(2)//TODO-TEST
                                                      .setT(2).setI(2).setSPtrim(2).setSPres(-5).setSPresmax(-10).buildTRSystem();
       co2TRProcessor = new TrimResponseProcessor(co2TRResponse);
    }
    
    @Override
    public void doSystemControl()
    {
        satTRProcessor.processTrimResponse();
        co2TRProcessor.processTrimResponse();
        Log.d("VAV ", "SAT : " + satTRProcessor.getSetPoint()+", CO2 : "+co2TRProcessor.getSetPoint());
    }
    
    public void updateSATRequest(TrimResponseRequest req)
    {
        satTRResponse.updateRequest(req);
        satTRProcessor.getTrSetting().dump();
    }
    
    public void updateCO2Request(TrimResponseRequest req) {
        co2TRResponse.updateRequest(req);
        co2TRProcessor.getTrSetting().dump();
    }
    
    @JsonIgnore
    public int getCurrentSAT()
    {
        return (int) satTRProcessor.getSetPoint();
    }
    
    @JsonIgnore
    public int getCurrentCO2()
    {
        return (int) co2TRProcessor.getSetPoint();
    }
    
    @Override
    @JsonIgnore
    public TrimResponseProcessor getSystemSATTRProcessor() {
        return satTRProcessor;
    }
    
    @Override
    @JsonIgnore
    public TrimResponseProcessor getSystemCO2TRProcessor() {
        return co2TRProcessor;
    }
}
    

