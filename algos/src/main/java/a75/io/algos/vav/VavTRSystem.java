package a75.io.algos.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.tr.SystemTrimResponseBuilder;
import a75.io.algos.tr.TRSystem;
import a75.io.algos.tr.TrimResponseProcessor;
import a75.io.algos.tr.TrimResponseRequest;

/**
 * Created by samjithsadasivan on 8/13/18.
 */

public class VavTRSystem extends TRSystem
{
    public VavTRSystem()
    {
        buildSATTRSystem();
        buildCO2TRSystem();
        buildSpTRSystem();
        buildHwstTRSystem();
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
     * SPtrim 	+20 ppm
     * SPres 	 -10 ppm
     * SPres-max -30 ppm
     *
     * */
    private void buildCO2TRSystem() {
        co2TRResponse = new SystemTrimResponseBuilder().setSP0(800).setSPmin(800).setSPmax(1000).setTd(2)//TODO-TEST
                                                       .setT(2).setI(2).setSPtrim(20).setSPres(-10).setSPresmax(-30).buildTRSystem();
        co2TRProcessor = new TrimResponseProcessor(co2TRResponse);
    }
    
    /**
     * Variable 	 Value
     * SP0 	0.5 inches
     * SPmin 	0.1 inches
     * SPmax 	Per TAB report
     * Td 	 10 minutes
     * T 	2 minutes
     * I	2
     * R 	Zone Static Pressure Reset Requests
     * SPtrim 	-0.02 inches
     * SPres 	 +0.05 inches
     * SPres-max    +0.10 inches
     * */
    private void buildSpTRSystem() {
        spTRResponse = new SystemTrimResponseBuilder().setSP0(0.5).setSPmin(0.1).setSPmax(1.5).setTd(2)//TODO-TEST
                                                      .setT(2).setI(2).setSPtrim(-0.02).setSPres(0.05).setSPresmax(0.10).buildTRSystem();
        spTRProcessor = new TrimResponseProcessor(spTRResponse);
    }
    
    
    /**
     * SP0 	100ºF
     * SPmin 	90ºF
     * SPmax 	120ºF
     * Td 	10 minutes
     * T 	2 minutes
     * I 	2
     * R 	Zone Cooling SAT Requests
     * SPtrim 	-0.4ºF
     * SPres 	0.6ºF
     * SPres-max 	1ºF
     */
    
    private void buildHwstTRSystem() {
        hwstTRResponse = new SystemTrimResponseBuilder().setSP0(100).setSPmin(90).setSPmax(120).setTd(2)//TODO-TEST
                                                      .setT(2).setI(2).setSPtrim(-0.4).setSPres(0.6).setSPresmax(1.0).buildTRSystem();
        hwstTRProcessor = new TrimResponseProcessor(hwstTRResponse);
    }
    
    @Override
    public void processResetResponse()
    {
        satTRProcessor.processResetResponse();
        co2TRProcessor.processResetResponse();
        spTRProcessor.processResetResponse();
        hwstTRProcessor.processResetResponse();
        Log.d("VAV ", "SAT : " + satTRProcessor.getSetPoint() + ", CO2 : " +
                      co2TRProcessor.getSetPoint()+", SP : "+spTRProcessor.getSetPoint()+" HWST : "+spTRProcessor.getSetPoint());
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
    
    public void updateSpRequest(TrimResponseRequest req) {
        spTRResponse.updateRequest(req);
        spTRProcessor.getTrSetting().dump();
    }
    
    public void updateHwstRequest(TrimResponseRequest req) {
        hwstTRResponse.updateRequest(req);
        hwstTRProcessor.getTrSetting().dump();
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
    
    @JsonIgnore
    public double getCurrentSp()
    {
        return (double) spTRProcessor.getSetPoint();
    }
    
    @JsonIgnore
    public double getCurrentHwst()
    {
        return (double) hwstTRProcessor.getSetPoint();
    }
    
}
