package a75f.io.logic.tuners;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;

class TemperatureLimitTuners {
    
    public static void addDefaultTempLimitTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                             String tz) {
    
        Point snCoolingAirflowTemperature  = new Point.Builder()
                                                 .setDisplayName(equipDis+"-"+"snCoolingAirflowTemp")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                                 .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                 .addMarker("sn").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("sp")
                                                 .setMinVal("35").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                                                 .setUnit("\u00B0F")
                                                 .setTz(tz)
                                                 .build();
        String snCoolingAirflowTemperatureId = hayStack.addPoint(snCoolingAirflowTemperature);
        hayStack.writePointForCcuUser(snCoolingAirflowTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                                      TunerConstants.SN_COOLING_AIRFLOW_TEMP, 0);
        hayStack.writeHisValById(snCoolingAirflowTemperatureId, TunerConstants.SN_COOLING_AIRFLOW_TEMP);
    
        Point snHeatingAirflowTemperature  = new Point.Builder()
                                                 .setDisplayName(equipDis+"-"+"snHeatingAirflowTemp")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                                 .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                 .addMarker("sn").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("sp")
                                                 .setMinVal("65").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                                                 .setUnit("\u00B0F")
                                                 .setTz(tz)
                                                 .build();
        String snHeatingAirflowTemperatureId = hayStack.addPoint(snHeatingAirflowTemperature);
        hayStack.writePointForCcuUser(snHeatingAirflowTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                                      TunerConstants.SN_HEATING_AIRFLOW_TEMP, 0);
        hayStack.writeHisValById(snHeatingAirflowTemperatureId, TunerConstants.SN_HEATING_AIRFLOW_TEMP);
        
    }
}
