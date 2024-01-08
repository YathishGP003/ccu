package a75f.io.renatus.views.MasterControl;

import static a75f.io.logic.bo.building.schedules.ScheduleManager.isHeatingOrCoolingLimitsNull;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsiusRelative;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HDict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.util.SchedulableMigrationKt;
import a75f.io.domain.api.Domain;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.migration.MigrationHandler;
import a75f.io.logic.migration.scheduler.SchedulerRevampMigration;
import a75f.io.renatus.schedules.ScheduleUtil;

import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.renatus.util.CCUUiUtil;

public class MasterControlUtil {


    public static String getAdapterVal(double val, boolean isZone) {
        if (isCelsiusTunerAvailableStatus()) {
            if(isZone) {
                return (fahrenheitToCelsius(val) + "\u00B0C");
            }else {
                return ((int) val + "\u00B0F  (" + fahrenheitToCelsius(val) + "\u00B0C)" );

            }
        }  else
            return ((int) val) + "\u00B0F";
    }

    public static String getAdapterValDeadBand(double val, boolean isZone) {
        if (isCelsiusTunerAvailableStatus()) {
            double celsiusVal = fahrenheitToCelsiusRelative(val);
            if (celsiusVal - (int) celsiusVal != 0)
                celsiusVal = CCUUtils.roundToOneDecimal(celsiusVal);
            if(isZone){
                return   celsiusVal + "\u00B0C";
            }else {
                return (val) + "\u00B0F  (" + celsiusVal + "\u00B0C)";
            }

        } else
            return (val) + "\u00B0F";
    }

    public static String getAdapterValDiff(double val) {
        if (isCelsiusTunerAvailableStatus()) {
            double celsiusVal = fahrenheitToCelsiusRelative(val);
            return ((int)val) + "\u00B0F  (" + celsiusVal + "\u00B0C)";
        } else
            return ((int)val) + "\u00B0F";
    }

    public static double getAdapterFarhenheitVal(String val) {
        double fahrenheitVal = Double.parseDouble(StringUtils.substringBefore(val, "\u00B0F" ));
        if (fahrenheitVal - (int) fahrenheitVal != 0)
            fahrenheitVal = CCUUtils.roundToOneDecimal(fahrenheitVal);
        return fahrenheitVal;
    }



    public static double zoneMinHeatingVal() {
        if (isMigrated()) {
            if (isZonesAvailable()) {
                ArrayList<HashMap<Object, Object>> allHeatingMin =
                        CCUHsApi.getInstance().readAllEntities("schedulable and point and limit and min and heating and user and zone");
                double min = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and min and heating and user and zone");
                for (HashMap<Object, Object> heatingMin : allHeatingMin) {
                    HashMap<Object,Object> zone = CCUHsApi.getInstance().readMapById(heatingMin.get("roomRef").toString() );
                    if(zone.isEmpty() || checkIfNonTempEquipInZone(zone))
                        continue;
                    double val = CCUHsApi.getInstance().readPointPriorityVal(heatingMin.get("id").toString());
                    min = Math.min(val, min);
                }
                return min;
            } else {
                return Domain.buildingEquip.getHeatingUserLimitMin().readPriorityVal();
            }
        } else {
            return Domain.buildingEquip.getHeatingUserLimitMin().readPriorityVal();
        }

    }

    public static double zoneMaxHeatingVal() {
        if (isMigrated()) {
            if (isZonesAvailable()) {
                ArrayList<HashMap<Object, Object>> allHeatingMax =
                        CCUHsApi.getInstance().readAllEntities("schedulable and point and limit and max and heating and user and zone");
                double max = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and max and heating and user and zone");
                for (HashMap<Object, Object> heatingMax : allHeatingMax) {
                    HashMap<Object,Object> zone = CCUHsApi.getInstance().readMapById(heatingMax.get("roomRef").toString());
                    if(zone.isEmpty() || checkIfNonTempEquipInZone(zone))
                        continue;
                    double val = CCUHsApi.getInstance().readPointPriorityVal(heatingMax.get("id").toString());
                    max = Math.max(val, max);
                }
                return max;
            } else {
                return Domain.buildingEquip.getHeatingUserLimitMax().readPriorityVal();
            }
        } else {
            return Domain.buildingEquip.getHeatingUserLimitMax().readPriorityVal();
        }
    }


