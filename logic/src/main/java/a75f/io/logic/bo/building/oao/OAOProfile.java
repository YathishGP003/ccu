package a75f.io.logic.bo.building.oao;

import android.content.Context;
import android.util.Log;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

/*
*  OAO Combines both System profile and Zone Profile behaviours.
*  It controls system components like OA damper , but mapped to smart node device like a zone profile.
* */
public class OAOProfile
{
    
    public static boolean economizingAvailable = false;
    
    double economizingLoopOutput;
    double outsideAirCalculatedMinDamper;
    double outsideAirLoopOutput;
    double outsideAirFinalLoopOutput;
    double returnAirFinalOutput;
    EpidemicState epidemicState = EpidemicState.OFF;
    SystemMode systemMode;
    OAOEquip oaoEquip;
    
    public boolean isEconomizingAvailable()
    {
        return economizingAvailable;
    }
    public void setEconomizingAvailable(boolean economizingAvailable)
    {
        this.economizingAvailable = economizingAvailable;
    }
    
    public void addOaoEquip(short addr, OAOProfileConfiguration config, String floorRef, String roomRef) {
        oaoEquip = new OAOEquip(getProfileType(), addr);
        oaoEquip.createEntities(config, floorRef, roomRef);
    }
    
    public void addOaoEquip(short addr) {
        oaoEquip = new OAOEquip(getProfileType(), addr);
        oaoEquip.init();
        oaoEquip.update((OAOProfileConfiguration)getProfileConfiguration(addr));
    }
    
    public void updateOaoEquip(OAOProfileConfiguration config) {
        oaoEquip.update(config);
        oaoEquip.init();
    }
    
