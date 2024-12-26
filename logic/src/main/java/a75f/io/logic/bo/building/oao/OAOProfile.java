package a75f.io.logic.bo.building.oao;

import static a75f.io.domain.api.DomainName.systemEnhancedVentilationEnable;
import static a75f.io.domain.api.DomainName.systemPostPurgeEnable;
import static a75f.io.domain.api.DomainName.systemPrePurgeEnable;
import static a75f.io.logic.bo.building.system.AdvancedAhuPointMappingsKt.getCMRelayAssociationMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Occupied;
import a75f.io.domain.OAOEquip;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.api.Point;
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip;
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu;
import a75f.io.logic.bo.building.system.dab.DabAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.dab.DabExternalAhu;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd;
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu;
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.vav.VavExternalAhu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.tuners.TunerUtil;

/*
*  OAO Combines both System profile and Zone Profile behaviours.
*  It controls system components like OA damper , but mapped to smart node device like a zone profile.
* */
public class OAOProfile
{
    
    public static boolean economizingAvailable = false;

    private boolean dcvAvailable;
    private boolean matThrottle;
    
    double economizingLoopOutput;
    double outsideAirCalculatedMinDamper;
    double outsideAirLoopOutput;
    double outsideAirFinalLoopOutput;
    double returnAirFinalOutput;
    EpidemicState epidemicState = EpidemicState.OFF;
    SystemMode systemMode;
    OAOEquip oaoEquip;
    public int nodeAddr;
    ProfileType profileType;
    String equipRef = null;
    
    public boolean isEconomizingAvailable()
    {
        return economizingAvailable;
    }
    public void setEconomizingAvailable(boolean economizingAvailable)
    {
        OAOProfile.economizingAvailable = economizingAvailable;
    }

    public boolean isDcvAvailable() {
        return dcvAvailable;
    }

    public void setDcvAvailable(boolean dcvAvailable) {
        this.dcvAvailable = dcvAvailable;
    }

    public boolean isMatThrottle() {
        return matThrottle;
    }

    public void setMatThrottle(boolean matThrottle) {
        this.matThrottle = matThrottle;
    }

    public void addOAOEquip(String equipRef, Short addr, ProfileType profileType) {
        oaoEquip = new OAOEquip(equipRef);
        this.nodeAddr = addr;
        this.equipRef =equipRef;
        this.profileType = profileType;
    }


    public ProfileType getProfileType()
    {
        return ProfileType.OAO;
    }
    
    public int getNodeAddress() {
        return nodeAddr;
    }
    
    public String getEquipRef() {
        return this.equipRef;
    }
    
    public void doOAO() {
        long start = System.currentTimeMillis();
        SystemProfile systemProfile = L.ccu().systemProfile;
        systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];

        double outsideDamperMinOpen = getEffectiveOutsideDamperMinOpen(systemProfile);
        doEpidemicControl(systemProfile);
        doEconomizing(systemProfile);
        doDcvControl(outsideDamperMinOpen, systemProfile);
        
        outsideAirLoopOutput = Math.max(economizingLoopOutput, outsideAirCalculatedMinDamper);
        
        double outsideDamperMatTarget = oaoEquip.getOutsideDamperMixedAirTarget().readPriorityVal();
        double outsideDamperMatMin = oaoEquip.getOutsideDamperMixedAirMinimum().readPriorityVal();

        double returnDamperMinOpen = oaoEquip.getReturnDamperMinOpen().readDefaultVal();
        double matTemp  = oaoEquip.getMixedAirTemperature().readHisVal();
    
        CcuLog.d(L.TAG_CCU_OAO,"outsideAirLoopOutput "+outsideAirLoopOutput+" outsideDamperMatTarget "+outsideDamperMatTarget+" outsideDamperMatMin "+outsideDamperMatMin
                            +" matTemp "+matTemp);
        setMatThrottle(false);
        if (outsideAirLoopOutput > outsideDamperMinOpen) {
            if (matTemp < outsideDamperMatTarget && matTemp > outsideDamperMatMin) {
                outsideAirFinalLoopOutput = outsideAirLoopOutput - outsideAirLoopOutput * ((outsideDamperMatTarget - matTemp) / (outsideDamperMatTarget - outsideDamperMatMin));
            }
            else {
                outsideAirFinalLoopOutput = (matTemp <= outsideDamperMatMin) ? 0 : outsideAirLoopOutput;
            }
            if (matTemp < outsideDamperMatTarget) {
                setMatThrottle(true);
            }
        } else {
            outsideAirFinalLoopOutput = outsideDamperMinOpen;
        }