    public static double zoneMinCoolingVal() {
        if (isMigrated()) {
            if (isZonesAvailable()) {
                ArrayList<HashMap<Object, Object>> allCoolingMin =
                        CCUHsApi.getInstance().readAllEntities("schedulable and point and limit and min and cooling and user and zone");
                double min = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and min and cooling and user and zone");
                for (HashMap<Object, Object> coolingMin : allCoolingMin) {
                    HashMap<Object,Object> zone = CCUHsApi.getInstance().readMapById(
                            coolingMin.get("roomRef").toString() );
                    if(zone.isEmpty() || checkIfNonTempEquipInZone(zone))
                        continue;
                    double val = CCUHsApi.getInstance().readPointPriorityVal(coolingMin.get("id").toString());
                    min = Math.min(val, min);
                }
                return min;
            } else {
                return Domain.buildingEquip.getCoolingUserLimitMin().readPriorityVal();
            }
        } else {
            return Domain.buildingEquip.getCoolingUserLimitMin().readPriorityVal();
        }
    }

    public static double zoneMaxCoolingVal() {
        if (isMigrated()) {
            if (isZonesAvailable()) {
                ArrayList<HashMap<Object, Object>> allCoolingMax =
                        CCUHsApi.getInstance().readAllEntities("schedulable and point and limit and max and cooling and user and zone");
                double max = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and max and cooling and user and zone");
                for (HashMap<Object, Object> coolingMax : allCoolingMax) {
                    HashMap<Object,Object> zone = CCUHsApi.getInstance().readMapById(coolingMax .get("roomRef").toString() );
                    if(zone.isEmpty() || checkIfNonTempEquipInZone(zone))
                        continue;
                    double val = CCUHsApi.getInstance().readPointPriorityVal(coolingMax.get("id").toString());
                    max = Math.max(val, max);
                }
                return max;
            } else {
                return Domain.buildingEquip.getCoolingUserLimitMax().readPriorityVal();
            }
        } else {
            return Domain.buildingEquip.getCoolingUserLimitMax().readPriorityVal();
        }
    }

    public static boolean isZonesAvailable() {
        ArrayList<HashMap<Object, Object>> allZones = CCUHsApi.getInstance().readAllEntities("room" );
        return !allZones.isEmpty();
    }