    public ProfileType getProfileType()
    {
        return ProfileType.OAO;
    }
    
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return oaoEquip.getProfileConfiguration();
    }
    
    public int getNodeAddress() {
        return oaoEquip.nodeAddr;
    }
    
    public String getEquipRef() {
        return oaoEquip.equipRef;
    }
    
    public void doOAO() {

        systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        double outsideDamperMinOpen = oaoEquip.getConfigNumVal("oao and not purge and not enhanced and outside and damper and min and open");
        doEpidemicControl();
        doEconomizing();
        doDcvControl(outsideDamperMinOpen);
        
        outsideAirLoopOutput = Math.max(economizingLoopOutput, outsideAirCalculatedMinDamper);
        
        double outsideDamperMatTarget = TunerUtil.readTunerValByQuery("oao and outside and damper and mat and target",oaoEquip.equipRef);
        double outsideDamperMatMin = TunerUtil.readTunerValByQuery("oao and outside and damper and mat and min",oaoEquip.equipRef);

        double returnDamperMinOpen = oaoEquip.getConfigNumVal("oao and return and damper and min and open");
        double matTemp  = oaoEquip.getHisVal("mixed and air and temp and sensor");
    
        Log.d(L.TAG_CCU_OAO,"outsideAirLoopOutput "+outsideAirLoopOutput+" outsideDamperMatTarget "+outsideDamperMatTarget+" outsideDamperMatMin "+outsideDamperMatMin
                            +" matTemp "+matTemp);
        if (outsideAirLoopOutput > outsideDamperMinOpen) {
            if (matTemp < outsideDamperMatTarget && matTemp > outsideDamperMatMin) {
                outsideAirFinalLoopOutput = outsideAirLoopOutput - outsideAirLoopOutput * ((outsideDamperMatTarget - matTemp) / (outsideDamperMatTarget - outsideDamperMatMin));
            }
            else {
                outsideAirFinalLoopOutput = (matTemp <= outsideDamperMatMin) ? outsideDamperMinOpen : outsideAirLoopOutput;
            }
        } else {
            outsideAirFinalLoopOutput = outsideDamperMinOpen;
        }
        outsideAirFinalLoopOutput = Math.max(outsideAirFinalLoopOutput , outsideDamperMinOpen);
        outsideAirFinalLoopOutput = Math.min(outsideAirFinalLoopOutput , 100);
        
        returnAirFinalOutput = Math.max(returnDamperMinOpen ,(100 - outsideAirFinalLoopOutput));
    
        Log.d(L.TAG_CCU_OAO," economizingLoopOutput "+economizingLoopOutput+" outsideAirCalculatedMinDamper "+outsideAirCalculatedMinDamper
                                            +" outsideAirFinalLoopOutput "+outsideAirFinalLoopOutput+","+returnAirFinalOutput);
    
        oaoEquip.setHisVal("outside and air and final and loop", outsideAirFinalLoopOutput);
        oaoEquip.setHisVal("outside and air and damper and cmd", outsideAirFinalLoopOutput);
        oaoEquip.setHisVal("return and air and damper and cmd", returnAirFinalOutput);
        
        double exhaustFanHysteresis = oaoEquip.getConfigNumVal("config and exhaust and fan and hysteresis");
        double exhaustFanStage1Threshold = oaoEquip.getConfigNumVal("config and exhaust and fan and stage1 and threshold");
        double exhaustFanStage2Threshold = oaoEquip.getConfigNumVal("config and exhaust and fan and stage2 and threshold");
        if (outsideAirFinalLoopOutput > exhaustFanStage1Threshold) {
            oaoEquip.setHisVal("cmd and exhaust and fan and stage1",1);
        } else if (outsideAirFinalLoopOutput < (exhaustFanStage1Threshold - exhaustFanHysteresis)){
            oaoEquip.setHisVal("cmd and exhaust and fan and stage1",0);
        }
    
        if (outsideAirFinalLoopOutput > exhaustFanStage2Threshold) {
            oaoEquip.setHisVal("cmd and exhaust and fan and stage2",1);
        } else if (outsideAirFinalLoopOutput < (exhaustFanStage2Threshold - exhaustFanHysteresis)) {
            oaoEquip.setHisVal("cmd and exhaust and fan and stage2",0);
        }
    }
    public void doEpidemicControl(){
        epidemicState = EpidemicState.OFF;
        if(systemMode != SystemMode.OFF) {
            Occupancy systemOccupancy = ScheduleProcessJob.getSystemOccupancy();
            switch (systemOccupancy) {
                case UNOCCUPIED:
                    boolean isSmartPrePurge = TunerUtil.readSystemUserIntentVal("prePurge and enabled ") > 0;
                    boolean isSmartPostPurge = TunerUtil.readSystemUserIntentVal("postPurge and enabled ") > 0;
                    if (isSmartPrePurge) {
                        handleSmartPrePurgeControl();
                    }
                    if (isSmartPostPurge) {
                        handleSmartPostPurgeControl();
                    }
                    break;
                case OCCUPIED:
                case FORCEDOCCUPIED:
                case OCCUPANCYSENSING:
                    boolean isEnhancedVentilation = TunerUtil.readSystemUserIntentVal("enhanced and ventilation and enabled ") > 0;
                    if (isEnhancedVentilation)
                        handleEnhancedVentilationControl();
                    break;
            }
        }
        if(epidemicState == EpidemicState.OFF) {
            double prevEpidemicStateValue = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and mode and state");
            if(prevEpidemicStateValue != EpidemicState.OFF.ordinal())
                CCUHsApi.getInstance().writeHisValByQuery("point and sp and system and epidemic and mode and state", (double)EpidemicState.OFF.ordinal());
        }
    }
    public void doEconomizing() {
        
        double externalTemp , externalHumidity;
        try {
            if (Globals.getInstance().isWeatherTest()) {
                externalTemp = Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                      .getInt("outside_temp", 0);
                externalHumidity = Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                      .getInt("outside_humidity", 0);
            } else {
                externalTemp = CCUHsApi.getInstance().readHisValByQuery("system and outside and temp");
                externalHumidity = CCUHsApi.getInstance().readHisValByQuery("system and outside and humidity");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(L.TAG_CCU_OAO," Failed to read external Temp or Humidity , Disable Economizing");
            //setEconomizingAvailable(false);
            //resetOAOParams();
            //return;
        }
        oaoEquip.setHisVal("outsideWeather and air and temp", externalTemp);
        oaoEquip.setHisVal("outsideWeather and air and humidity", externalHumidity);
        
        
        double economizingToMainCoolingLoopMap = TunerUtil.readTunerValByQuery("oao and economizing and main and cooling and loop and map", oaoEquip.equipRef);
        
        if (canDoEconomizing(externalTemp, externalHumidity)) {
            
            setEconomizingAvailable(true);
            if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_ANALOG_RTU ||
                                L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_ANALOG_RTU ||
                                L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_IE_RTU
            ) {
                economizingLoopOutput = Math.min(L.ccu().systemProfile.getCoolingLoopOp() * 100 / economizingToMainCoolingLoopMap ,100);
            }else if (L.ccu().systemProfile instanceof VavStagedRtu) {
                //VavStagedProfile
                VavStagedRtu profile = (VavStagedRtu) L.ccu().systemProfile;
                economizingLoopOutput = Math.min(profile.getCoolingLoopOp() * (profile.coolingStages + 1)  , 100);
            }else if (L.ccu().systemProfile instanceof DabStagedRtu) {
                //DabStagedProfile
                DabStagedRtu profile = (DabStagedRtu) L.ccu().systemProfile;
                economizingLoopOutput = Math.min(profile.getCoolingLoopOp() * (profile.coolingStages + 1), 100);
            }
        } else {
            setEconomizingAvailable(false);
            economizingLoopOutput = 0;
        }
        oaoEquip.setHisVal("economizing and available", economizingAvailable?1:0);
        oaoEquip.setHisVal("economizing and loop and output", economizingLoopOutput);
    }
    
    /**
     * re-initialize OAO specific his points.
     */
    private void resetOAOParams() {
        oaoEquip.setHisVal("outsideWeather and air and temp", 0);
        oaoEquip.setHisVal("outsideWeather and air and humidity", 0);
        oaoEquip.setHisVal("inside and enthalpy", 0);
        oaoEquip.setHisVal("outside and enthalpy", 0);
        oaoEquip.setHisVal("economizing and available", 0);
        oaoEquip.setHisVal("economizing and loop and output", 0);
    }
    
    /**
     * Evaluates outside temperature and humidity to determine if free-cooling can be used.
     * @param externalTemp
     * @param externalHumidity
     * @return
     */
    private boolean canDoEconomizing(double externalTemp, double externalHumidity) {
    
        double economizingMinTemp = TunerUtil.readTunerValByQuery("oao and economizing and min and " +
                                                                  "temp",oaoEquip.equipRef);
        
        if (L.ccu().systemProfile.getSystemController().getSystemState() != SystemController.State.COOLING) {
            return false;
        }
    
        if (isDryBulbTemperatureGoodForEconomizing(externalTemp, externalHumidity, economizingMinTemp)) {
            CcuLog.d(L.TAG_CCU_OAO, "Do economizing based on drybulb temperature");
            return true;
        }
        
        if (!isOutsideWeatherSuitableForEconomizing(externalTemp, externalHumidity, economizingMinTemp)) {
            return false;
        }
        
        //If outside enthalpy is lower, do economizing.
        if (isInsideEnthalpyGreaterThanOutsideEnthalpy(externalTemp, externalHumidity)) {
            CcuLog.d(L.TAG_CCU_OAO, "Do economizing based on enthalpy");
            return true;
        }
        
        return false;
    }
    
    /**
     *  Checks the external temp against drybulb threshold tuner.
     * @param externalTemp
     * @param externalHumidity
     * @return
     */
    private boolean isDryBulbTemperatureGoodForEconomizing(double externalTemp, double externalHumidity, double economizingMinTemp) {
        double dryBulbTemperatureThreshold = TunerUtil.readTunerValByQuery("oao and economizing and dry and bulb and " +
                                                                           "threshold", oaoEquip.equipRef);
        double outsideAirTemp = externalTemp;
    
        /* Both the weather parameters may be zero when CCU cant reach remote weather service
         * Then fallback to Local Outside Air Temp.
         */
        if (externalHumidity == 0 && externalTemp == 0) {
            outsideAirTemp  = oaoEquip.getHisVal("outside and air and temp");
        }
        
        if (outsideAirTemp > economizingMinTemp) {
            return outsideAirTemp < dryBulbTemperatureThreshold;
        }
        return false;
    }
    
    /**
     * Checks if the outside whether is suitable for economizing.
     * @param externalTemp
     * @param externalHumidity
     * @return
     */
    private boolean isOutsideWeatherSuitableForEconomizing(double externalTemp, double externalHumidity,
                                                           double economizingMinTemp) {
        
        double economizingMaxTemp = TunerUtil.readTunerValByQuery("oao and economizing and max and " +
                                                                  "temp",oaoEquip.equipRef);
        double economizingMinHumidity = TunerUtil.readTunerValByQuery("oao and economizing and min and " +
                                                                      "humidity",oaoEquip.equipRef);
        double economizingMaxHumidity = TunerUtil.readTunerValByQuery("oao and economizing and max and " +
                                                                      "humidity",oaoEquip.equipRef);
        
        if (externalTemp > economizingMinTemp
            && externalTemp < economizingMaxTemp
            && externalHumidity > economizingMinHumidity
            && externalHumidity < economizingMaxHumidity) {
            return true;
        }
        CcuLog.d(L.TAG_CCU_OAO, "Outside air not suitable for economizing Temp : "+externalTemp
                                +" Humidity : "+externalHumidity);
        return false;
    }
    
    /**
     * Compare the inside vs outside enthalpy.
     * @param externalTemp
     * @param externalHumidity
     * @return
     */
    private boolean isInsideEnthalpyGreaterThanOutsideEnthalpy(double externalTemp, double externalHumidity) {
        double insideEnthalpy = getAirEnthalpy(L.ccu().systemProfile.getSystemController().getAverageSystemTemperature(),
                                               L.ccu().systemProfile.getSystemController().getAverageSystemHumidity());
    
    
        double enthalpyDuctCompensationOffset = TunerUtil.readTunerValByQuery("oao and enthalpy and duct and compensation and offset",oaoEquip.equipRef);
    
        double outsideEnthalpy = getAirEnthalpy(externalTemp, externalHumidity);
    
        Log.d(L.TAG_CCU_OAO," insideEnthalpy "+insideEnthalpy+", outsideEnthalpy "+outsideEnthalpy);
    
        oaoEquip.setHisVal("inside and enthalpy", insideEnthalpy);
        oaoEquip.setHisVal("outside and enthalpy", outsideEnthalpy);
        
        return insideEnthalpy > outsideEnthalpy + enthalpyDuctCompensationOffset;
    
    }
    
    public void doDcvControl(double outsideDamperMinOpen) {
        double dcvCalculatedMinDamper = 0;
        boolean usePerRoomCO2Sensing = oaoEquip.getConfigNumVal("config and oao and co2 and sensing") > 0? true : false;
        if (usePerRoomCO2Sensing)
        {
            dcvCalculatedMinDamper = L.ccu().systemProfile.getCo2LoopOp();
            Log.d(L.TAG_CCU_OAO,"usePerRoomCO2Sensing dcvCalculatedMinDamper "+dcvCalculatedMinDamper);
            
        } else {
            double returnAirCO2  = oaoEquip.getHisVal("return and air and co2 and sensor");
            double co2Threshold = oaoEquip.getConfigNumVal("co2 and threshold");
            double co2DamperOpeningRate = TunerUtil.readTunerValByQuery("oao and co2 and damper and opening and rate",oaoEquip.equipRef);
            
            if (returnAirCO2 > co2Threshold) {
                dcvCalculatedMinDamper = (returnAirCO2 - co2Threshold)/co2DamperOpeningRate;
            }
            Log.d(L.TAG_CCU_OAO," dcvCalculatedMinDamper "+dcvCalculatedMinDamper+" returnAirCO2 "+returnAirCO2+" co2Threshold "+co2Threshold);
        }
        oaoEquip.setHisVal("co2 and weighted and average", L.ccu().systemProfile.getWeightedAverageCO2());
        Occupancy systemOccupancy = ScheduleProcessJob.getSystemOccupancy();
        switch (systemOccupancy) {
            case OCCUPIED:
            case FORCEDOCCUPIED:
                if(systemMode != SystemMode.OFF) {
                    outsideDamperMinOpen = epidemicState != EpidemicState.OFF ? outsideAirCalculatedMinDamper : outsideDamperMinOpen;
                    outsideAirCalculatedMinDamper = Math.min(outsideDamperMinOpen + dcvCalculatedMinDamper, 100);
                }else
                    outsideAirCalculatedMinDamper = outsideDamperMinOpen;
                break;
            case PRECONDITIONING:
            case VACATION:
                outsideAirCalculatedMinDamper = outsideDamperMinOpen;
                break;
            case UNOCCUPIED:
                if(epidemicState == EpidemicState.OFF)
                    outsideAirCalculatedMinDamper = outsideDamperMinOpen;
                break;
        }
        oaoEquip.setHisVal("outside and air and calculated and min and damper", outsideAirCalculatedMinDamper);
    }
    
    public static double getAirEnthalpy(double averageTemp, double averageHumidity) {
	/*	10 REM ENTHALPY CALCULATION
		20 REM Assumes standard atmospheric pressure (14.7 psi), see line 110
		30 REM Dry-bulb temperature in degrees F (TEMP)
		40 REM Relative humidity in percentage (RH)
		50 REM Enthalpy in BTU/LB OF DRY AIR (H)
		60 T = TEMP + 459.67
		70 N = LN( T ) : REM Natural Logrithm
		80 L = -10440.4 / T - 11.29465 - 0.02702235 * T + 1.289036E-005 * T ^ 2 - 2.478068E-009 * T ^ 3 + 6.545967 * N
		90 S = LN-1( L ) : REM Inverse Natural Logrithm
		100 P = RH / 100 * S
		110 W = 0.62198 * P / ( 14.7 - P )
		120 H = 0.24 * TEMP + W * ( 1061 + 0.444 * TEMP )
	*/
	/*  A = .007468 * DB^2 - .4344 * DB + 11.1769

		B = .2372 * DB + .1230

		Enth = A * RH + B
    */
        double A = 0.007468 * Math.pow(averageTemp,2) - 0.4344 * averageTemp + 11.1769;
        double B = 0.2372 * averageTemp + 0.1230;
        double H = A * averageHumidity + B;
    
        Log.d(L.TAG_CCU_OAO, "averageTemp "+averageTemp+" averageHumidity "+averageHumidity+" Enthalpy "+H);
        return CCUUtils.roundToTwoDecimal(H);
    }

    private void handleSmartPrePurgeControl(){
        double smartPrePurgeRunTime = TunerUtil.readTunerValByQuery("prePurge and runtime and oao and system", oaoEquip.equipRef);
        double smartPrePurgeOccupiedTimeOffset = TunerUtil.readTunerValByQuery("prePurge and occupied and time and offset and oao and system", oaoEquip.equipRef);
        Occupied occuSchedule = ScheduleProcessJob.getNextOccupiedTimeInMillis();
        int minutesToOccupancy = occuSchedule != null ? (int)occuSchedule.getMillisecondsUntilNextChange()/60000 : -1;
        if((minutesToOccupancy != -1) && (smartPrePurgeOccupiedTimeOffset >= minutesToOccupancy) && (minutesToOccupancy >= (smartPrePurgeOccupiedTimeOffset - smartPrePurgeRunTime))) {
            outsideAirCalculatedMinDamper = oaoEquip.getConfigNumVal("userIntent and purge and outside and damper and pos and min and open");
            CCUHsApi.getInstance().writeHisValByQuery("point and sp and system and epidemic and mode and state", (double)EpidemicState.PREPURGE.ordinal());
            epidemicState = EpidemicState.PREPURGE;
        }
    }
    private void handleSmartPostPurgeControl(){
        double smartPostPurgeRunTime = TunerUtil.readTunerValByQuery("postPurge and runtime and oao and system", oaoEquip.equipRef);
        double smartPostPurgeOccupiedTimeOffset = TunerUtil.readTunerValByQuery("postPurge and occupied and time and offset and oao and system", oaoEquip.equipRef);
        Occupied occuSchedule = ScheduleProcessJob.getPrevOccupiedTimeInMillis();
        if(occuSchedule != null)
            Log.d(L.TAG_CCU_OAO, "System Unoccupied, check postpurge22 = "+occuSchedule.getMillisecondsUntilPrevChange()+","+(occuSchedule.getMillisecondsUntilPrevChange())/60000+","+smartPostPurgeOccupiedTimeOffset+","+smartPostPurgeRunTime);
        int minutesInUnoccupied = occuSchedule != null ? (int)(occuSchedule.getMillisecondsUntilPrevChange()/60000) : -1;
        if( (epidemicState == EpidemicState.OFF) && (minutesInUnoccupied != -1) && (minutesInUnoccupied  >= smartPostPurgeOccupiedTimeOffset) && (minutesInUnoccupied <= (smartPostPurgeRunTime + smartPostPurgeOccupiedTimeOffset))) {
            outsideAirCalculatedMinDamper = oaoEquip.getConfigNumVal("userIntent and purge and outside and damper and pos and min and open");
            CCUHsApi.getInstance().writeHisValByQuery("point and sp and system and epidemic and mode and state", (double)EpidemicState.POSTPURGE.ordinal());
            epidemicState = EpidemicState.POSTPURGE;
        }
    }
    private void handleEnhancedVentilationControl(){
        epidemicState = EpidemicState.ENHANCED_VENTILATION;
        outsideAirCalculatedMinDamper = CCUHsApi.getInstance().readDefaultVal("enhanced and ventilation and outside and damper and pos and min and open and equipRef ==\""+oaoEquip.equipRef+"\"");
        CCUHsApi.getInstance().writeHisValByQuery("point and sp and system and epidemic and mode and state", (double)EpidemicState.ENHANCED_VENTILATION.ordinal());
        Log.d(L.TAG_CCU_OAO, "System occupied, check enhanced ventilation = "+outsideAirCalculatedMinDamper+","+epidemicState.name());
    }
}
