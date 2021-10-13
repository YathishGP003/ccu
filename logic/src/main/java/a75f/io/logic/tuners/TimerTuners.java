package a75f.io.logic.tuners;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;

class TimerTuners {
    
    public static void addDefaultTimerTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                                 String tz) {
    
        Point zoneDeadTime  = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"zoneDeadTime")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                  .addMarker("zone").addMarker("dead").addMarker("time").addMarker("sp")
                                  .setMinVal("1").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                                  .setUnit("m")
                                  .setTz(tz)
                                  .build();
        String zoneDeadTimeId = hayStack.addPoint(zoneDeadTime);
        hayStack.writePointForCcuUser(zoneDeadTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_DEAD_TIME,
                                      0);
        hayStack.writeHisValById(zoneDeadTimeId, TunerConstants.ZONE_DEAD_TIME);
    
        Point autoAwayTime  = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"autoAwayTime")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                  .addMarker("auto").addMarker("away").addMarker("time").addMarker("sp")
                                  .setMinVal("0").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                                  .setUnit("m")
                                  .setTz(tz)
                                  .build();
        String autoAwayTimeId = hayStack.addPoint(autoAwayTime);
        hayStack.writePointForCcuUser(autoAwayTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 30.0, 0);
        hayStack.writeHisValById(autoAwayTimeId, 30.0);
    
        Point forcedOccupiedTime  = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"forcedOccupiedTime")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                        .addMarker("forced").addMarker("occupied").addMarker("time").addMarker("sp")
                                        .setMinVal("0").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                                        .setUnit("m")
                                        .setTz(tz)
                                        .build();
        String forcedOccupiedTimeId = hayStack.addPoint(forcedOccupiedTime);
        hayStack.writePointForCcuUser(forcedOccupiedTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_FORCED_OCCUPIED_TIME, 0);
        hayStack.writeHisValById(forcedOccupiedTimeId, TunerConstants.ZONE_FORCED_OCCUPIED_TIME);
    
        Point cmResetCommand  = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"cmResetCommandTimer")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                    .addMarker("reset").addMarker("command").addMarker("time").addMarker("sp")
                                    .setMinVal("0").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                                    .setUnit("m")
                                    .setTz(tz)
                                    .build();
        String cmResetCommandId = hayStack.addPoint(cmResetCommand);
        hayStack.writePointForCcuUser(cmResetCommandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                            TunerConstants.CM_RESET_CMD_TIME, 0);
        hayStack.writeHisValById(cmResetCommandId, TunerConstants.CM_RESET_CMD_TIME);
    
    
    }
}