    public static String isValidData(ArrayList<Schedule> schedules, String heatingMax, String heatingMin, String coolingMax, String coolingMin,
                                     String coolingDeadBand, String heatingDeadBand, String buildingLimMin,
                                     String buildingLimMax, String unoccupiedzoneSetback, String buildingZoneDifferential, List<Zone> zones, List<Equip> equipList) {
        String WarningMessage;
        double heatingMaxVal;
        double heatingMinVal;
        double coolingMaxVal;
        double coolingMinVal;
        double coolingDeadBandVal;
        double heatingDeadBandVal;
        double buildingLimMinVal;
        double buildingLimMaxVal;
        double buildingZoneDifferentialVal;
        double unoccupiedZoneSetBackval;


        heatingMaxVal = MasterControlUtil.getAdapterFarhenheitVal(heatingMax);
        heatingMinVal = MasterControlUtil.getAdapterFarhenheitVal(heatingMin);
        coolingMaxVal = MasterControlUtil.getAdapterFarhenheitVal(coolingMax);
        coolingMinVal = MasterControlUtil.getAdapterFarhenheitVal(coolingMin);
        coolingDeadBandVal = MasterControlUtil.getAdapterFarhenheitVal(coolingDeadBand);
        heatingDeadBandVal = MasterControlUtil.getAdapterFarhenheitVal(heatingDeadBand);
        buildingLimMinVal = MasterControlUtil.getAdapterFarhenheitVal(buildingLimMin);
        buildingLimMaxVal = MasterControlUtil.getAdapterFarhenheitVal(buildingLimMax);
        unoccupiedZoneSetBackval = MasterControlUtil.getAdapterFarhenheitVal(unoccupiedzoneSetback);
        buildingZoneDifferentialVal = MasterControlUtil.getAdapterFarhenheitVal(buildingZoneDifferential);

        if ((buildingLimMinVal +  (buildingZoneDifferentialVal + unoccupiedZoneSetBackval)) > heatingMinVal) {
            WarningMessage = "Please go back and edit the Heating Limit Min temperature/ Unoccupied Zone Setback to be within the temperature limits of the building " +
                    "or adjust the temperature limits of the building to accommodate the required Heating Limit Min temperature/ Unoccupied Zone Setback as per" +
                    " formula \n > \"Heating User Limit Min - (unoccupiedZoneSetback + buildingToZoneDifferential) > Building Limit Min\" ";
        } else if ((buildingLimMaxVal - (buildingZoneDifferentialVal + unoccupiedZoneSetBackval) < coolingMaxVal)) {
            WarningMessage = "Please go back and edit the Cooling Limit Max temperature/ Unoccupied Zone Setback to be within the temperature limits" +
                    " of the building or adjust the temperature limits of the building to accommodate the required Cooling Limit Max temperature/ Unoccupied" +
                    " Zone Setback as per formula \n > \"Cooling User Limit Max + (unoccupiedZoneSetback + buildingToZoneDifferential) < Building Limit Max \"";
        }else
            WarningMessage = validateLimits(heatingMaxVal, heatingMinVal, heatingDeadBandVal,
                    coolingMaxVal, coolingMinVal, coolingDeadBandVal);


        if (WarningMessage == null) {
            WarningMessage = validateZoneVal(buildingLimMinVal, buildingZoneDifferentialVal, buildingLimMaxVal, heatingMinVal, heatingMaxVal, coolingMinVal,
                        coolingMaxVal,unoccupiedZoneSetBackval);
            StringBuilder globalWarning = validateGlobalSchedule(schedules,buildingLimMinVal, buildingZoneDifferentialVal, buildingLimMaxVal, heatingMinVal, heatingMaxVal, coolingMinVal,
                    coolingMaxVal, zones, equipList);
            if(globalWarning.length() > 1)
                WarningMessage = WarningMessage + globalWarning;


            if (!getSpecialSchedule(null).isEmpty() || isZoneSpecialExist())  {
                String buildingSpecial = validateSpecialSchedule(null, buildingLimMinVal, buildingLimMaxVal, unoccupiedZoneSetBackval, buildingZoneDifferentialVal,
                        heatingMinVal,heatingMaxVal,coolingMinVal,coolingMaxVal);
                String val = validateZoneSpecialSchedule(null, buildingLimMinVal, buildingLimMaxVal, unoccupiedZoneSetBackval, buildingZoneDifferentialVal,
                        heatingMinVal,heatingMaxVal,coolingMinVal,coolingMaxVal);

                if (val != null || buildingSpecial != null) {
                    WarningMessage = WarningMessage + "\n \n For the Below Special schedules, please go back and edit the Heating/Cooling limit min/max temperature/desiredTemp to be within the temperature limits of the building"
                            + " or adjust the temperature limits of the building to accommodate the required Heating/Cooling user limit min/max temperature."
                            + (val != null ? val : "") + (buildingSpecial != null ? buildingSpecial : "");
                }


            }
        }

        if(WarningMessage.length() >1)
           return WarningMessage;
        else
            return null;
    }

    private static List<HashMap<Object, Object>> getSpecialSchedule(String zoneId) {
        return CCUHsApi.getInstance().getSpecialSchedules(zoneId);
    }

