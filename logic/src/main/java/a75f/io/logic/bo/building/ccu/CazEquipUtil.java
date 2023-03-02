package a75f.io.logic.bo.building.ccu;


import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class CazEquipUtil {

    public static String createSupplyAirTempPoint(Equip equip, int nodeAddr) {
        CcuLog.d(L.TAG_CCU_ZONE, "CAZ CreateSupplyAirTemp point");
        Point supplyAirTempPoint = new Point.Builder()
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
        String supplyAirTempId = CCUHsApi.getInstance().addPoint(supplyAirTempPoint);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(supplyAirTempId, 0.0);

        return supplyAirTempId;
    }

    public static double getConfigNumVal(String tags, String nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and ti and "+tags+" and group == \""+nodeAddr+"\"");
    }

    public static boolean isPortMappedToSupplyAirTemprature(String pointRef){
        HashMap<Object, Object> logicalPoint = CCUHsApi.getInstance().readEntity("point and id == " + pointRef);
        return logicalPoint.containsKey("supply");
    }

}
