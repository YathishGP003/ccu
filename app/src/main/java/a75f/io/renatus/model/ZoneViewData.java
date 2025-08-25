package a75f.io.renatus.model;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.equips.DabEquip;
import a75f.io.domain.equips.PlcEquip;
import a75f.io.domain.equips.SseEquip;
import a75f.io.domain.equips.VavEquip;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Thermistor;
import a75f.io.logic.bo.building.plc.PlcProfile;
import a75f.io.logic.bo.building.sensors.NativeSensor;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;
import a75f.io.logic.jobs.StringConstants;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint;

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
        DabEquip dabEquip = (DabEquip) Domain.INSTANCE.getDomainEquip(equipID);
        HashMap dabPoints = new HashMap();
        dabPoints.put("Profile","DAB");
        boolean isThermister1On = (CCUHsApi.getInstance().readDefaultVal(THERMISTER_QUERY_POINT + equipID + "\"") > 0) ;
        String equipStatusPoint = dabEquip.getEquipStatusMessage().readDefaultStrVal();
        double damperPosPoint = dabEquip.getNormalizedDamper1Cmd().readHisVal(); // revisit this
        double dischargePoint = dabEquip.getDischargeAirTemp1().readHisVal();
        double airflowCFM =  dabEquip.getAirFlowSensor().readHisVal();
        double reheatPoint = dabEquip.getReheatCmd().readHisVal();
        dabPoints.put(AIRFLOW_SENSOR,isThermister1On);
        if (!equipStatusPoint.isEmpty()) {
            dabPoints.put("Status",equipStatusPoint);
        }else{
            dabPoints.put("Status","OFF");
        }
        if (damperPosPoint > 0) {
            dabPoints.put("Damper",(int)damperPosPoint+"% Open");
        }else{
            dabPoints.put("Damper",0+"% Open");
        }
        if (dischargePoint  != 0) {
            dabPoints.put("Supply Airflow",dischargePoint+" \u2109");
        }else{
            dabPoints.put("Supply Airflow",0+" \u2109");
        }
        if (airflowCFM != 0.0) {
            dabPoints.put("Airflow CFM",String.format("%.0f", airflowCFM));
        }else{
            dabPoints.put("Airflow CFM",0);
        }
        if (reheatPoint  > 0) {
            dabPoints.put("Reheat Coil", reheatPoint+"% Open");
        }else{
            dabPoints.put("Reheat Coil", "0% Open");
        }
        return dabPoints;
    }
    
    public static HashMap getTIEquipPoints(String equipID) {
        
        HashMap tiPoints = new HashMap();
        tiPoints.put("Profile","TEMP_INFLUENCE");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and domainName == \"" + DomainName.equipStatusMessage + "\" and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            tiPoints.put("Status",equipStatusPoint);
        }else{
            tiPoints.put("Status","OFF");
        }
        return tiPoints;
    }

    public static HashMap getSSEEquipPoints(String equipID) {
        SseEquip sseEquip = (SseEquip) Domain.INSTANCE.getDomainEquip(equipID);
        HashMap ssePoints = new HashMap();
        ssePoints.put("Profile","SSE");
        boolean isThermister1On = (CCUHsApi.getInstance().readDefaultVal(THERMISTER_QUERY_POINT + equipID + "\"") > 0);
        
        String equipStatusPoint = sseEquip.getEquipStatusMessage().readDefaultStrVal();
        double dischargePoint = sseEquip.getDischargeAirTemperature().readHisVal();
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
        double damperPosPoint = TrueCFMUtil.isCfmOnEdgeActive(CCUHsApi.getInstance(), vavEquip.getId()) ? vavEquip.getDamperCmdCal().readHisVal() : vavEquip.getNormalizedDamperCmd().readHisVal();
        double valvePoint = CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + DomainName.chilledWaterValve + "\" and equipRef == \""+equipID+"\"");
        double reheatPoint = TrueCFMUtil.isCfmOnEdgeActive(CCUHsApi.getInstance(), vavEquip.getId()) ? vavEquip.getReheatCmdCal().readHisVal() : vavEquip.getReheatCmd().readHisVal();
        if (!(damperPosPoint > 0 || reheatPoint > 0)) {
            damperPosPoint = vavEquip.getNormalizedDamperCmd().readHisVal();
            reheatPoint = vavEquip.getReheatCmd().readHisVal();
        }

        double enteringAirPoint = vavEquip.getEnteringAirTemp().readHisVal();
        double dischargePoint = vavEquip.getDischargeAirTemp().readHisVal();
        double airflowCFM =  CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + DomainName.airFlowSensor + "\" and equipRef == \""+equipID+"\"");
        double condensateNC = CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + DomainName.condensateNC + "\" and equipRef == \""+equipID+"\"");
        double condensateNO = CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + DomainName.condensateNO + "\" and equipRef == \""+equipID+"\"");
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
            vavPoints.put("Reheat Coil",(int)reheatPoint+"% Open");
        } else if (valvePoint > 0) {
            vavPoints.put("Reheat Coil",(int)valvePoint+"% Open");
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
        if (condensateNC > 0.0 || condensateNO > 0.0) {
            vavPoints.put("Condensate","Condensate Sensed");
        } else {
            vavPoints.put("Condensate", 0);
        }

        HashMap<Object, Object> equip = CCUHsApi.getInstance().readMapById(equipID);
        if (equip.containsKey("series")) {
            vavPoints.put("Profile","VAV Series Fan");
        } else if (equip.containsKey("parallel")){
            vavPoints.put("Profile","VAV Parallel Fan");
        } else if (equip.containsKey("chilledBeam")) {
            vavPoints.put("Profile", "Active Chilled Beams + DOAS");
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
}