    private static String validateZoneSpecialSchedule(String zoneId, double buildingLimMin, double buildingLimMax,
                                                      double unoccupiedSetback, double buildingZoneDifferentialVal,double heatingMin,
                                                      double heatingMax, double coolingMin, double coolingMax) {
        String WarningMessage = null;
        ArrayList<HashMap<Object, Object>> zones = CCUHsApi.getInstance().readAllEntities("room");
        for (HashMap<Object, Object> zone : zones) {
            List<Schedule.Days> combinedSpecialSchedules = Schedule.getSpecialScheduleDaysForZone(zone.get("id").toString().startsWith("@")? zoneId : "@"+zoneId);
            if(!combinedSpecialSchedules.isEmpty())
                WarningMessage = validateSpecialSchedule(zone.get("id").toString(),
                                buildingLimMin, buildingLimMax, 0, buildingZoneDifferentialVal, heatingMin,
                                heatingMax, coolingMin, coolingMax);




        }
        return WarningMessage;
    }

    private static String validateSpecialSchedule(String zoneId, double buildingLimMin, double buildingLimMax,
                                                  double unoccupiedSetback, double buildingZoneDifferentialVal,double heatingMin,
                                                  double heatingMax, double coolingMin, double coolingMax) {
        List<HashMap<Object, Object>> specialSchedules = getSpecialSchedule(zoneId);
        StringBuilder warningMessage = new StringBuilder();
        double unoccupiedSetbackVal = unoccupiedSetback;

        if(zoneId != null){
            HashMap<Object, Object> setBackHashMap = CCUHsApi.getInstance().readEntity("unoccupied and " +
                    "setback and zone and roomRef == \"" + zoneId + "\"");
            unoccupiedSetbackVal = CCUHsApi.getInstance().readPointPriorityVal(setBackHashMap.get("id").toString());
        }
        for (HashMap<Object, Object> schedule: specialSchedules) {
            boolean addScheduleToWarning = false;
            HDict range = (HDict) schedule.get(Tags.RANGE);
            double heatingLimMin = Double.parseDouble(range.get(Tags.HEATING_USER_LIMIT_MIN).toString());
            double coolingLimMax = Double.parseDouble(range.get(Tags.COOLING_USER_LIMIT_MAX).toString());
            double heatingDesired = Double.parseDouble(range.get(Tags.HEATVAL).toString());
            double coolingDesired = Double.parseDouble(range.get(Tags.COOLVAL).toString());
            if( validateZone(buildingLimMin,heatingLimMin,buildingZoneDifferentialVal,unoccupiedSetbackVal,buildingLimMax,coolingLimMax) != null) {
                addScheduleToWarning = true;
            }
            StringBuilder desiredCheck = validateSpecialScheduleDesired(buildingLimMin,buildingLimMax, heatingDesired, coolingDesired);
            if (desiredCheck != null) {
                addScheduleToWarning = true;
                warningMessage.append(desiredCheck);
            }
            if(addScheduleToWarning){
                warningMessage.insert(0,"\n\t Special schedule - "+schedule.get("dis").toString());
            }
        }

        if(warningMessage.length() > 2){
           return warningMessage.toString();
        }

        return null;
    }

