package a75f.io.logic.bo.building.oao;

import android.util.Log;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
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
    
    OAOEquip oaoEquip;
    
    public boolean isEconomizingAvailable()
    {
        return economizingAvailable;
    }
    public void setEconomizingAvailable(boolean economizingAvailable)
    {
        economizingAvailable = economizingAvailable;
    }
    
    public void addOaoEquip(short addr, OAOProfileConfiguration config, String floorRef, String roomRef) {
        oaoEquip = new OAOEquip(getProfileType(), addr);
        oaoEquip.createEntities(config, floorRef, roomRef);
        oaoEquip.init();
    }
    
    public void addOaoEquip(short addr) {
        oaoEquip = new OAOEquip(getProfileType(), addr);
        oaoEquip.init();
    }
    
    public void updateOaoEquip(OAOProfileConfiguration config) {
        oaoEquip.update(config);
    }
    
    public ProfileType getProfileType()
    {
        return ProfileType.OAO;
    }
    
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return oaoEquip.getProfileConfiguration();
    }
    
    public void doOAO() {
        
        doEconomizing();
        doDcvControl();
    
        outsideAirLoopOutput = Math.max(economizingLoopOutput, outsideAirCalculatedMinDamper);
        
        double outsideDamperMatTarget = TunerUtil.readTunerValByQuery("oao and outside and damper and mat and target",oaoEquip.equipRef);
        double outsideDamperMatMin = TunerUtil.readTunerValByQuery("oao and outside and damper and mat and min",oaoEquip.equipRef);
    
        double matTemp  = oaoEquip.getHisVal("mixed and air and temp and sensor");
    
        outsideAirFinalLoopOutput = outsideAirLoopOutput -
                                        outsideAirLoopOutput * ((outsideDamperMatTarget - matTemp)/(outsideDamperMatTarget - outsideDamperMatMin));
    
        returnAirFinalOutput = 100 - outsideAirFinalLoopOutput;
    
        oaoEquip.setConfigNumVal("outside and air and damper and cmd", outsideAirFinalLoopOutput);
        oaoEquip.setConfigNumVal("return and air and damper and cmd", returnAirFinalOutput);
        
        if (outsideAirFinalLoopOutput > oaoEquip.getConfigNumVal("config and exhaust and fan and stage1 and threshold")) {
            oaoEquip.setConfigNumVal("cmd and exhaust and fan and stage1",1);
        } else {
            oaoEquip.setConfigNumVal("cmd and exhaust and fan and stage1",0);
        }
    
        if (outsideAirFinalLoopOutput > oaoEquip.getConfigNumVal("config and exhaust and fan and stage2 and threshold")) {
            oaoEquip.setConfigNumVal("cmd and exhaust and fan and stage2",1);
        } else {
            oaoEquip.setConfigNumVal("cmd and exhaust and fan and stage2",0);
        }
    }
    
    public void doEconomizing() {
        double insideEnthalpy = getAirEnthalpy(L.ccu().systemProfile.getSystemController().getAverageSystemTemperature(),
                                    L.ccu().systemProfile.getSystemController().getAverageSystemHumidity());
    
    
        double enthalpyDuctCompensationOffset = TunerUtil.readTunerValByQuery("oao and enthalpy and duct and compensation and offset",oaoEquip.equipRef);
    
        double outsideEnthalpy = getAirEnthalpy(71, 94); //TODO-
    
        double economizingToMainCoolingLoopMap = TunerUtil.readTunerValByQuery("oao and economizing and main and cooling and loop and map", oaoEquip.equipRef);
        if (L.ccu().systemProfile.getSystemController().getSystemState() == SystemController.State.COOLING
                                && (insideEnthalpy > outsideEnthalpy + enthalpyDuctCompensationOffset)) {
            economizingAvailable = true;
            
            if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_ANALOG_RTU ||
                                L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_ANALOG_RTU) {
                economizingLoopOutput = Math.min(L.ccu().systemProfile.getCoolingLoopOp() * 100 / economizingToMainCoolingLoopMap ,100);
            }else if (L.ccu().systemProfile instanceof VavStagedRtu) {
                //VavStagedProfile
                VavStagedRtu profile = (VavStagedRtu) L.ccu().systemProfile;
                economizingLoopOutput = Math.min(profile.getCoolingLoopOp() * 100 /(profile.coolingStages + 1), 100);
            }else if (L.ccu().systemProfile instanceof DabStagedRtu) {
                //DabStagedProfile
                DabStagedRtu profile = (DabStagedRtu) L.ccu().systemProfile;
                economizingLoopOutput = Math.min(profile.getCoolingLoopOp() * 100 /(profile.coolingStages + 1), 100);
            }
        } else {
            economizingAvailable = false;
        }
    }
    
    public void doDcvControl() {
        double dcvCalculatedMinDamper = 0;
        boolean usePerRoomCO2Sensing = oaoEquip.getConfigNumVal("room and co2 and sensing") > 0? true : false;
        if (usePerRoomCO2Sensing)
        {
            dcvCalculatedMinDamper = L.ccu().systemProfile.getCo2LoopOp();
            
        } else {
            double returnAirCO2  = oaoEquip.getHisVal("return and air and co2 and sensor");
            double co2Threshold = oaoEquip.getConfigNumVal("co2 and threshold");
            double co2DamperOpeningRate = TunerUtil.readTunerValByQuery("oao and co2 and damper and opening and rate",oaoEquip.equipRef);
            
            if (returnAirCO2 > co2Threshold) {
                dcvCalculatedMinDamper = (returnAirCO2 - co2Threshold)/co2DamperOpeningRate;
            }
        }
    
        double outsideDamperMinOpen = TunerUtil.readTunerValByQuery("oao and outside and damper and min and open");
        outsideAirCalculatedMinDamper = outsideDamperMinOpen + dcvCalculatedMinDamper;
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
        Log.d("CCU_OAO", String.format("Enthalpy %f %f %f", averageTemp, averageHumidity, H));
        return H;
    }
    
}
