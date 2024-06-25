package a75.io.algos.vav;


import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.tr.SystemTrimResponseBuilder;
import a75.io.algos.tr.TRSystem;
import a75.io.algos.tr.TrimResponseProcessor;
import a75.io.algos.tr.TrimResponseRequest;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

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
        satTRResponse = new SystemTrimResponseBuilder().setSP0(getSatTRTunerVal("spinit", "sat")).setSPmin(getSatTRTunerVal("spmin", "sat"))
                                                       .setSPmax(getSatTRTunerVal("spmax", "sat")).setTd((int)getSatTRTunerVal("timeDelay", "sat"))//TODO- TEST
                                                       .setT((int)getSatTRTunerVal("timeInterval", "sat")).setI((int)getSatTRTunerVal("ignoreRequest", "sat"))
                                                       .setSPtrim(getSatTRTunerVal("sptrim", "sat")).setSPres(getSatTRTunerVal("spres", "sat"))
                                                       .setSPresmax(getSatTRTunerVal("spresmax", "sat")).buildTRSystem();
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
     * */
    private void buildCO2TRSystem() {
        co2TRResponse = new SystemTrimResponseBuilder().setSP0(getCO2TRTunerVal("spinit", "co2")).setSPmin(getCO2TRTunerVal("spmin", "co2"))
                                                       .setSPmax(getCO2TRTunerVal("spmax", "co2")).setTd((int)getCO2TRTunerVal("timeDelay", "co2"))//TODO-TEST
                                                       .setT((int)getCO2TRTunerVal("timeInterval", "co2")).setI((int)getCO2TRTunerVal("ignoreRequest", "co2"))
                                                       .setSPtrim(getCO2TRTunerVal("sptrim", "co2")).setSPres(getCO2TRTunerVal("spres", "co2"))
                                                       .setSPresmax(getCO2TRTunerVal("spresmax", "co2")).buildTRSystem();
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
        spTRResponse = new SystemTrimResponseBuilder().setSP0(getSpTRTunerVal("spinit", "staticPressure")).setSPmin(getSpTRTunerVal("spmin", "staticPressure"))
                                                      .setSPmax(getSpTRTunerVal("spmax", "staticPressure")).setTd((int)getSpTRTunerVal("timeDelay", "staticPressure"))//TODO-TEST
                                                      .setT((int)getSpTRTunerVal("timeInterval", "staticPressure")).setI((int)getSpTRTunerVal("ignoreRequest", "staticPressure"))
                                                      .setSPtrim(getSpTRTunerVal("sptrim", "staticPressure")).setSPres(getSpTRTunerVal("spres", "staticPressure"))
                                                      .setSPresmax(getSpTRTunerVal("spresmax", "staticPressure")).buildTRSystem();
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
        CcuLog.d("CCU_SYSTEM", "processResetResponse SAT : " + satTRProcessor.getSetPoint() + ", CO2 : " +
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
        return spTRProcessor.getSetPoint();
    }

    public double getSatTRTunerVal(String trParam, String tunerType) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        if(hayStack.readEntity("system and equip and not modbus").containsKey("domainName")){
            double tunerValue = getSystemTunerVal(trParam, hayStack, tunerType);
            if(tunerValue != -1) return tunerValue;
        }

        HashMap cdb = hayStack.read("point and system and tuner and tr and sat and " + trParam);
        if((cdb == null) || (cdb.isEmpty())) {
            cdb = hayStack.read("point and tuner and default and tr and sat and " + trParam);
        }
        if(cdb != null && !cdb.isEmpty()) {

            ArrayList values = hayStack.readPoint(cdb.get("id").toString());
            if (values != null && !values.isEmpty()) {
                for (int l = 1; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
                }
            }
        }
        switch (trParam){
            case "spinit":
            case "spmax":
                return 65.0;
            case "spmin":
                return 55.0;
            case "spres":
                return -0.3;
            case "ignoreRequest":
                return 2;
            case "sptrim":
                return 0.2;
            case "timeInterval":
            case "timeDelay":
                return 2.0;
            case "spresmax":
                return -1.0;
            
        }
        return 0;
        //throw new IllegalStateException("Tuner not initialized");
    }
    public double getSpTRTunerVal(String trParam, String tunerType) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        if(hayStack.readEntity("system and equip and not modbus").containsKey("domainName")){
            double tunerValue = getSystemTunerVal(trParam, hayStack, tunerType);
            if(tunerValue != -1) return tunerValue;
        }

        HashMap cdb = hayStack.read("point and system and tuner and tr and staticPressure and "+trParam);
        if(cdb == null)
            cdb = hayStack.read("point and default and tuner and tr and staticPressure and "+trParam);
        if(cdb != null && !cdb.isEmpty()) {
            ArrayList values = hayStack.readPoint(cdb.get("id").toString());
            if (values != null && !values.isEmpty()) {
                for (int l = 1; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
                }
            }
        }
        switch (trParam){
            case "spinit":
            case "spmin":
                return 0.2;
            case "spmax":
                return 1.0;
            case "spres":
                return 0.05;
            case "sptrim":
                return -0.02;
            case "ignoreRequest":
            case "timeInterval":
            case "timeDelay":
                return 2.0;
            case "spresmax":
                return 0.1;
        }
        return 0;
        //throw new IllegalStateException("Tuner not initialized");
    }
    
    public double getCO2TRTunerVal(String trParam, String tunerType) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        if(hayStack.readEntity("system and equip and not modbus").containsKey("domainName")){
            double tunerValue = getSystemTunerVal(trParam, hayStack, tunerType);
            if(tunerValue != -1) return tunerValue;
        }
        HashMap cdb = hayStack.read("point and system and tuner and tr and co2 and "+trParam);
        if(cdb == null)
            cdb = hayStack.read("point and default and tuner and tr and co2 and "+trParam);
        if(cdb != null && !cdb.isEmpty()) {

            ArrayList values = hayStack.readPoint(cdb.get("id").toString());
            if (values != null && !values.isEmpty()) {
                for (int l = 1; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
                }
            }
        }
        switch (trParam){
            case "spinit":
            case "spmin":
                return 800.0;
            case "spmax":
                return 1000.0;
            case "spres":
                return -10.0;
            case "sptrim":
                return 20.0;
            case "ignoreRequest":
            case "timeInterval":
            case "timeDelay":
                return 2.0;
            case "spresmax":
                return -30.0;
        }
        return 0;
        //throw new IllegalStateException("Tuner not initialized");
    }

    private double getSystemTunerVal(String trParam, CCUHsApi hayStack, String tunerType){
        String domainName = getDomainName(trParam, tunerType);
        HashMap cdb =  hayStack.readEntity("system and point and domainName == \"" + domainName + "\"");
        if(cdb == null)
            cdb = hayStack.read("point and default and tuner and tr and co2 and "+trParam);
        if(cdb != null && !cdb.isEmpty()) {
            ArrayList values = hayStack.readPoint(cdb.get("id").toString());
            if (values != null && !values.isEmpty()) {
                for (int l = 1; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
                }
            }
        }

        return -1;
    }

    private String getDomainName(String trParam, String tunerType){ {
            switch (tunerType) {
                case "sat":
                    switch (trParam) {
                        case "spinit":
                            return "satSpInit";
                        case "spmin":
                            return "satSpMin";
                        case "spmax":
                            return "satSpMax";
                        case "spres":
                            return "satSpRes";
                        case "sptrim":
                            return "satSpTrim";
                        case "ignoreRequest":
                            return "satIgnoreRequest";
                        case "timeInterval":
                            return "satTimeInterval";
                        case "timeDelay":
                            return "satTimeDelay";
                        case "spresmax":
                            return "satSpResMax";
                    }
                    break;
                case "co2":
                    switch (trParam) {
                        case "spinit":
                            return "co2SpInit";
                        case "spmin":
                            return "co2SpMin";
                        case "spmax":
                            return "co2SpMax";
                        case "spres":
                            return "co2SpRes";
                        case "sptrim":
                            return "co2SpTrim";
                        case "ignoreRequest":
                            return "co2IgnoreRequest";
                        case "timeInterval":
                            return "co2TimeInterval";
                        case "timeDelay":
                            return "co2TimeDelay";
                        case "spresmax":
                            return "co2SpResMax";
                    }
                    break;
                case "staticPressure":
                    switch (trParam) {
                        case "spinit":
                            return "staticPressureSpInit";
                        case "spmin":
                            return "staticPressureSpMin";
                        case "spmax":
                            return "staticPressureSpMax";
                        case "spres":
                            return "staticPressureSpRes";
                        case "sptrim":
                            return "staticPressureSpTrim";
                        case "ignoreRequest":
                            return "staticPressureIgnoreRequest";
                        case "timeInterval":
                            return "staticPressureTimeInterval";
                        case "timeDelay":
                            return "staticPressureTimeDelay";
                        case "spresmax":
                            return "staticPressureSpResMax";
                    }
                    break;
            }
            return trParam;
        }
    }
}