    public static String validateLimits(double heatingMaxVal, double heatingMinVal, double heatingDeadBandVal,
                                        double coolingMaxVal, double coolingMinVal, double coolingDeadBandVal) {
        String WarningMessage = null;
        if ((heatingMaxVal - heatingMinVal) < heatingDeadBandVal) {
            WarningMessage = "Heating limits are violating the deadband to be maintained, " +
                    "the difference in heating limit maximum and minimum to be more or than or equal to the heating deadband ";

        } else if ((coolingMaxVal - coolingMinVal) < coolingDeadBandVal) {
            WarningMessage = "Cooling limits are violating the deadband to be maintained, " +
                    "the difference in cooling limit maximum and minimum to be more or than or equal to the cooling deadband ";

        } else if ((heatingMaxVal + heatingDeadBandVal + coolingDeadBandVal) > coolingMaxVal) {
            WarningMessage = "Heating Limit Max is violating the Deadband to be maintained, it can be extended only till following condition is satisfied,\n" +
                    "Heating Limit Max + deadband (heating + cooling) should be less than or equal to Cooling Limit Max ";

        } else if ((heatingMinVal + heatingDeadBandVal + coolingDeadBandVal) > coolingMinVal) {
            WarningMessage = "Cooling Limit Min is violating the Deadband to be maintained, it can be extended only till following condition is satisfied, " +
                    "Cooling Limit min - deadband (heating + cooling) should be greater than or equal to Heating Limit Min.";
        }
        return WarningMessage;
    }

    public static String validateZone(double  buildingLimMinVal, double heatingMinVal, double buildingZoneDifferential, double unoccupiedZoneSetBackval,
                                      double buildingLimMaxVal, double coolingMaxVal) {
        String WarningMessage = null;

        if (buildingLimMinVal > (heatingMinVal - (buildingZoneDifferential + unoccupiedZoneSetBackval))) {
            WarningMessage = "Please go back and edit the Heating limit min temperature to be within the temperature limits of the building  " +
                    "or adjust the temperature limits of the building to accommodate the required Heating user limit min temperature";
        } else if (buildingLimMaxVal < (coolingMaxVal + (buildingZoneDifferential + unoccupiedZoneSetBackval))) {
            WarningMessage = "Please go back and edit the Cooling limit max temperature to be within the temperature limits of the building  " +
                    "or adjust the temperature limits of the building to accommodate the required Cooling user limit max temperature";

        }
        return WarningMessage;

    }

    public static String validateZoneVal(double buildingLimMinVal, double buildingZoneDifferential, double buildingLimMaxVal,
                                         double heatingMinVal, double heatingMaxVal, double coolingMinVal,
                                         double coolingMaxVal,double unoccupiedZoneSetback) {
        StringBuilder WarningMessage = new StringBuilder();
        ArrayList<HashMap<Object, Object>> zones = CCUHsApi.getInstance().readAllEntities("room");


        for (HashMap<Object, Object> zone : zones) {
            if (!checkIfNonTempEquipInZone(zone)) {
            String scheduleTypeId = CCUHsApi.getInstance().readId("point and scheduleType and roomRef == \"" + zone.get("id").toString() + "\"");
            if (scheduleTypeId != null) {
                int scheduleType = (int) CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId);
                if (scheduleType == 1) {
                    Schedule schedule = CCUHsApi.getInstance().getZoneSchedule(zone.get("id").toString(), false).get(0);

                    Double unoccupiedZoneSetBackval = schedule.getUnoccupiedZoneSetback();
                    if (unoccupiedZoneSetBackval == null) {
                        String unOccupiedId = CCUHsApi.getInstance().readId("schedulable and zone and unoccupied and setback and roomRef == \"" + zone.get("id").toString() + "\"");
                        unoccupiedZoneSetBackval = CCUHsApi.getInstance().readPointPriorityVal(unOccupiedId);
                    }
                    if (schedule.getMarkers().contains("followBuilding")) {
                        StringBuilder val = validation(schedule, buildingLimMinVal, buildingZoneDifferential, buildingLimMaxVal, unoccupiedZoneSetback,
                                heatingMinVal, heatingMaxVal, coolingMinVal, coolingMaxVal, zone.get("dis").toString(), false);
                        if (val != null)
                            WarningMessage = WarningMessage.append(val);

                    } else {
                        StringBuilder val = validation(schedule, buildingLimMinVal, buildingZoneDifferential, buildingLimMaxVal, unoccupiedZoneSetBackval,
                                heatingMinVal, heatingMaxVal, coolingMinVal, coolingMaxVal, zone.get("dis").toString(), true);
                        if (val != null)
                            WarningMessage = WarningMessage.append(val);
                    }
                } else if (scheduleType == 2) {
                    Schedule schedule = CCUHsApi.getInstance().getScheduleById(zone.get("scheduleRef").toString());
                    double unoccupiedZoneSetBackval = schedule.getUnoccupiedZoneSetback();
                    StringBuilder val = validation(schedule, buildingLimMinVal, buildingZoneDifferential, buildingLimMaxVal, unoccupiedZoneSetBackval,
                            heatingMinVal, heatingMaxVal, coolingMinVal, coolingMaxVal, zone.get("dis").toString(), true);
                    if (val != null)
                        WarningMessage = WarningMessage.append(val);

                }
            }

            }
        }

