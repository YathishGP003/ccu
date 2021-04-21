package a75f.io.logic.tuners;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;

public class SystemTuners {
    
    /**
     * When VAV/DAB system equip is created , this will be called to create PI related tuners on the SystemEquip.
     * And then copies all the levels present in BuildingTuner equip.
     */
    public static void addPITuners(String equipRef, String tunerGroup, String typeTag, CCUHsApi hayStack) {
        
        HashMap equip = hayStack.readMapById(equipRef);
        String equipDis = equip.get("dis").toString();
        String siteRef = equip.get("siteRef").toString();
        Point propGain = new Point.Builder()
                             .setDisplayName(equipDis+"-"+"proportionalKFactor")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef)
                             .setHisInterpolate("cov")
                             .addMarker("tuner").addMarker(typeTag).addMarker("writable").addMarker("his")
                             .addMarker("pgain").addMarker("sp").addMarker("system")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(tunerGroup)
                             .setTz(hayStack.getTimeZone())
                             .build();
        String pgainId = hayStack.addPoint(propGain);
        BuildingTunerUtil.copyFromBuildingTuner(pgainId, getQueryString(propGain), hayStack);
        hayStack.writeHisValById(pgainId, HSUtil.getPriorityVal(pgainId));
    
        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"integralKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker(typeTag).addMarker("writable").addMarker("his")
                                 .addMarker("igain").addMarker("sp").addMarker("system")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(tunerGroup)
                                 .setTz(hayStack.getTimeZone())
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        BuildingTunerUtil.copyFromBuildingTuner(igainId, getQueryString(integralGain), hayStack);
        hayStack.writeHisValById(igainId, HSUtil.getPriorityVal(igainId));
    
        Point propSpread = new Point.Builder()
                               .setDisplayName(equipDis+"-"+"temperatureProportionalRange")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipRef)
                               .setHisInterpolate("cov")
                               .addMarker("tuner").addMarker(typeTag).addMarker("writable").addMarker("his")
                               .addMarker("pspread").addMarker("sp").addMarker("system")
                               .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(tunerGroup)
                               .setTz(hayStack.getTimeZone())
                               .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        BuildingTunerUtil.copyFromBuildingTuner(pSpreadId, getQueryString(propSpread), hayStack);
        hayStack.writeHisValById(pSpreadId, HSUtil.getPriorityVal(pSpreadId));
    
        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"temperatureIntegralTime")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef)
                                    .setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker(typeTag).addMarker("writable").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp").addMarker("system")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(tunerGroup)
                                    .setUnit("m")
                                    .setTz(hayStack.getTimeZone())
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        BuildingTunerUtil.copyFromBuildingTuner(iTimeoutId, getQueryString(integralTimeout), hayStack);
        hayStack.writeHisValById(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));
    }
    
    /**
     * Creates a hayStack query using point marker tags.
     */
    private static String getQueryString(Point tunerPoint) {
        ArrayList<String> markersFiltered = tunerPoint.getMarkers();
        HSUtil.removeGenericMarkerTags(markersFiltered);
        return HSUtil.getQueryFromMarkers(markersFiltered);
    }
}
