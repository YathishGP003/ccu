package a75f.io.logic.bo.building.ccu;


import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.haystack.device.ControlMote;

public class CazEquipUtil {

    public static void updateThermistor2Association(int configVal, Point configPoint) {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        Thermistor2AssociationType configType = Thermistor2AssociationType.values()[configVal];

        switch (configType) {

            case EXTERNAL_10K_TEMP_SENSOR:
                HashMap<Object,Object> supplyAirTempPoint = CCUHsApi.getInstance().readEntity("point and supply and cur and sensor and  " +
                        "ti and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!supplyAirTempPoint.isEmpty()) {
                    CCUHsApi.getInstance().deleteEntity(supplyAirTempPoint.get("id").toString());
                }
                createExternal10kTempSensorPoint(equip, Integer.parseInt(nodeAddr));
                break;
            case SUPPLY_AIR_TEMP:
                HashMap<Object, Object> external10KTempSensorPoint = CCUHsApi.getInstance().readEntity("point and external and cur and sensor and  " +
                        "ti and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!external10KTempSensorPoint.isEmpty()) {
                    CCUHsApi.getInstance().deleteEntity(external10KTempSensorPoint.get("id").toString());
                }
                createSupplyAirTempPoint(equip, Integer.parseInt(nodeAddr));
                break;
        }
        ControlMote.setPointEnabled(Integer.valueOf(nodeAddr), Port.TH2_IN.name(), configVal > 0 ? true : false );
        CCUHsApi.getInstance().scheduleSync();
    }



    private static void createSupplyAirTempPoint(Equip equip, int nodeAddr) {
        CcuLog.d(L.TAG_CCU_ZONE, "CAZ CreateSupplyAirTemp point");
        Point external10kTempSensor = new Point.Builder()
                .setDisplayName(equip.getDisplayName() + "-supplyAirTemperature")
                .setEquipRef(equip.getId())
                .setSiteRef(equip.getSiteRef())
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                .addMarker("ti").addMarker("temp").addMarker("supply").addMarker("cur").addMarker("sensor")
                .addMarker("logical").addMarker("zone").addMarker("his").addMarker("air")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(CCUHsApi.getInstance().getTimeZone())
                .build();
        String id = CCUHsApi.getInstance().addPoint(external10kTempSensor);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(id,0.0);
    }

    private static void createExternal10kTempSensorPoint(Equip equip, int nodeAddr) {
        CcuLog.d(L.TAG_CCU_ZONE, "CAZ CreateExternal10kTempSensor point");
        Point external10kTempSensorPoint = new Point.Builder()
                .setDisplayName(equip.getDisplayName() + "-external10kTempSensor")
                .setEquipRef(equip.getId())
                .setSiteRef(equip.getSiteRef())
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                .addMarker("ti").addMarker("temp").addMarker("cur").addMarker("sensor").addMarker("zone").addMarker("external")
                .addMarker("logical").addMarker("air").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(CCUHsApi.getInstance().getTimeZone())
                .build();
        String id = CCUHsApi.getInstance().addPoint(external10kTempSensorPoint);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(id,0.0);

    }

    public static double getConfigNumVal(String tags, String nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and ti and "+tags+" and group == \""+nodeAddr+"\"");
    }
}