        if (matThrottle) {
            outsideAirFinalLoopOutput = Math.max(outsideAirFinalLoopOutput , 0);
        } else {
            outsideAirFinalLoopOutput = Math.max(outsideAirFinalLoopOutput , outsideDamperMinOpen);
        }

        outsideAirFinalLoopOutput = Math.min(outsideAirFinalLoopOutput , 100);
        
        returnAirFinalOutput = Math.max(returnDamperMinOpen ,(100 - outsideAirFinalLoopOutput));
    
        CcuLog.d(L.TAG_CCU_OAO," economizingLoopOutput "+economizingLoopOutput+" outsideAirCalculatedMinDamper "+outsideAirCalculatedMinDamper
                                            +" outsideAirFinalLoopOutput "+outsideAirFinalLoopOutput+","+returnAirFinalOutput);

        oaoEquip.getOutsideAirFinalLoopOutput().writeHisVal(outsideAirFinalLoopOutput);
        oaoEquip.getOutsideDamperCmd().writeHisVal(outsideAirFinalLoopOutput);
        oaoEquip.getReturnDamperCmd().writeHisVal(returnAirFinalOutput);

        double exhaustFanHysteresis = oaoEquip.getExhaustFanHysteresis().readDefaultVal();
        double exhaustFanStage1Threshold = oaoEquip.getExhaustFanStage1Threshold().readDefaultVal();
        double exhaustFanStage2Threshold = oaoEquip.getExhaustFanStage2Threshold().readDefaultVal();

        CcuLog.d(L.TAG_CCU_OAO," exhaustFanHysteresis "+exhaustFanHysteresis+" exhaustFanStage1Threshold "+exhaustFanStage1Threshold
                            +" exhaustFanStage2Threshold "+exhaustFanStage2Threshold+" outsideAirFinalLoopOutput "+outsideAirFinalLoopOutput);

        if (outsideAirFinalLoopOutput > exhaustFanStage1Threshold) {
              oaoEquip.getExhaustFanStage1().writeHisVal(1);
        } else if (outsideAirFinalLoopOutput < (exhaustFanStage1Threshold - exhaustFanHysteresis)){
            oaoEquip.getExhaustFanStage1().writeHisVal(0);
        }
    
