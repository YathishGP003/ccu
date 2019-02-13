package a75.io.algos.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.tr.SystemTrimResponseBuilder;
import a75.io.algos.tr.TRSystem;
import a75.io.algos.tr.TrimResponseProcessor;
import a75.io.algos.tr.TrimResponseRequest;
import a75f.io.api.haystack.CCUHsApi;

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
        satTRResponse = new SystemTrimResponseBuilder().setSP0(getSatTRTunerVal("spinit")).setSPmin(getSatTRTunerVal("spmin"))
                                                       .setSPmax(getSatTRTunerVal("spmax")).setTd((int)getSatTRTunerVal("timeDelay"))//TODO- TEST
                                                       .setT((int)getSatTRTunerVal("timeInterval")).setI((int)getSatTRTunerVal("ignoreRequest"))
                                                       .setSPtrim(getSatTRTunerVal("sptrim")).setSPres(getSatTRTunerVal("spres"))
                                                       .setSPresmax(getSatTRTunerVal("spresmax")).buildTRSystem();
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
        co2TRResponse = new SystemTrimResponseBuilder().setSP0(getCO2TRTunerVal("spinit")).setSPmin(getCO2TRTunerVal("spmin"))
                                                       .setSPmax(getCO2TRTunerVal("spmax")).setTd((int)getCO2TRTunerVal("timeDelay"))//TODO-TEST
                                                       .setT((int)getCO2TRTunerVal("timeInterval")).setI((int)getCO2TRTunerVal("ignoreRequest"))
                                                       .setSPtrim(getCO2TRTunerVal("sptrim")).setSPres(-getCO2TRTunerVal("spres"))
                                                       .setSPresmax(-getCO2TRTunerVal("spresmax")).buildTRSystem();
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
        spTRResponse = new SystemTrimResponseBuilder().setSP0(getSpTRTunerVal("spinit")).setSPmin(getSpTRTunerVal("spmin"))
                                                      .setSPmax(getSpTRTunerVal("spmax")).setTd((int)getSpTRTunerVal("timeDelay"))//TODO-TEST
                                                      .setT((int)getSpTRTunerVal("timeInterval")).setI((int)getSpTRTunerVal("ignoreRequest"))
                                                      .setSPtrim(getSpTRTunerVal("sptrim")).setSPres(getSpTRTunerVal("spres"))
                                                      .setSPresmax(getSpTRTunerVal("spresmax")).buildTRSystem();
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
        Log.d("VAV ", "processResetResponse SAT : " + satTRProcessor.getSetPoint() + ", CO2 : " +
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
    
    private double getSatTRTunerVal(String trParam) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and tuner and tr and sat and " + trParam);
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        throw new IllegalStateException("Tuner not initialized");
    }
    
    private double getSpTRTunerVal(String trParam) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and tuner and tr and staticPressure and "+trParam);
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        throw new IllegalStateException("Tuner not initialized");
    }
    
    private double getCO2TRTunerVal(String trParam) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and tuner and tr and co2 and "+trParam);
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        throw new IllegalStateException("Tuner not initialized");
    }
}
