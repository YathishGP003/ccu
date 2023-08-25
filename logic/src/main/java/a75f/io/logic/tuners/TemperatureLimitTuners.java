package a75f.io.logic.tuners;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;

class TemperatureLimitTuners {
    
    public static void addDefaultTempLimitTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                             String tz) {

        Point coolingAirflowTemperature  = new Point.Builder()
                                                 .setDisplayName(equipDis+"-"+"coolingAirflowTemp")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                                 .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                 .addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("sp")
                                                 .setMinVal("35").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                                                 .setUnit("\u00B0F")
                                                 .setTz(tz)
                                                 .build();
        String coolingAirflowTemperatureId = hayStack.addPoint(coolingAirflowTemperature);
        hayStack.writePointForCcuUser(coolingAirflowTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                                      TunerConstants.SN_COOLING_AIRFLOW_TEMP, 0);
        hayStack.writeHisValById(coolingAirflowTemperatureId, TunerConstants.SN_COOLING_AIRFLOW_TEMP);

        Point heatingAirflowTemperature  = new Point.Builder()
                                                 .setDisplayName(equipDis+"-"+"heatingAirflowTemp")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                                 .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                 .addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("sp")
                                                 .setMinVal("65").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                                                 .setUnit("\u00B0F")
                                                 .setTz(tz)
                                                 .build();
        String heatingAirflowTemperatureId = hayStack.addPoint(heatingAirflowTemperature);
        hayStack.writePointForCcuUser(heatingAirflowTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                                      TunerConstants.SN_HEATING_AIRFLOW_TEMP, 0);
        hayStack.writeHisValById(heatingAirflowTemperatureId, TunerConstants.SN_HEATING_AIRFLOW_TEMP);

    }
}
