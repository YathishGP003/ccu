package a75f.io.logic.migration.point;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Queries;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.monitoring.HyperStatMonitoringUtil;
import a75f.io.logic.bo.building.plc.PlcRelayConfigHandler;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;

public class PointMigrationHandler {

    private PointMigrationHandler(){

    }

    public static void updateSenseAnalogInputUnitPointDisplayName(String analog){
        HashMap<Object, Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
        String siteDis = siteMap.get(Tags.DIS).toString();
        ArrayList<HashMap<Object, Object>> senseEquips = CCUHsApi.getInstance().readAllEntities(Queries.SENSE_EQUIP);
        if(!senseEquips.isEmpty()){
            for(HashMap<Object, Object> senseEquip : senseEquips){

                String equipId = senseEquip.get(Tags.ID).toString();
                String equipDis = siteDis + "-MONITORING-" + senseEquip.get(Tags.GROUP).toString() + "-";

                HashMap<Object, Object> senseAnalog1InputPoint = CCUHsApi.getInstance().readEntity("sense and " +
                        analog+" and unit and equipRef == \""+ equipId +"\"");
                if(!senseAnalog1InputPoint.isEmpty()) {
                    int configVal = CCUHsApi.getInstance().readDefaultVal("sense and "+analog+" and point and " +
                            "input and sensor and config" +
                            " and equipRef == \"" + equipId + "\"").intValue();
                    Bundle bundle = HyperStatMonitoringUtil.getAnalogBundle(configVal);
                    String shortDis = bundle.getString(Tags.SHORTDIS);
                    Point updatePoint = new Point.Builder().setHashMap(senseAnalog1InputPoint)
                            .setDisplayName(equipDis + shortDis)
                            .setShortDis(shortDis)
                            .build();
                    CCUHsApi.getInstance().updatePoint(updatePoint, updatePoint.getId());
                }
            }
        }
    }

    public static void updatePILoopAnalog1InputUnitPointDisplayName(){
        HashMap<Object, Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
        String siteDis = siteMap.get(Tags.DIS).toString();
        ArrayList<HashMap<Object, Object>> piLoopEquips = CCUHsApi.getInstance().readAllEntities(Queries.PI_LOOP_EQUIP);
        if(!piLoopEquips.isEmpty()){
            for(HashMap<Object, Object> piLoopEquip : piLoopEquips){

                String equipId = piLoopEquip.get(Tags.ID).toString();
                String equipDis = siteDis + "-PID-" + piLoopEquip.get(Tags.GROUP).toString();

                HashMap<Object, Object> piLoopAnalog1InputPoint = CCUHsApi.getInstance().readEntity("pid and " +
                        " process and variable and equipRef == \""+ equipId +"\"");
                if(!piLoopAnalog1InputPoint.isEmpty()) {
                    int configVal = CCUHsApi.getInstance().readDefaultVal("pid and analog1 and point and " +
                            "input and sensor and config" +
                            " and equipRef == \"" + equipId + "\"").intValue();
                    Bundle bundle = PlcRelayConfigHandler.getAnalog1Bundle(configVal);
                    String shortDis = bundle.getString(Tags.SHORTDIS);
                    Point updatePoint = new Point.Builder().setHashMap(piLoopAnalog1InputPoint)
                            .setDisplayName(equipDis + "-processVariable- " + shortDis)
                            .setShortDis(shortDis)
                            .build();
                    CCUHsApi.getInstance().updatePoint(updatePoint, updatePoint.getId());
                }
            }
        }
    }

    public static void updatePILoopAnalog2InputUnitPointDisplayName(){
        ArrayList<HashMap<Object, Object>> piLoopEquips = CCUHsApi.getInstance().readAllEntities(Queries.PI_LOOP_EQUIP);
        if(!piLoopEquips.isEmpty()){
            for(HashMap<Object, Object> piLoopEquip : piLoopEquips){

                String equipId = piLoopEquip.get(Tags.ID).toString();

                HashMap<Object, Object>piLoopAnalog2InputPoint = CCUHsApi.getInstance().readEntity("pid and " +
                        " dynamic and target and equipRef == \""+ equipId +"\"");
                if(!piLoopAnalog2InputPoint.isEmpty()) {
                    List<Sensor> sensorList =  SensorManager.getInstance().getExternalSensorList();
                    for(Sensor sensor : sensorList){
                        String sensorName = sensor.sensorName;
                        String dynamicTargetDisName = piLoopAnalog2InputPoint.get("dis").toString();
                        if(dynamicTargetDisName.contains(sensorName)){
                            String sensorNameWithColon = sensorName.replace("-", ":");
                            String modifiedDisplayName = dynamicTargetDisName.replace(sensorName, sensorNameWithColon);
                            Point updatePoint = new Point.Builder().setHashMap(piLoopAnalog2InputPoint)
                                    .setDisplayName(modifiedDisplayName)
                                    .build();
                            CCUHsApi.getInstance().updatePoint(updatePoint, updatePoint.getId());
                            break;
                        }
                    }
                }
            }
        }
    }
}