        if (outsideAirFinalLoopOutput > exhaustFanStage2Threshold) {
            oaoEquip.getExhaustFanStage2().writeHisVal(1);
        } else if (outsideAirFinalLoopOutput < (exhaustFanStage2Threshold - exhaustFanHysteresis)) {
            oaoEquip.getExhaustFanStage2().writeHisVal(0);
        }
        oaoEquip.getMatThrottle().writeHisVal(isMatThrottle() ? 1 : 0);
        long end = System.currentTimeMillis();
        CcuLog.d(L.TAG_CCU_OAO,"OAO Algo Time taken in milli seconds: "+(end-start));
    }

    /**
     * Enabled for profiles with domain name on points
     */
    private boolean isDMMigrated(SystemProfile systemProfile){
        return (systemProfile instanceof DabExternalAhu
                || (systemProfile instanceof DabStagedRtu && !(systemProfile instanceof DabAdvancedHybridRtu))
                || systemProfile instanceof DabStagedRtuWithVfd
                || systemProfile instanceof DabFullyModulatingRtu
                || systemProfile instanceof VavExternalAhu
                || (systemProfile instanceof VavStagedRtu && !(systemProfile instanceof VavAdvancedHybridRtu))
                || systemProfile instanceof VavFullyModulatingRtu
                || systemProfile instanceof VavAdvancedAhu
                || systemProfile instanceof DabAdvancedAhu);
    }
    public void doEpidemicControl(SystemProfile systemProfile){
        epidemicState = EpidemicState.OFF;
        if(systemMode != SystemMode.OFF) {
            Occupancy systemOccupancy = ScheduleManager.getInstance().getSystemOccupancy();
            switch (systemOccupancy) {
                case UNOCCUPIED:
                    boolean isSmartPrePurge;
                    boolean isSmartPostPurge;
                    if (isDMMigrated(systemProfile)) {
                        isSmartPrePurge = TunerUtil.readSystemUserIntentVal("domainName == \""+systemPrePurgeEnable+"\"") > 0;
                        isSmartPostPurge = TunerUtil.readSystemUserIntentVal("domainName == \""+systemPostPurgeEnable+"\"") > 0;
                    } else {
                        isSmartPrePurge = TunerUtil.readSystemUserIntentVal("prePurge and enabled ") > 0;
                        isSmartPostPurge = TunerUtil.readSystemUserIntentVal("postPurge and enabled ") > 0;
                    }
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
                    boolean isEnhancedVentilation;
                    if (isDMMigrated(systemProfile))
                        isEnhancedVentilation = TunerUtil.readSystemUserIntentVal("domainName == \""+systemEnhancedVentilationEnable+"\"") > 0;
                    else
                        isEnhancedVentilation = TunerUtil.readSystemUserIntentVal("enhanced and ventilation and enabled ") > 0;

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

    private double getEffectiveOutsideDamperMinOpen(SystemProfile systemProfile) {


        double outsideDamperMinOpenFromFan = 0.0;
        if (systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_STAGED_RTU ||
                systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_STAGED_VFD_RTU ||
                systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_HYBRID_RTU) {

            DabStagedRtu dabStagedProfile = (DabStagedRtu)systemProfile;
            if (dabStagedProfile.isStageEnabled(Stage.FAN_3) || dabStagedProfile.isStageEnabled(Stage.FAN_4) || dabStagedProfile.isStageEnabled(Stage.FAN_5)) {
                // 3+ Stages mapped: Stage 1 = LOW, Stage 2 = MEDIUM, Stage 3+ = HIGH
                if (dabStagedProfile.getStageStatus(Stage.FAN_3) > 0.0 || dabStagedProfile.getStageStatus(Stage.FAN_4) > 0.0 || dabStagedProfile.getStageStatus(Stage.FAN_5) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                } else if (dabStagedProfile.getStageStatus(Stage.FAN_2) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanMedium().readDefaultVal();
                } else if (dabStagedProfile.getStageStatus(Stage.FAN_1) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanLow().readDefaultVal();
                }
            } else if (dabStagedProfile.isStageEnabled(Stage.FAN_2)) {
                // 2 stages mapped: Stage 2 = HIGH, Stage 1 = MEDIUM
                if (dabStagedProfile.getStageStatus(Stage.FAN_2) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                } else if (dabStagedProfile.getStageStatus(Stage.FAN_1) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanMedium().readDefaultVal();
                }
            } else if (dabStagedProfile.isStageEnabled(Stage.FAN_1)) {
                // 1 stage mapped: Stage 1 = HIGH
                if (dabStagedProfile.getStageStatus(Stage.FAN_1) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                }
            }
        } else if (systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_RTU ||
                systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU ||
                systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_HYBRID_RTU
        ) {

            VavStagedRtu vavStagedProfile = (VavStagedRtu)systemProfile;
            if (vavStagedProfile.isStageEnabled(Stage.FAN_3) || vavStagedProfile.isStageEnabled(Stage.FAN_4) || vavStagedProfile.isStageEnabled(Stage.FAN_5)) {
                // 3+ Stages mapped: Stage 1 = LOW, Stage 2 = MEDIUM, Stage 3+ = HIGH
                if (vavStagedProfile.getStageStatus(Stage.FAN_3) > 0.0 || vavStagedProfile.getStageStatus(Stage.FAN_4) > 0.0 || vavStagedProfile.getStageStatus(Stage.FAN_5) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                } else if (vavStagedProfile.getStageStatus(Stage.FAN_2) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanMedium().readDefaultVal();
                } else if (vavStagedProfile.getStageStatus(Stage.FAN_1) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanLow().readDefaultVal();
                }
            } else if (vavStagedProfile.isStageEnabled(Stage.FAN_2)) {
                // 2 stages mapped: Stage 2 = HIGH, Stage 1 = MEDIUM
                if (vavStagedProfile.getStageStatus(Stage.FAN_2) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                } else if (vavStagedProfile.getStageStatus(Stage.FAN_1) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanMedium().readDefaultVal();
                }
            } else if (vavStagedProfile.isStageEnabled(Stage.FAN_1)) {
                // 1 stage mapped: Stage 1 = HIGH
                if (vavStagedProfile.getStageStatus(Stage.FAN_1) > 0.0) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                }
            }

        } else if (systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_ADVANCED_AHU) {
            DabAdvancedHybridSystemEquip systemEquip = (DabAdvancedHybridSystemEquip) Domain.systemEquip;
            DabAdvancedAhu dabAdvancedAhu = (DabAdvancedAhu) systemProfile;

            Map<Point, Point> cmRelayAssociationMap = getCMRelayAssociationMap(
                    systemEquip.getCmEquip());

            if (dabAdvancedAhu.isStageEnabled(DomainName.loadFanStage3, cmRelayAssociationMap) ||
                    dabAdvancedAhu.isStageEnabled(DomainName.loadFanStage4, cmRelayAssociationMap) ||
                    dabAdvancedAhu.isStageEnabled(DomainName.loadFanStage5, cmRelayAssociationMap)) {
                // 3+ Stages mapped: Stage 1 = LOW, Stage 2 = MEDIUM, Stage 3+ = HIGH
                if (dabAdvancedAhu.isHighFanStagesEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                } else if (dabAdvancedAhu.isMediumFanStageEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanMedium().readDefaultVal();
                } else if (dabAdvancedAhu.isLowFanStageEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanLow().readDefaultVal();
                }
            } else if (dabAdvancedAhu.isStageEnabled(DomainName.loadFanStage2, cmRelayAssociationMap)) {
                // 2 stages mapped: Stage 2 = HIGH, Stage 1 = MEDIUM
                if (dabAdvancedAhu.isMediumFanStageEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                } else if (dabAdvancedAhu.isLowFanStageEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanMedium().readDefaultVal();
                }
            } else if (dabAdvancedAhu.isStageEnabled(DomainName.loadFanStage1, cmRelayAssociationMap)) {
                // 1 stage mapped: Stage 1 = HIGH
                if (dabAdvancedAhu.isLowFanStageEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                }
            }
        } else if (systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_ADVANCED_AHU){
            VavAdvancedHybridSystemEquip systemEquip = (VavAdvancedHybridSystemEquip) Domain.systemEquip;
            VavAdvancedAhu vavAdvancedAhu = (VavAdvancedAhu) systemProfile;

            Map<Point, Point> cmRelayAssociationMap = getCMRelayAssociationMap(
                    systemEquip.getCmEquip());

            if (vavAdvancedAhu.isStageEnabled(DomainName.loadFanStage3, cmRelayAssociationMap) ||
                    vavAdvancedAhu.isStageEnabled(DomainName.loadFanStage4, cmRelayAssociationMap) ||
                    vavAdvancedAhu.isStageEnabled(DomainName.loadFanStage5, cmRelayAssociationMap)) {
                // 3+ Stages mapped: Stage 1 = LOW, Stage 2 = MEDIUM, Stage 3+ = HIGH
                if (vavAdvancedAhu.isHighFanStagesEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                } else if (vavAdvancedAhu.isMediumFanStageEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanMedium().readDefaultVal();
                } else if (vavAdvancedAhu.isLowFanStageEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanLow().readDefaultVal();
                }
            } else if (vavAdvancedAhu.isStageEnabled(DomainName.loadFanStage2, cmRelayAssociationMap)) {
                // 2 stages mapped: Stage 2 = HIGH, Stage 1 = MEDIUM
                if (vavAdvancedAhu.isMediumFanStageEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                } else if (vavAdvancedAhu.isLowFanStageEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanMedium().readDefaultVal();
                }
            } else if (vavAdvancedAhu.isStageEnabled(DomainName.loadFanStage1,cmRelayAssociationMap)) {
                // 1 stage mapped: Stage 1 = HIGH
                if (vavAdvancedAhu.isLowFanStageEnabled()) {
                    outsideDamperMinOpenFromFan = oaoEquip.getOutsideDamperMinOpenDuringFanHigh().readDefaultVal();
                }
            }
        }


        double outsideDamperMinOpenFromConditioning;
        if (systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_HYBRID_RTU) {
            DabAdvancedHybridRtu dabHybridRtu = (DabAdvancedHybridRtu) systemProfile;
            if (dabHybridRtu.isCoolingActive() || dabHybridRtu.isHeatingActive() || dabHybridRtu.isModulatingCoolingActive() || dabHybridRtu.isModulatingHeatingActive() || oaoEquip.getEconomizingLoopOutput().readHisVal() > 0.0) {
                outsideDamperMinOpenFromConditioning = oaoEquip.getOutsideDamperMinOpenDuringConditioning().readDefaultVal();
            } else {
                outsideDamperMinOpenFromConditioning = oaoEquip.getOutsideDamperMinOpenDuringRecirculation().readDefaultVal();
            }
        } else if (systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_HYBRID_RTU) {
            VavAdvancedHybridRtu vavHybridRtu = (VavAdvancedHybridRtu) systemProfile;
            if (vavHybridRtu.isCoolingActive() || vavHybridRtu.isHeatingActive() || vavHybridRtu.isModulatingCoolingActive() || vavHybridRtu.isModulatingHeatingActive() || oaoEquip.getEconomizingLoopOutput().readHisVal() > 0.0) {
                outsideDamperMinOpenFromConditioning = oaoEquip.getOutsideDamperMinOpenDuringConditioning().readDefaultVal();
            } else {
                outsideDamperMinOpenFromConditioning = oaoEquip.getOutsideDamperMinOpenDuringRecirculation().readDefaultVal();
            }
        } else {
            if (systemProfile.isCoolingActive() || systemProfile.isHeatingActive() || oaoEquip.getEconomizingLoopOutput().readHisVal() > 0.0) {
                outsideDamperMinOpenFromConditioning = oaoEquip.getOutsideDamperMinOpenDuringConditioning().readDefaultVal();
            } else {
                outsideDamperMinOpenFromConditioning = oaoEquip.getOutsideDamperMinOpenDuringRecirculation().readDefaultVal();
            }
        }

        Occupancy systemOccupancy = ScheduleManager.getInstance().getSystemOccupancy();
        if ((systemOccupancy.equals(Occupancy.OCCUPIED) || systemOccupancy.equals(Occupancy.FORCEDOCCUPIED)) && systemMode != SystemMode.OFF) {
            double outsideDamperMinOpen = getOutsideDamperMinOpen(outsideDamperMinOpenFromFan,
                    outsideDamperMinOpenFromConditioning, systemProfile.getProfileType());
            CcuLog.d(L.TAG_CCU_OAO, "Occupied mode," +
                    " SystemProfile currently selected "+systemProfile.getProfileType()+
                    "\n outsideDamperMinOpenFromFan "+ outsideDamperMinOpenFromFan +
                    " outsideDamperMinOpenFromConditioning "+ outsideDamperMinOpenFromConditioning +
                    "\n outside damper min open for OAO based on system profile selected: " + outsideDamperMinOpen);
            return outsideDamperMinOpen;
        } else {
            return 0;
        }

    }

    public double getOutsideDamperMinOpen(double outsideDamperMinOpenFromFan,
                                          double outsideDamperMinOpenFromConditioning,
                                          ProfileType systemProfile) {
        switch (systemProfile){
            case SYSTEM_DAB_STAGED_RTU:
            case SYSTEM_VAV_STAGED_RTU:
                return outsideDamperMinOpenFromFan;

            case SYSTEM_DAB_STAGED_VFD_RTU:
            case SYSTEM_VAV_STAGED_VFD_RTU:
            case SYSTEM_VAV_ADVANCED_AHU:
            case SYSTEM_DAB_ADVANCED_AHU:
            case SYSTEM_DAB_HYBRID_RTU:
            case SYSTEM_VAV_HYBRID_RTU:
                return Math.max(outsideDamperMinOpenFromFan, outsideDamperMinOpenFromConditioning);

                /*Below profiles are VAV and DAB fully modulating AHU*/
            case SYSTEM_VAV_ANALOG_RTU:
            case SYSTEM_DAB_ANALOG_RTU:
                return outsideDamperMinOpenFromConditioning;
        }
        CcuLog.e(L.TAG_CCU_OAO, "Invalid system profile type for outside damper min open calculation");
        return 0;
    }

    public void doEconomizing(SystemProfile systemProfile) {

        //TODO: This needs to changed with domainName
        double externalTemp = CCUHsApi.getInstance().readHisValByQuery("system and outside and temp and not lockout");
        double externalHumidity = CCUHsApi.getInstance().readHisValByQuery("system and outside and humidity");
        
        double economizingToMainCoolingLoopMap = oaoEquip.getEconomizingToMainCoolingLoopMap().readPriorityVal();
        
        if (canDoEconomizing(externalTemp, externalHumidity, systemProfile)) {
            
            setEconomizingAvailable(true);
            if (systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_ANALOG_RTU ||
                                systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_ANALOG_RTU ||
                                systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_IE_RTU
            ) {
                economizingLoopOutput = Math.min(systemProfile.getCoolingLoopOp() * 100 / economizingToMainCoolingLoopMap ,100);
            }else if (systemProfile instanceof VavStagedRtu) {
                //VavStagedProfile
                VavStagedRtu profile = (VavStagedRtu) systemProfile;
                economizingLoopOutput = Math.min(profile.getCoolingLoopOp() * (profile.coolingStages + 1)  , 100);
            }else if (systemProfile instanceof DabStagedRtu) {
                //DabStagedProfile
                DabStagedRtu profile = (DabStagedRtu) systemProfile;
                economizingLoopOutput = Math.min(profile.getCoolingLoopOp() * (profile.coolingStages + 1), 100);
            }
        } else {
            setEconomizingAvailable(false);
            economizingLoopOutput = 0;
        }
        oaoEquip.getEconomizingAvailable().writeHisVal(isEconomizingAvailable() ? 1 : 0);
        oaoEquip.getEconomizingLoopOutput().writeHisVal(economizingLoopOutput);
    }
    
    /**
     * Evaluates outside temperature and humidity to determine if free-cooling can be used.
     *
     * @param externalTemp     external temperature
     * @param externalHumidity external humidity
     * @param systemProfile present system profile
     */
    private boolean canDoEconomizing(double externalTemp, double externalHumidity, SystemProfile systemProfile) {
    
        double economizingMinTemp = oaoEquip.getEconomizingMinTemperature().readPriorityVal();
    
        double insideEnthalpy = getAirEnthalpy(systemProfile.getSystemController().getAverageSystemTemperature(),
                                               systemProfile.getSystemController().getAverageSystemHumidity());
        
        double outsideEnthalpy = getAirEnthalpy(externalTemp, externalHumidity);
        oaoEquip.getInsideEnthalpy().writeHisVal(insideEnthalpy);
        oaoEquip.getOutsideEnthalpy().writeHisVal(outsideEnthalpy);
    
    
        CcuLog.d(L.TAG_CCU_OAO," canDoEconomizing externalTemp "+externalTemp+" externalHumidity "+externalHumidity);
        
        if (systemProfile.getSystemController().getSystemState() != SystemController.State.COOLING) {
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
        if (isInsideEnthalpyGreaterThanOutsideEnthalpy(insideEnthalpy, outsideEnthalpy)) {
            CcuLog.d(L.TAG_CCU_OAO, "Do economizing based on enthalpy");
            return true;
        }
        
        return false;
    }
    
    /**
     *  Checks the external temp against drybulb threshold tuner.
     * @param externalTemp external temperature
     * @param externalHumidity external humidity
     * @param economizingMinTemp economizing min temp
     */
    private boolean isDryBulbTemperatureGoodForEconomizing(double externalTemp, double externalHumidity, double economizingMinTemp) {
        double dryBulbTemperatureThreshold = oaoEquip.getEconomizingDryBulbThreshold().readPriorityVal();
        double outsideAirTemp = externalTemp;
    
        /* Both the weather parameters may be zero when CCU cant reach remote weather service
         * Then fallback to Local Outside Air Temp.
         */
        if (externalHumidity == 0 && externalTemp == 0) {
            outsideAirTemp  = oaoEquip.getOutsideTemperature().readHisVal();
        }
        
        if (outsideAirTemp > economizingMinTemp) {
            return outsideAirTemp < dryBulbTemperatureThreshold;
        }
        return false;
    }
    
    /**
     * Checks if the outside whether is suitable for economizing.
     * @param externalTemp external temperature
     * @param externalHumidity external humidity
     * @param economizingMinTemp economizing min temp
     */
    private boolean isOutsideWeatherSuitableForEconomizing(double externalTemp, double externalHumidity,
                                                           double economizingMinTemp) {
        
        double economizingMaxTemp = oaoEquip.getEconomizingMaxTemperature().readPriorityVal();
        double economizingMinHumidity = oaoEquip.getEconomizingMinHumidity().readPriorityVal();
        double economizingMaxHumidity = oaoEquip.getEconomizingMaxHumidity().readPriorityVal();
        
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
     */
    private boolean isInsideEnthalpyGreaterThanOutsideEnthalpy(double insideEnthalpy, double outsideEnthalpy) {
    
        CcuLog.d(L.TAG_CCU_OAO," insideEnthalpy "+insideEnthalpy+", outsideEnthalpy "+outsideEnthalpy);
    
        double enthalpyDuctCompensationOffset = oaoEquip.getEnthalpyDuctCompensationOffset().readPriorityVal();
        
        return insideEnthalpy > outsideEnthalpy + enthalpyDuctCompensationOffset;
    
    }
    
    public void doDcvControl(double outsideDamperMinOpen, SystemProfile systemProfile) {
        setDcvAvailable(false);
        double dcvCalculatedMinDamper = 0;
        boolean usePerRoomCO2Sensing = oaoEquip.getUsePerRoomCO2Sensing().readDefaultVal() > 0;
        boolean isCo2levelUnderThreshold = true;
        if (usePerRoomCO2Sensing)
        {
            dcvCalculatedMinDamper = systemProfile.getCo2LoopOp();
            CcuLog.d(L.TAG_CCU_OAO,"usePerRoomCO2Sensing dcvCalculatedMinDamper "+dcvCalculatedMinDamper);
            
        } else {
            double returnAirCO2  = oaoEquip.getReturnAirCo2().readHisVal();
            double co2Threshold = oaoEquip.getCo2Threshold().readDefaultVal();
            double co2DamperOpeningRate = oaoEquip.getCo2DamperOpeningRate().readPriorityVal();
            
            if (returnAirCO2 > co2Threshold) {
                dcvCalculatedMinDamper = (returnAirCO2 - co2Threshold)/co2DamperOpeningRate;
                isCo2levelUnderThreshold = false;
            }
            CcuLog.d(L.TAG_CCU_OAO," dcvCalculatedMinDamper "+dcvCalculatedMinDamper+" returnAirCO2 "+returnAirCO2+" co2Threshold "+co2Threshold);
        }
        oaoEquip.getCo2WeightedAverage().writeHisVal(systemProfile.getWeightedAverageCO2());
        Occupancy systemOccupancy = ScheduleManager.getInstance().getSystemOccupancy();
        switch (systemOccupancy) {
            case OCCUPIED:
            case FORCEDOCCUPIED:
            case DEMAND_RESPONSE_OCCUPIED:
                if(systemMode != SystemMode.OFF) {
                    outsideDamperMinOpen = epidemicState != EpidemicState.OFF ? outsideAirCalculatedMinDamper : outsideDamperMinOpen;
                    outsideAirCalculatedMinDamper = Math.min(outsideDamperMinOpen + dcvCalculatedMinDamper, 100);
                    if(!isCo2levelUnderThreshold){
                        setDcvAvailable(true);
                    }
                }else
                    outsideAirCalculatedMinDamper = outsideDamperMinOpen;
                break;
            case PRECONDITIONING:
            case VACATION:
                outsideAirCalculatedMinDamper = outsideDamperMinOpen;
                break;
            case UNOCCUPIED:
            case DEMAND_RESPONSE_UNOCCUPIED:
                if(epidemicState == EpidemicState.OFF)
                    outsideAirCalculatedMinDamper = outsideDamperMinOpen;
                break;
        }
        oaoEquip.getOutsideAirCalculatedMinDamper().writeHisVal(outsideAirCalculatedMinDamper);
        oaoEquip.getDcvAvailable().writeHisVal(isDcvAvailable() ? 1 : 0);
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
        double H = A * 0.01 * averageHumidity + B;
    
        CcuLog.d(L.TAG_CCU_OAO, "temperature "+averageTemp+" humidity "+averageHumidity+" Enthalpy: "+H);
        return CCUUtils.roundToTwoDecimal(H);
    }

    private void handleSmartPrePurgeControl(){
        double smartPrePurgeRunTime = oaoEquip.getSystemPrePurgeRuntimeTuner().readPriorityVal();
        double smartPrePurgeOccupiedTimeOffset = oaoEquip.getSystemPrePurgeOccupiedTimeOffsetTuner().readPriorityVal();
        Occupied occuSchedule = ScheduleManager.getInstance().getNextOccupiedTimeInMillis();
        int minutesToOccupancy = occuSchedule != null ? (int)occuSchedule.getMillisecondsUntilNextChange()/60000 : -1;
        if((minutesToOccupancy != -1) && (smartPrePurgeOccupiedTimeOffset >= minutesToOccupancy) && (minutesToOccupancy >= (smartPrePurgeOccupiedTimeOffset - smartPrePurgeRunTime))) {
            outsideAirCalculatedMinDamper = oaoEquip.getSystemPurgeOutsideDamperMinPos().readDefaultVal();

            //TODO: This needs to changed with domainName
            CCUHsApi.getInstance().writeHisValByQuery("point and sp and system and epidemic and mode and state", (double)EpidemicState.PREPURGE.ordinal());
            epidemicState = EpidemicState.PREPURGE;
        }
    }
    private void handleSmartPostPurgeControl(){
        double smartPostPurgeRunTime = oaoEquip.getSystemPostPurgeRuntimeTuner().readPriorityVal();
        double smartPostPurgeOccupiedTimeOffset = oaoEquip.getSystemPostPurgeOccupiedTimeOffsetTuner().readPriorityVal();
        Occupied occuSchedule = ScheduleManager.getInstance().getPrevOccupiedTimeInMillis();
        if(occuSchedule != null)
            CcuLog.d(L.TAG_CCU_OAO, "System Unoccupied, check postpurge22 = "+occuSchedule.getMillisecondsUntilPrevChange()+","+(occuSchedule.getMillisecondsUntilPrevChange())/60000+","+smartPostPurgeOccupiedTimeOffset+","+smartPostPurgeRunTime);
        int minutesInUnoccupied = occuSchedule != null ? (int)(occuSchedule.getMillisecondsUntilPrevChange()/60000) : -1;
        if( (epidemicState == EpidemicState.OFF) && (minutesInUnoccupied != -1) && (minutesInUnoccupied  >= smartPostPurgeOccupiedTimeOffset) && (minutesInUnoccupied <= (smartPostPurgeRunTime + smartPostPurgeOccupiedTimeOffset))) {
            outsideAirCalculatedMinDamper = oaoEquip.getSystemPurgeOutsideDamperMinPos().readDefaultVal();
            //TODO: This needs to changed with domainName
            CCUHsApi.getInstance().writeHisValByQuery("point and sp and system and epidemic and mode and state", (double)EpidemicState.POSTPURGE.ordinal());
            epidemicState = EpidemicState.POSTPURGE;
        }
    }
    private void handleEnhancedVentilationControl(){
        epidemicState = EpidemicState.ENHANCED_VENTILATION;
        outsideAirCalculatedMinDamper = oaoEquip.getEnhancedVentilationOutsideDamperMinOpen().readDefaultVal();
        //TODO: This needs to changed with domainName
        CCUHsApi.getInstance().writeHisValByQuery("point and sp and system and epidemic and mode and state", (double)EpidemicState.ENHANCED_VENTILATION.ordinal());
        CcuLog.d(L.TAG_CCU_OAO, "System occupied, check enhanced ventilation = "+outsideAirCalculatedMinDamper+","+epidemicState.name());
    }
    public OAOEquip getOAOEquip() {
        return oaoEquip;
    }
}
