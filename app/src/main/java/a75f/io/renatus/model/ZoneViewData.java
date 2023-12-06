package a75f.io.renatus.model;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.domain.VavEquip;
import a75f.io.domain.api.Domain;
import a75f.io.domain.logic.DomainManager;
import a75f.io.logic.bo.building.Thermistor;
import a75f.io.logic.bo.building.sensors.NativeSensor;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.jobs.StringConstants;

/**
*
* This has to be replaced with ViewModel and DomainModel during ZoneFragment refactor.
* For the time being this class is created to move all these methods out of logic component's ScheduleProcessJob which
* has nothing to do with zone data or UI.
* */
public class ZoneViewData {
    
    private static final String AIRFLOW_SENSOR = "airflow sensor";
    public static final String ACTION_STATUS_CHANGE = "status_change";
    private static final String THERMISTER_QUERY_POINT = "point and config and standalone and enable and th1 and equipRef == \"";

    public static HashMap getDABEquipPoints(String equipID) {
        HashMap dabPoints = new HashMap();
        dabPoints.put("Profile","DAB");
        boolean isThermister1On = (CCUHsApi.getInstance().readDefaultVal(THERMISTER_QUERY_POINT + equipID + "\"") > 0) ;
        
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        //double damperPosPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and damper and base and equipRef == \""+equipID+"\"");
        double damperPosPoint = CCUHsApi.getInstance().readHisValByQuery("point and damper and normalized and cmd and equipRef == \""+equipID+"\"");
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and supply and air and temp and primary and equipRef == \""+equipID+"\"");
        double airflowCFM =  CCUHsApi.getInstance().readHisValByQuery("point and air and flow and trueCfm and dab and equipRef == \""+equipID+"\"");
        double reheatPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and reheat and cmd and equipRef == \""+equipID+"\"");
        dabPoints.put(AIRFLOW_SENSOR,isThermister1On);
        if (equipStatusPoint.length() > 0)
        {
            dabPoints.put("Status",equipStatusPoint);
        }else{
            dabPoints.put("Status","OFF");
        }
        if (damperPosPoint > 0)
        {
            dabPoints.put("Damper",(int)damperPosPoint+"% Open");
        }else{
            dabPoints.put("Damper",0+"% Open");
        }
        if (dischargePoint  != 0)
        {
            dabPoints.put("Supply Airflow",dischargePoint+" \u2109");
        }else{
            dabPoints.put("Supply Airflow",0+" \u2109");
        }
        if (airflowCFM != 0.0)
        {
            dabPoints.put("Airflow CFM",String.format("%.0f", airflowCFM));
        }else{
            dabPoints.put("Airflow CFM",0);
        }
        if (reheatPoint  > 0)
        {
            dabPoints.put("Reheat Coil",reheatPoint+"% Open");
        }else{
            dabPoints.put("Reheat Coil",0);
        }
        return dabPoints;
    }
    
    public static HashMap getTIEquipPoints(String equipID) {
        
        HashMap tiPoints = new HashMap();
        tiPoints.put("Profile","TEMP_INFLUENCE");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            tiPoints.put("Status",equipStatusPoint);
        }else{
            tiPoints.put("Status","OFF");
        }
        return tiPoints;
    }

    public static HashMap getSSEEquipPoints(String equipID) {
        
        HashMap ssePoints = new HashMap();
        ssePoints.put("Profile","SSE");
        boolean isThermister1On = (CCUHsApi.getInstance().readDefaultVal(THERMISTER_QUERY_POINT + equipID + "\"") > 0);
        
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and equipRef == \""+equipID+"\"");
        ssePoints.put(AIRFLOW_SENSOR,isThermister1On);
        if (equipStatusPoint.length() > 0)
        {
            ssePoints.put("Status",equipStatusPoint);
        }else{
            ssePoints.put("Status","OFF");
        }
        if (dischargePoint  != 0)
        {
            
            ssePoints.put("Discharge Airflow",dischargePoint+" \u2109");
        }else{
            ssePoints.put("Discharge Airflow",0+" \u2109");
        }
        return ssePoints;
    }
    
    public static HashMap getVAVEquipPoints(String equipID) {
        VavEquip vavEquip = (VavEquip) Domain.INSTANCE.getDomainEquip(equipID);
        HashMap vavPoints = new HashMap();
        boolean isThermister1On = (CCUHsApi.getInstance().readDefaultVal(THERMISTER_QUERY_POINT + equipID + "\"") > 0);

        String equipStatusPoint = vavEquip.getEquipStatusMessage().readDefaultStrVal();
        double damperPosPoint = vavEquip.getNormalizedDamperCmd().readHisVal();
        double reheatPoint = vavEquip.getReheatCmd().readHisVal();
        double enteringAirPoint = vavEquip.getEnteringAirTemp().readHisVal();
        double dischargePoint = vavEquip.getDischargeAirTemp().readHisVal();
        double airflowCFM =  vavEquip.getAirFlowSensor().readHisVal();
        vavPoints.put(AIRFLOW_SENSOR,isThermister1On);
        if (equipStatusPoint.length() > 0)
        {
            vavPoints.put("Status",equipStatusPoint);
        }else{
            vavPoints.put("Status","OFF");
        }
        if (damperPosPoint > 0)
        {
            vavPoints.put("Damper",(int)damperPosPoint+"% Open");
        }else{
            vavPoints.put("Damper",0+"% Open");
        }
        if (reheatPoint  > 0)
        {
            vavPoints.put("Reheat Coil",reheatPoint+"% Open");
        }else{
            vavPoints.put("Reheat Coil",0);
        }
        if (enteringAirPoint != 0)
        {
            vavPoints.put("Entering Airflow",enteringAirPoint+" \u2109");
        }else{
            vavPoints.put("Entering Airflow",0+" \u2109");
        }
        if (dischargePoint != 0)
        {
            vavPoints.put("Discharge Airflow",dischargePoint+" \u2109");
        }else{
            vavPoints.put("Discharge Airflow",0+" \u2109");
        }
        if (airflowCFM != 0.0)
        {
            vavPoints.put("Airflow CFM",String.format("%.0f", airflowCFM));
        }else{
            vavPoints.put("Airflow CFM",0);
        }
        
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readMapById(equipID);
        if (equip.containsKey("series")) {
            vavPoints.put("Profile","VAV Series Fan");
        } else if (equip.containsKey("parallel")){
            vavPoints.put("Profile","VAV Parallel Fan");
        } else {
            vavPoints.put("Profile", "VAV Reheat - No Fan");
        }

        return vavPoints;
    }

    public static HashMap get2PFCUEquipPoints(String equipID) {
        HashMap p2FCUPoints = new HashMap();
        
        
        p2FCUPoints.put("Profile","Smartstat - 2 Pipe FCU");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        double fanopModePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double condtionModePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and temp and mode and conditioning and equipRef == \""+equipID+"\"");
        boolean isThermister1On = (CCUHsApi.getInstance().readDefaultVal(THERMISTER_QUERY_POINT + equipID + "\"") > 0);
        
        boolean isCoolingOn = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay6 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanLowEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay3 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanMediumEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay1 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanHighEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay2 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            p2FCUPoints.put("Status",equipStatusPoint);
            //vavPoints.add(status);
        }else{
            p2FCUPoints.put("Status","OFF");
        }
        p2FCUPoints.put("Fan Mode",fanopModePoint);
        p2FCUPoints.put("Conditioning Mode",condtionModePoint);
        p2FCUPoints.put(AIRFLOW_SENSOR,isThermister1On);
        if (dischargePoint != 0) {
            p2FCUPoints.put("Discharge Airflow", dischargePoint + " \u2109");
        } else {
            p2FCUPoints.put("Discharge Airflow", 0 + " \u2109");
        }
        
        //We not dont consider auxiliary heating selection for determining available conditioning modes.
        if(!isCoolingOn)
            p2FCUPoints.put("condEnabled","Off");
        
        if(isFanLowEnabled && isFanMediumEnabled && !isFanHighEnabled)
            p2FCUPoints.put("fanEnabled","No High Fan");
        else if(isFanLowEnabled && !isFanMediumEnabled)
            p2FCUPoints.put("fanEnabled","No Medium High Fan");
        else if(!isFanLowEnabled)
            p2FCUPoints.put("fanEnabled","No Fan");
        return p2FCUPoints;
    }

    public static HashMap get4PFCUEquipPoints(String equipID) {
        HashMap p4FCUPoints = new HashMap();
        
        p4FCUPoints.put("Profile","Smartstat - 4 Pipe FCU");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        double fanopModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double condtionModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and temp and mode and conditioning and equipRef == \""+equipID+"\"");
        boolean isThermister1On = (CCUHsApi.getInstance().readDefaultVal(THERMISTER_QUERY_POINT + equipID + "\"") > 0);
        
        boolean isCoolingOn = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay6 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isHeatingOn = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay4 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        
        boolean isFanLowEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay3 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanMediumEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay1 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanHighEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay2 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            p4FCUPoints.put("Status",equipStatusPoint);
        }else{
            p4FCUPoints.put("Status","OFF");
        }
        p4FCUPoints.put("Fan Mode",fanopModePoint);
        p4FCUPoints.put("Conditioning Mode",condtionModePoint);
        p4FCUPoints.put(AIRFLOW_SENSOR,isThermister1On);
        if (dischargePoint != 0) {
            p4FCUPoints.put("Discharge Airflow", dischargePoint + " \u2109");
        } else {
            p4FCUPoints.put("Discharge Airflow", 0 + " \u2109");
        }
        if(isCoolingOn && !isHeatingOn)
            p4FCUPoints.put("condEnabled",StringConstants.COOL_ONLY);
        else if(!isCoolingOn && isHeatingOn)
            p4FCUPoints.put("condEnabled",StringConstants.HEAT_ONLY);
        else if(!isCoolingOn && !isHeatingOn)
            p4FCUPoints.put("condEnabled","Off");
        
        if(isFanLowEnabled && isFanMediumEnabled && !isFanHighEnabled)
            p4FCUPoints.put("fanEnabled","No High Fan");
        else if(isFanLowEnabled && !isFanMediumEnabled)
            p4FCUPoints.put("fanEnabled","No Medium High Fan");
        else if(!isFanLowEnabled)
            p4FCUPoints.put("fanEnabled","No Fan");
        return p4FCUPoints;
    }
    
    public static HashMap getCPUEquipPoints(String equipID) {
        HashMap cpuPoints = new HashMap();
        
        cpuPoints.put("Profile","Smartstat - Conventional Package Unit");
        ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");
        
        boolean isCooling1On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay1 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isCooling2On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay2 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        
        boolean isHeating1On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay4 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isHeating2On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay5 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        
        boolean isThermister1On = (CCUHsApi.getInstance().readDefaultVal(THERMISTER_QUERY_POINT + equipID + "\"") > 0);
        boolean isFanLowEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay3 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanHighEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay6 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and equipRef == \""+equipID+"\"");
        double fanopModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double conditionModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and temp and mode and conditioning and equipRef == \""+equipID+"\"");
        double fanHighHumdOption = CCUHsApi.getInstance().readDefaultVal("point and zone and config and relay6 and type and equipRef == \"" + equipID + "\"");
        double targetHumidity = 0;
        if (equipStatusPoint != null && equipStatusPoint.size() > 0)
        {
            String id = ((HashMap) equipStatusPoint.get(0)).get("id").toString();
            String status = CCUHsApi.getInstance().readDefaultStrValById(id);
            cpuPoints.put("Status",status);
        }else{
            cpuPoints.put("Status","OFF");
        }
        cpuPoints.put("Fan Mode",fanopModePoint);
        cpuPoints.put("Conditioning Mode",conditionModePoint);
        cpuPoints.put(AIRFLOW_SENSOR,isThermister1On);
        if (dischargePoint != 0) {
            cpuPoints.put("Discharge Airflow", dischargePoint + " \u2109");
        } else {
            cpuPoints.put("Discharge Airflow", 0 + " \u2109");
        }
        if(fanHighHumdOption > 0){
            if(fanHighHumdOption > 1) isFanHighEnabled = false;
            cpuPoints.put("Fan High Humidity",fanHighHumdOption);
            if(fanHighHumdOption == 2.0) {
                targetHumidity = CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and target and humidity and his and equipRef == \"" + equipID + "\"");
                cpuPoints.put("Target Humidity",targetHumidity);
            }else {
                targetHumidity = CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and target and dehumidifier and his and equipRef == \"" + equipID + "\"");
                cpuPoints.put("Target Dehumidity",targetHumidity);
            }
        }else{
            cpuPoints.put("Fan High Humidity",0);
        }
        if((isCooling1On || isCooling2On) && (!isHeating1On && !isHeating2On))
            cpuPoints.put("condEnabled","Cool Only");
        else if((!isCooling1On && !isCooling2On) && (isHeating1On || isHeating2On))
            cpuPoints.put("condEnabled","Heat Only");
        else if((!isCooling1On && !isCooling2On) && (!isHeating1On && !isHeating2On))
            cpuPoints.put("condEnabled","Off");
        if(isFanLowEnabled && !isFanHighEnabled)
            cpuPoints.put("fanEnabled","No High Fan");
        else if(!isFanLowEnabled && !isFanHighEnabled)
            cpuPoints.put("fanEnabled","No Fan");
        return cpuPoints;
    }

    public static HashMap getHPUEquipPoints(String equipID) {
        HashMap hpuPoints = new HashMap();
        
        
        
        hpuPoints.put("Profile","Smartstat - Heat Pump Unit");
        ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");
        
        boolean isCompressor1On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay1 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isCompressor2On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay2 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isThermister1On = (CCUHsApi.getInstance().readDefaultVal(THERMISTER_QUERY_POINT + equipID + "\"") > 0);
        
        boolean isFanLowEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay3 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanHighEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay5 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        double fanopModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double conditionModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and temp and mode and conditioning and equipRef == \""+equipID+"\"");
        double fanHighHumdOption = CCUHsApi.getInstance().readDefaultVal("point and zone and config and relay5 and type and equipRef == \"" + equipID + "\"");
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and equipRef == \""+equipID+"\"");
        double targetHumidity = 0;
        if (equipStatusPoint != null && equipStatusPoint.size() > 0)
        {
            String id = ((HashMap) equipStatusPoint.get(0)).get("id").toString();
            String status = CCUHsApi.getInstance().readDefaultStrValById(id);
            hpuPoints.put("Status",status);
            hpuPoints.put("StatusTag",id);
            //vavPoints.add(status);
        }else{
            hpuPoints.put("Status","OFF");
        }
        hpuPoints.put("Fan Mode",fanopModePoint);
        hpuPoints.put("Conditioning Mode",conditionModePoint);
        hpuPoints.put(AIRFLOW_SENSOR,isThermister1On);
        if (dischargePoint != 0) {
            hpuPoints.put("Discharge Airflow", dischargePoint + " \u2109");
        } else {
            hpuPoints.put("Discharge Airflow", 0 + " \u2109");
        }
        if(fanHighHumdOption > 0){
            if(fanHighHumdOption > 1)isFanHighEnabled = false; //Since relay 5 is mapped to humidity or dehumidity
            hpuPoints.put("Fan High Humidity",fanHighHumdOption);
            if(fanHighHumdOption == 2.0) {
                targetHumidity = CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and target and humidity and his and equipRef == \"" + equipID + "\"");
                hpuPoints.put("Target Humidity",targetHumidity);
            }else {
                targetHumidity = CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and target and dehumidifier and his and equipRef == \"" + equipID + "\"");
                hpuPoints.put("Target Dehumidity",targetHumidity);
            }
        }else{
            hpuPoints.put("Fan High Humidity",0);
        }
        
        if(!isCompressor1On && !isCompressor2On) {
            hpuPoints.put("condEnabled","Off");
        }
        
        if(isFanLowEnabled && !isFanHighEnabled)
            hpuPoints.put("fanEnabled","No High Fan");
        else if(!isFanLowEnabled && !isFanHighEnabled)
            hpuPoints.put("fanEnabled","No Fan");
        return hpuPoints;
    }

    public static HashMap getEMEquipPoints(String equipID) {
        HashMap emPoints = new HashMap();
        
        emPoints.put("Profile","Energy Meter");
        ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");
        ArrayList currentRate = CCUHsApi.getInstance().readAll("point and emr and rate and equipRef == \""+equipID+"\"");
        double energyReading = CCUHsApi.getInstance().readHisValByQuery("point and emr and sensor and sp and equipRef == \""+equipID+"\"");
        
        if (equipStatusPoint != null && equipStatusPoint.size() > 0)
        {
            String id = ((HashMap) equipStatusPoint.get(0)).get("id").toString();
            String status = CCUHsApi.getInstance().readDefaultStrValById(id);
            emPoints.put("Status",status);
        }else{
            emPoints.put("Status","OFF");
        }
        double currentRateVal = 0;
        if (currentRate != null && currentRate.size() > 0)
        {
            String id = ((HashMap) currentRate.get(0)).get("id").toString();
            HisItem currentRateHis = CCUHsApi.getInstance().curRead(id);
            if(currentRateHis != null){
                currentRateVal = currentRateHis.getVal();
            }
        }
        emPoints.put("Current Rate",currentRateVal);
        if (energyReading > 0)
        {
            emPoints.put("Energy Reading",energyReading);
        }else{
            emPoints.put("Energy Reading",0.0);
        }
        
        return emPoints;
    }

    public static HashMap getPiEquipPoints(String equipID) {
        HashMap plcPoints = new HashMap();
        
        plcPoints.put("Profile","Pi Loop Controller");
        ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");
        ArrayList inputValue = CCUHsApi.getInstance().readAll("point and process and logical and variable and equipRef == \""+equipID+"\"");
        ArrayList piSensorValue = CCUHsApi.getInstance().readAll("point and analog1 and config and input and sensor and equipRef == \""+equipID+"\"");
        double dynamicSetpoint = CCUHsApi.getInstance().readDefaultVal("point and analog2 and config and enabled and equipRef == \""+equipID+"\"");
        int th1InputSensor =  CCUHsApi.getInstance().readDefaultVal("point and config and th1 and input and sensor and equipRef == \"" + equipID + "\"").intValue();
        double targetValue = dynamicSetpoint > 0 ? 0: CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and pid and target and config and equipRef == \""+equipID+"\"");
        double analog1sensorType = CCUHsApi.getInstance().readPointPriorityValByQuery("point and analog1 and config and input and sensor and equipRef == \""+equipID+"\"");
        double analog2sensorType = CCUHsApi.getInstance().readPointPriorityValByQuery("point and analog2 and config and input and sensor and equipRef == \""+equipID+"\"");
        double offsetValue = CCUHsApi.getInstance().readDefaultVal("point and config and setpoint and sensor and offset and equipRef == \""+equipID+"\"");
        double loopOutput =
            CCUHsApi.getInstance().readHisValByQuery("point and control and variable and equipRef == \""+equipID+"\"");
        
        if (equipStatusPoint != null && equipStatusPoint.size() > 0)
        {
            String id = ((HashMap) equipStatusPoint.get(0)).get("id").toString();
            String status = CCUHsApi.getInstance().readDefaultStrValById(id);
            plcPoints.put("Status",status);
        }else{
            plcPoints.put("Status","OFF");
        }
        if (inputValue != null && inputValue.size() > 0)
        {
            String id = ((HashMap) inputValue.get(0)).get("id").toString();
            double inputVal = CCUHsApi.getInstance().readHisValById(id);
            plcPoints.put("Input Value",inputVal);
        }
        
        plcPoints.put("LoopOutput",loopOutput);
        
        plcPoints.put("Offset Value",offsetValue);
        
        if (piSensorValue != null && piSensorValue.size() > 0)
        {
            String id = ((HashMap) piSensorValue.get(0)).get("id").toString();
            double piSensorVal = CCUHsApi.getInstance().readHisValById(id);
            plcPoints.put("Pi Sensor Value",piSensorVal);
        }
        if(dynamicSetpoint == 1) {
            plcPoints.put("Dynamic Setpoint",true);
            targetValue = CCUHsApi.getInstance().readHisValByQuery("point and dynamic and target and value and equipRef == \""+equipID+"\"");
        }else {
            if(dynamicSetpoint == 0)
                plcPoints.put("Dynamic Setpoint",false);
        }
        
        plcPoints.put("Target Value",targetValue);
        
        HashMap inputDetails = CCUHsApi.getInstance().read(
            "point and process and logical and variable and equipRef == \""+equipID+"\"");
        HashMap targetDetails =
            CCUHsApi.getInstance().read("point and target and pid and equipRef == \""+equipID+"\"");
        
        plcPoints.put("Unit Type", inputDetails.get("shortDis"));
        plcPoints.put("Unit",  inputDetails.get("unit"));
        plcPoints.put("Dynamic Unit Type", targetDetails.get("shortDis"));
        plcPoints.put("Dynamic Unit",  targetDetails.get("unit"));
        
        if (th1InputSensor == 1 || th1InputSensor == 2) {
            plcPoints.put("Unit Type", "Temperature");
            plcPoints.put("Unit", "\u00B0F");
        }
        
        int nativeInputSensor =  CCUHsApi.getInstance().readDefaultVal("point and config and native and input and " +
                                                                       "sensor and equipRef == \"" + equipID + "\"").intValue();
        if (nativeInputSensor > 0) {
            NativeSensor selectedSensor = SensorManager.getInstance().getNativeSensorList().get(nativeInputSensor - 1);
            plcPoints.put("Unit Type", selectedSensor.sensorName);
            plcPoints.put("Unit", selectedSensor.engineeringUnit);
        }
        
        
        return plcPoints;
    }
    
    public static HashMap getHyperStatMonitoringEquipPoints(String equipID) {
        HashMap monitoringPoints = new HashMap();
        CCUHsApi haystack = CCUHsApi.getInstance();
        monitoringPoints.put("Profile","MONITORING");
        double currentTemp = haystack.readHisValByQuery("zone and point and current and temp and group == \""+equipID+"\"");
        double tempOffset = haystack.readDefaultVal("point and offset and temperature and group == \""+equipID+"\"");
        double analog1Sensor = haystack.readDefaultVal("point and config and analog1 and input and sensor and group == \"" + equipID + "\"").intValue();
        double analog2Sensor = haystack.readDefaultVal("point and config and analog2 and input and sensor and group == \"" + equipID + "\"").intValue();
        double th1Sensor = haystack.readDefaultVal("point and config and th1 and input and sensor and group == \"" + equipID + "\"").intValue();
        double th2Sensor = haystack.readDefaultVal("point and config and th2 and input and sensor and group == \"" + equipID + "\"").intValue();
        boolean isAnalog1Enable = haystack.readDefaultVal("point and config and analog1 and enabled and group == \"" + equipID + "\"") > 0;
        boolean isAnalog2Enable = haystack.readDefaultVal("point and config and analog2 and enabled and group == \"" + equipID + "\"") > 0;
        boolean isTh1Enable =
            haystack.readDefaultVal("point and config and th1 and enabled and group == \"" + equipID + "\"") > 0;
        boolean isTh2Enable =
            haystack.readDefaultVal("point and config and th2 and enabled and group == \"" + equipID + "\"") > 0;
        double an1Val = haystack.readHisValByQuery("point and logical and analog1 and group == \"" + equipID + "\"");
        double an2Val = haystack.readHisValByQuery("point and logical and analog2 and group == \"" + equipID + "\"");
        double th1Val =
            haystack.readHisValByQuery("point and logical and th1 and group == \"" + equipID + "\"");
        double th2Val =
            haystack.readHisValByQuery("point and logical and th2 and group == \"" + equipID + "\"");
        int size = 0;
        
        double offset = tempOffset/10;
        double t = currentTemp + offset;
        
        monitoringPoints.put("curtempwithoffset",(currentTemp));
        
        if (tempOffset  != 0) {
            monitoringPoints.put("TemperatureOffset",tempOffset);
        }else{
            monitoringPoints.put("TemperatureOffset",0);
        }
        
        if(isAnalog1Enable){
            size++;
            monitoringPoints.put("iAn1Enable","true");
        } else monitoringPoints.put("iAn1Enable","false");
        
        if(isAnalog2Enable){
            size++;
            monitoringPoints.put("iAn2Enable","true");
        }
        else monitoringPoints.put("iAn2Enable","false");
        
        if(isTh1Enable){
            size++;
            monitoringPoints.put("isTh1Enable","true");
        }
        else monitoringPoints.put("isTh1Enable","false");
        
        if(isTh2Enable){
            size++;
            monitoringPoints.put("isTh2Enable","true");
        }
        else monitoringPoints.put("isTh2Enable","false");
        
        monitoringPoints.put("size",size);
        if (analog1Sensor >= 0 ) {
            Sensor selectedSensor = SensorManager.getInstance().getExternalSensorList().get((int) analog1Sensor );
            monitoringPoints.put("Analog1",selectedSensor.sensorName );
            monitoringPoints.put("Unit1", selectedSensor.engineeringUnit);
            monitoringPoints.put("An1Val",an1Val);
        }
        
        if (analog2Sensor >= 0) {
            Sensor selectedSensor = SensorManager.getInstance().getExternalSensorList().get((int) analog2Sensor );
            monitoringPoints.put("Analog2", selectedSensor.sensorName);
            monitoringPoints.put("Unit2", selectedSensor.engineeringUnit);
            monitoringPoints.put("An2Val",an2Val);
        }
        
        if (th1Sensor >= 0) {
            Thermistor selectedSensor = Thermistor.getThermistorList().get((int) th1Sensor );
            monitoringPoints.put("Thermistor1", selectedSensor.sensorName);
            monitoringPoints.put("Unit3", selectedSensor.engineeringUnit);
            monitoringPoints.put("Th1Val",th1Val);
        }
        if (th2Sensor >= 0) {
            Thermistor selectedSensor = Thermistor.getThermistorList().get((int) th2Sensor);
            monitoringPoints.put("Thermistor2", selectedSensor.sensorName);
            monitoringPoints.put("Unit4", selectedSensor.engineeringUnit);
            monitoringPoints.put("Th2Val",th2Val);
        }
        
        return monitoringPoints;
    }
    
    private static String getAnalogShortDis(int analog) {
        String shortDis = "Generic 0-10 Voltage";
        switch (analog) {
            case 0:
                shortDis = "Generic 0-10 Voltage";
                break;
            case 1:
                shortDis = "Pressure [0-2 in.]";
                break;
            case 2:
                shortDis = "Pressure[0-0.25 in. Differential]";
                break;
            case 3:
                shortDis = "Airflow";
                break;
            case 4:
                shortDis = "Humidity";
                break;
            case 5:
                shortDis = "CO2 Level";
                break;
            case 6:
                shortDis = "CO Level";
                break;
            case 7:
                shortDis = "NO2 Level";
                break;
            case 8:
                shortDis = "Current Drawn[CT 0-10]";
                break;
            case 9:
                shortDis = "Current Drawn[CT 0-20]";
                break;
            case 10:
                shortDis = "Current Drawn[CT 0-50]";
                break;
            case 11:
                shortDis ="ION Density";
                break;
        }
        return shortDis;
    }
}