        if (WarningMessage.length() > 1) {
            WarningMessage.insert(0, "Schedule Impacts \n");
            WarningMessage.append("Please go back and edit respective limits or edit zone limits/desired temp");
            return WarningMessage.toString();
        }

        return "";
    }

    public static boolean validateNamed(double heatingUserLimitMin,double coolingUserLimitMax,
                                        double unOccupiedzonesetback) {
        double buildingZoneDifferential = Domain.buildingEquip.getBuildingToZoneDifferential().readPriorityVal();
        double buildingLimMin = BuildingTunerCache.getInstance().getBuildingLimitMin();
        double buildingLimMax = BuildingTunerCache.getInstance().getBuildingLimitMax();

        if(buildingLimMin > (heatingUserLimitMin - (buildingZoneDifferential + unOccupiedzonesetback)))
            return false;
        else return !(buildingLimMax < (coolingUserLimitMax + (buildingZoneDifferential + unOccupiedzonesetback)));
    }

    public static boolean validateNamedDeadBand(double heatingMinVal, double heatingMaxVal,
                                                double coolingMinVal, double coolingMaxVal,
                                                double heatingDeadBandVal, double coolingDeadBandVal) {
        if ((heatingMaxVal - heatingMinVal) < heatingDeadBandVal)
            return false;
        else if ((coolingMaxVal - coolingMinVal) < coolingDeadBandVal)
            return false;
        else if ((heatingMaxVal + heatingDeadBandVal + coolingDeadBandVal) > coolingMaxVal)
            return false;
        else return !((heatingMinVal + heatingDeadBandVal + coolingDeadBandVal) > coolingMinVal);
    }

    public static String validateDesiredTemp(double coolingTemp, double heatingTemp,
                                             double coolingMin, double coolingMax,
                                             double heatingMin, double heatingMax,
                                             double heatDB,double coolDB){
        String WarningMessage = null;
        if(coolingTemp < coolingMin || coolingTemp> coolingMax){
            WarningMessage = "Please go back and edit the Cooling desired Temp to be within the building cooling limits ";
        }
        if(heatingTemp < heatingMin || heatingTemp > heatingMax){
            WarningMessage = "Please go back and edit the Heating desired Temp to be within the building heating limits ";
        }
        if((coolingTemp < coolingMin || coolingTemp> coolingMax) && (heatingTemp < heatingMin || heatingTemp > heatingMax)){
            WarningMessage = "Please go back and edit both Heating and Cooling desired Temp to be within the building user limits";
        }if((coolingTemp-heatingTemp) < (heatDB+coolDB)){
            WarningMessage = "Please go back and edit both Heating and Cooling desired Temp to be accommodate with deadbands";
        }

        return WarningMessage;
    }

    private static StringBuilder validation(Schedule schedule, double buildingLimMinVal, double buildingZoneDifferential, double buildingLimMaxVal,
                                            double unoccupiedZoneSetBackval, double heatingMinVal, double heatingMaxVal, double coolingMinVal,
                                            double coolingMaxVal, String zoneDis,boolean isNamed){
        StringBuilder WarningMessage = new StringBuilder();
        for (Schedule.Days day : schedule.getDays()) {
            if(isHeatingOrCoolingLimitsNull(day)){
                continue;
            }
            if (buildingLimMinVal > (day.getHeatingUserLimitMin() - (buildingZoneDifferential + unoccupiedZoneSetBackval))) {
                WarningMessage.append(ScheduleUtil.getDayString(day.getDay() + 1))
                        .append(" ")
                        .append(day.getSthh()).append(":").append(day.getStmm()).append("-")
                        .append(day.getEthh()).append(":").append(day.getEtmm())
                        .append(" \n\tHeatingUserLimitMin - ").append(day.getHeatingUserLimitMin()).append("Building limit -(").append(buildingLimMinVal).append("-").append(buildingLimMaxVal).append(") \n");
            }
            if (buildingLimMaxVal < (day.getCoolingUserLimitMax() + (buildingZoneDifferential + unoccupiedZoneSetBackval))) {
                WarningMessage.append(ScheduleUtil.getDayString(day.getDay() + 1))
                        .append(" ")
                        .append(day.getSthh()).append(":").append(day.getStmm()).append("-")
                        .append(day.getEthh()).append(":").append(day.getEtmm())
                        .append(" \n\tCoolingUserLimitMax - ").append(day.getCoolingUserLimitMax()).append("Building limit -(").append(buildingLimMinVal).append("-").append(buildingLimMaxVal).append(") \n");
            }
            if(!isNamed) {
                if (day.getHeatingVal() < heatingMinVal || day.getHeatingVal() > heatingMaxVal) {
                    WarningMessage.append(ScheduleUtil.getDayString(day.getDay() + 1))
                            .append(" ")
                            .append(day.getSthh()).append(":").append(day.getStmm()).append("-")
                            .append(day.getEthh()).append(":").append(day.getEtmm())
                            .append("\n\tHeatingDesired - ")
                            .append(day.getHeatingVal()).append(" should be within the limits\n");
                }
                if (day.getCoolingVal() < coolingMinVal || day.getCoolingVal() > coolingMaxVal) {
                    WarningMessage.append(ScheduleUtil.getDayString(day.getDay() + 1)).append(" ")
                            .append(day.getSthh()).append(":").append(day.getStmm()).append("-")
                            .append(day.getEthh()).append(":").append(day.getEtmm())
                            .append(" \n\tCoolingDesired - ")
                            .append(day.getCoolingVal()).append(" should be within the limits\n");
                }
                if ((day.getCoolingVal() - day.getHeatingVal()) < (day.getHeatingDeadBand() + day.getCoolingDeadBand())) {
                    WarningMessage.append(ScheduleUtil.getDayString(day.getDay() + 1)).append(" ")
                            .append(day.getSthh()).append(":").append(day.getStmm()).append("-")
                            .append(day.getEthh()).append(":").append(day.getEtmm())
                            .append(" \n\tDesired Temp - ").append(day.getHeatingVal()).append("|")
                            .append(day.getCoolingVal()).append("should respect the deadbands\n");
                }
            }
        }
        if(WarningMessage.length() > 2){
            String  zoneDetails = "\n\tZone - ("+zoneDis+") ";
            WarningMessage.insert(0,zoneDetails);
            return WarningMessage;
        }
        return null;
    }

    private static StringBuilder validateSpecialScheduleDesired(double buildingMin,double buildingMax,  double HDT, double CDT ) {

        StringBuilder WarningMessage = new StringBuilder();
        if (HDT < buildingMin || HDT > buildingMax) {
            WarningMessage.append(" \n\tHeatingDesired - ").append(HDT).append(" should be within the building limits").append("\n");
        }
        if (CDT < buildingMin || CDT > buildingMax) {
            WarningMessage.append(" \tCoolingDesired - ").append(CDT).append(" should be within the building limits");
        }
        if(WarningMessage.length() > 2)
            return WarningMessage;

        return null;
    }

    private static boolean isZoneSpecialExist(){
        ArrayList<HashMap<Object, Object>> zones = CCUHsApi.getInstance().readAllEntities("room");
        if(!zones.isEmpty()) {
            for (HashMap<Object, Object> zone : zones) {
                Set<Schedule.Days> combinedSpecialSchedules = Schedule.combineSpecialSchedules(zone.get("id").toString().replace("@", ""));
                return !(combinedSpecialSchedules.isEmpty());
            }
        }
        return false;
    }

    public static boolean isMigrated(){
        ArrayList<HashMap<Object , Object>> isSchedulableAvailable = CCUHsApi.getInstance().readAllSchedulable();
        return ((isSchedulableAvailable != null && SchedulableMigrationKt.validateMigration()));
    }

    public static boolean isNonTempModule(String profileType){
        return  (profileType.contains(ProfileType.EMR.toString()) || profileType.contains(ProfileType.PLC.toString())
                || profileType.contains(ProfileType.TEMP_MONITOR.toString())
                || profileType.contains("MODBUS") || profileType.contains(ProfileType.HYPERSTAT_MONITORING.toString()));
    }


    public static StringBuilder validateGlobalSchedule(ArrayList<Schedule> allSchedules, double buildingLimMinVal, double buildingZoneDifferential, double buildingLimMaxVal,
                                                       double heatingMinVal, double heatingMaxVal, double coolingMinVal,
                                                       double coolingMaxVal, List<Zone> zones, List<Equip> equipList) {
        ArrayList<HashMap<Object, Object>> allZones = CCUHsApi.getInstance().readAllEntities("room" );
        ArrayList<String> allRoomRef  = new ArrayList<>();
        allZones.forEach( zone -> allRoomRef.add(zone.get("id").toString()));
        StringBuilder WarningMessage = new StringBuilder();
        for (Schedule schedule : allSchedules) {
            if (schedule.getMarkers().contains("followBuilding") && !allRoomRef.contains(schedule.getRoomRef())) {
                double unoccupiedZoneSetBackval = schedule.getUnoccupiedZoneSetback();
                StringBuilder val = null;
                if (!checkNonTempProfilePairedInRoom(getZoneIdByScheduleId(schedule.getRoomRef(), zones), equipList)) {
                    val = validation(schedule, buildingLimMinVal, buildingZoneDifferential, buildingLimMaxVal, unoccupiedZoneSetBackval,
                            heatingMinVal, heatingMaxVal, coolingMinVal, coolingMaxVal, getZoneName(schedule.getRoomRef(), zones), false);
                }
                if (val != null)
                    WarningMessage = WarningMessage.append(val);

            }
        }

        if(WarningMessage.length() > 1)
            WarningMessage = WarningMessage.insert(0,"\n\n On other CCUs \n");

        return WarningMessage;

    }
    private static String getZoneIdByScheduleId(String id, List<Zone> zones) {
        for(Zone zone : zones){
            if(zone.getId().equals(id)){
                return zone.getId();
            }
        }
        return null;
    }

    private static boolean checkNonTempProfilePairedInRoom(String roomRef, List<Equip> equips) {
        ArrayList<Equip> roomEquips = new ArrayList<>();
        for(Equip equip : equips){
            if(equip.getRoomRef().equals(roomRef)){
                roomEquips.add(equip);
            }
        }
        return roomEquips.stream().anyMatch(equip ->
                MasterControlUtil.isNonTempModule(equip.getProfile()));
    }

    private static String getZoneName(String id, List<Zone> zones) {
        for(Zone zone : zones){
            if(zone.getId().equals(id)){
                return zone.getDisplayName();
            }
        }
        return null;
    }

    public static boolean checkIfNonTempEquipInZone(HashMap<Object,Object> zone){
        if(zone != null) {
            ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance().readAllEntities("equip and roomRef ==\""
                    + zone.get("id").toString() + "\"");
            return equips.stream().anyMatch(equip ->
                    MasterControlUtil.isNonTempModule(equip.get("profile").toString()));
        }
        return false;
    }

}
