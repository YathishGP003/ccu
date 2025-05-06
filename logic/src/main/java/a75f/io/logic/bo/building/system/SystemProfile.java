package a75f.io.logic.bo.building.system;

import static a75f.io.logic.L.ccu;
import static a75f.io.logic.bo.building.system.util.AdvancedAhuUtilKt.getConnectEquip;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;

import android.content.Context;

import org.projecthaystack.HDict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import a75.io.algos.tr.TRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.api.PhysicalPoint;
import a75f.io.domain.devices.CCUDevice;
import a75f.io.domain.logic.CCUBaseConfigurationBuilder;
import a75f.io.domain.logic.CCUDeviceBuilder;
import a75f.io.domain.logic.DiagEquipConfigurationBuilder;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BacnetIdKt;
import a75f.io.logic.BacnetUtilKt;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Schedule;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.building.system.dab.DabSystemProfile;
import a75f.io.logic.bo.building.system.util.AdvancedAhuUtilKt;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemProfile;
import a75f.io.logic.bo.util.DemandResponseMode;
import a75f.io.logic.tuners.SystemTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.util.ExecutorTask;

/**
 * Created by Yinten isOn 8/15/2017.
 */
public abstract class SystemProfile
{

    public Schedule schedule = new Schedule();

    public TRSystem trSystem;

    public SystemEquip sysEquip;
    private String equipRef = null;
    private String siteRef;
    private String equipDis;
    private String tz;
    private CCUHsApi hayStack;

    public double systemCoolingLoopOp;
    public double systemHeatingLoopOp;
    public double systemFanLoopOp;
    public double systemCo2LoopOp;

    private boolean isBypassCoolingLockoutActive;
    private boolean isBypassHeatingLockoutActive;

    private boolean mechanicalCoolingAvailable;
    private boolean mechanicalHeatingAvailable;

    private Timer bypassHeatingLockoutTimer;
    private Timer bypassCoolingLockoutTimer;
    private TimerTask heatingTimerTask;
    private TimerTask coolingTimerTask;

    public abstract void doSystemControl();

    public abstract void addSystemEquip();

    public abstract void deleteSystemEquip();

    //Is Cooling enabled in System Profile
    public abstract boolean isCoolingAvailable();

    public abstract boolean isHeatingAvailable();

    //Is Cooling stage/signal ON now
    public abstract boolean isCoolingActive();

    public abstract boolean isHeatingActive();

    public abstract ProfileType getProfileType();

    public abstract String getStatusMessage();

    public  int getSystemSAT() {
        return 0;
    }

    public  int getSystemCO2() {
        return 0;
    }

    public  int getSystemOADamper() {
        return 0;
    }

    public double getStaticPressure() {
        return 0;
    }

    public String getProfileName() {
        return "";
    }

    public int getAnalog1Out() {
        return 0;
    }

    public int getAnalog2Out() {
        return 0;
    }

    public int getAnalog3Out() {
        return 0;
    }

    public int getAnalog4Out() {
        return 0;
    }

    public double getCoolingLoopOp()
    {
        return systemCoolingLoopOp;
    }
    public double getHeatingLoopOp()
    {
        return systemHeatingLoopOp;
    }
    public double getFanLoopOp()
    {
        return systemFanLoopOp;
    }
    public double getCo2LoopOp() {
        return systemCo2LoopOp;
    }

    public double getCmd(String tags) {
        return CCUHsApi.getInstance().readHisValByQuery(tags+" and cmd and equipRef == \""+getSystemEquipRef()+"\"");
    }

    public SystemController getSystemController() {
        if (this instanceof VavSystemProfile) {
            return VavSystemController.getInstance();
        } else if (this instanceof DabSystemProfile) {
            return DabSystemController.getInstance();
        }
        return DefaultSystemController.getInstance();
    }

    public SystemController.State getConditioning() {
        return getSystemController().getSystemState();
    }

    public double getAverageTemp() {
        return getSystemController().getAverageSystemTemperature();
    }

    public double getWeightedAverageCO2() {
        return getSystemController().getSystemCO2WA();
    }

    public String getSystemEquipRef() {
        if (equipRef == null)
        {
            HashMap equip = CCUHsApi.getInstance().read("equip and system and not modbus and not connectModule");
            equipRef = equip.get("id").toString();
            equipDis = equip.get("dis").toString();
        }
        return equipRef;
    }

    public void updateAhuRef(String systemEquipId) {
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        List<HDict> equips = ccuHsApi.readAllHDictByQuery("equip and zone");
        if (L.ccu().oaoProfile != null || L.ccu().bypassDamperProfile != null) {
            equips.addAll(ccuHsApi.readAllHDictByQuery("equip and " +
                    "(domainName == \"" + DomainName.smartnodeOAO + "\" " +
                    "or domainName == \"" + DomainName.smartnodeBypassDamper +"\" " +
                    ") and not hyperstatsplit"));
        }

        equips.forEach( m -> {
            Equip q = new Equip.Builder().setHDict(m).build();
            //All the zone equips served by AHU/RTU will have an ahuRef.
            if (q.getMarkers().contains("dab") || q.getMarkers().contains("dualDuct") || q.getMarkers().contains("vav")
                || q.getMarkers().contains("ti") || q.getMarkers().contains("oao") || q.getMarkers().contains("sse")
                || q.getMarkers().contains("vrv") || q.getMarkers().contains("otn") || q.getMarkers().contains("bypassDamper")) {
                q.setAhuRef(systemEquipId);
            } else if (q.getMarkers().contains("smartstat") || q.getMarkers().contains("emr") || q.getMarkers().contains("pid") ||
                       q.getMarkers().contains("modbus") || q.getMarkers().contains("monitoring") || q.getMarkers().contains("hyperstat") ||q.getMarkers().contains("hyperstatsplit")) {
                //All the standalone zone equips will have a gatewayRef
                q.setGatewayRef(systemEquipId);
            }else {
                //TODO- This cant happen, we are passing an equip with invalid ahuRef/gatewayRef. There should be
                // some sort of retry mechanism.
                CcuLog.e(L.TAG_CCU_SYSTEM, "Invalid profile, AhuRef is not updated for " + q.getDisplayName());
            }
            ccuHsApi.updateEquip(q, q.getId());
        });
        
        List<HDict> modbusEquips = ccuHsApi.readAllHDictByQuery("equip and modbus");
        modbusEquips.forEach( equipMap -> {
            Equip equip = new Equip.Builder().setHDict(equipMap).build();
            equip.setGatewayRef(systemEquipId);
            ccuHsApi.updateEquip(equip, equip.getId());
        });

        DiagEquipConfigurationBuilder diagEquipConfigurationBuilder = new DiagEquipConfigurationBuilder(ccuHsApi);
        diagEquipConfigurationBuilder.updateDiagGatewayRef(systemEquipId);

        CCUBaseConfigurationBuilder ccuBaseConfigurationBuilder = new CCUBaseConfigurationBuilder(ccuHsApi);
        ccuBaseConfigurationBuilder.updateCcuConfigAhuRef(systemEquipId);

        CCUDeviceBuilder ccuDeviceBuilder = new CCUDeviceBuilder();
        CCUDevice ccuDeviceObj = Domain.ccuDevice;
        ccuDeviceBuilder.buildCCUDevice(ccuDeviceObj.getEquipRef(), ccuDeviceObj.getSiteRef(),
                ccuDeviceObj.getCcuDisName(), ccuDeviceObj.getInstallerEmail(),
                ccuDeviceObj.getManagerEmail(), systemEquipId, true);
    }

    public void addSystemTuners() {

        hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        siteRef = (String) siteMap.get(Tags.ID);
        tz = siteMap.get("tz").toString();
        equipRef = getSystemEquipRef();

        Point heatingPreconditioningRate = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "heatingPreconditioningRate")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("heating").addMarker("precon").addMarker("rate").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String heatingPreconditioningRateId = hayStack.addPoint(heatingPreconditioningRate);
        TunerUtil.copyDefaultBuildingTunerVal(heatingPreconditioningRateId, DomainName.heatingPreconditioningRate, hayStack);

        Point coolingPreconditioningRate = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "coolingPreconditioningRate")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("cooling").addMarker("precon").addMarker("rate").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String coolingPreconditioningRateId = hayStack.addPoint(coolingPreconditioningRate);
        TunerUtil.copyDefaultBuildingTunerVal(coolingPreconditioningRateId, DomainName.coolingPreconditioningRate, hayStack);

        Point cmTempInfPercentileZonesDead = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "cmTempPercentDeadZonesAllowed")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("percent").addMarker("dead").addMarker("influence").addMarker("sp")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String cmTempPercentDeadZonesAllowedId = hayStack.addPoint(cmTempInfPercentileZonesDead);
        TunerUtil.copyDefaultBuildingTunerVal(cmTempPercentDeadZonesAllowedId, DomainName.cmTempPercentDeadZonesAllowed, hayStack);

        Point airflowSampleWaitTime = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "airflowSampleWaitTime")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("airflow").addMarker("sample").addMarker("wait").addMarker("time").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("m")
                .setTz(tz).build();
        String airflowSampleWaitTimeId = hayStack.addPoint(airflowSampleWaitTime);
        TunerUtil.copyDefaultBuildingTunerVal(airflowSampleWaitTimeId, DomainName.airflowSampleWaitTime, hayStack);

        Point stage1CoolingAirflowTempLowerOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage1CoolingAirflowTempLowerOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage1").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage1CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage1CoolingAirflowTempLowerOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage1CoolingAirflowTempLowerOffsetId, DomainName.stage1CoolingAirflowTempLowerOffset, hayStack);

        Point stage2CoolingAirflowTempLowerOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage2CoolingAirflowTempLowerOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage2").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage2CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage2CoolingAirflowTempLowerOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage2CoolingAirflowTempLowerOffsetId, DomainName.stage2CoolingAirflowTempLowerOffset, hayStack);

        Point stage3CoolingAirflowTempLowerOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage3CoolingAirflowTempLowerOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage3").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage3CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage3CoolingAirflowTempLowerOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage3CoolingAirflowTempLowerOffsetId, DomainName.stage3CoolingAirflowTempLowerOffset, hayStack);

        Point stage4CoolingAirflowTempLowerOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage4CoolingAirflowTempLowerOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage4").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage4CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage4CoolingAirflowTempLowerOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage4CoolingAirflowTempLowerOffsetId, DomainName.stage4CoolingAirflowTempLowerOffset, hayStack);

        Point stage5CoolingAirflowTempLowerOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage5CoolingAirflowTempLowerOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage5").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage5CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage5CoolingAirflowTempLowerOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage5CoolingAirflowTempLowerOffsetId, DomainName.stage5CoolingAirflowTempLowerOffset, hayStack);

        Point stage1CoolingAirflowTempUpperOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage1CoolingAirflowTempUpperOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage1").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage1CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage1CoolingAirflowTempUpperOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage1CoolingAirflowTempUpperOffsetId, DomainName.stage1CoolingAirflowTempUpperOffset, hayStack);

        Point stage2CoolingAirflowTempUpperOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage2CoolingAirflowTempUpperOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage2").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage2CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage2CoolingAirflowTempUpperOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage2CoolingAirflowTempUpperOffsetId, DomainName.stage2CoolingAirflowTempUpperOffset, hayStack);

        Point stage3CoolingAirflowTempUpperOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage3CoolingAirflowTempUpperOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage3").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage3CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage3CoolingAirflowTempUpperOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage3CoolingAirflowTempUpperOffsetId, DomainName.stage3CoolingAirflowTempUpperOffset, hayStack);

        Point stage4CoolingAirflowTempUpperOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage4CoolingAirflowTempUpperOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage4").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage4CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage4CoolingAirflowTempUpperOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage4CoolingAirflowTempUpperOffsetId, DomainName.stage4CoolingAirflowTempUpperOffset, hayStack);

        Point stage5CoolingAirflowTempUpperOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage5CoolingAirflowTempUpperOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage5").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage5CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage5CoolingAirflowTempUpperOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage5CoolingAirflowTempUpperOffsetId, DomainName.stage5CoolingAirflowTempUpperOffset, hayStack);

        Point stage1HeatingAirflowTempLowerOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage1HeatingAirflowTempLowerOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage1").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage1HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage1HeatingAirflowTempLowerOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage1HeatingAirflowTempLowerOffsetId, DomainName.stage1HeatingAirflowTempLowerOffset, hayStack);

        Point stage2HeatingAirflowTempLowerOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage2HeatingAirflowTempLowerOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage2").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage2HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage2HeatingAirflowTempLowerOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage2HeatingAirflowTempLowerOffsetId, DomainName.stage2HeatingAirflowTempLowerOffset, hayStack);

        Point stage3HeatingAirflowTempLowerOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage3HeatingAirflowTempLowerOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage3").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage3HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage3HeatingAirflowTempLowerOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage3HeatingAirflowTempLowerOffsetId, DomainName.stage3HeatingAirflowTempLowerOffset, hayStack);

        Point stage4HeatingAirflowTempLowerOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage4HeatingAirflowTempLowerOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage4").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage4HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage4HeatingAirflowTempLowerOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage4HeatingAirflowTempLowerOffsetId, DomainName.stage4HeatingAirflowTempLowerOffset, hayStack);

        Point stage5HeatingAirflowTempLowerOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage5HeatingAirflowTempLowerOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage5").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage5HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage5HeatingAirflowTempLowerOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage5HeatingAirflowTempLowerOffsetId, DomainName.stage5HeatingAirflowTempLowerOffset, hayStack);

        Point stage1HeatingAirflowTempUpperOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage1HeatingAirflowTempUpperOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage1").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage1HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage1HeatingAirflowTempUpperOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage1HeatingAirflowTempUpperOffsetId, DomainName.stage1HeatingAirflowTempUpperOffset, hayStack);

        Point stage2HeatingAirflowTempUpperOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage2HeatingAirflowTempUpperOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage2").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage2HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage2HeatingAirflowTempUpperOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage2HeatingAirflowTempUpperOffsetId, DomainName.stage2HeatingAirflowTempUpperOffset, hayStack);

        Point stage3HeatingAirflowTempUpperOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage3HeatingAirflowTempUpperOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage3").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage3HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage3HeatingAirflowTempUpperOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage3HeatingAirflowTempUpperOffsetId, DomainName.stage3HeatingAirflowTempUpperOffset, hayStack);

        Point stage4HeatingAirflowTempUpperOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage4HeatingAirflowTempUpperOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage4").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage4HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage4HeatingAirflowTempUpperOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage4HeatingAirflowTempUpperOffsetId, DomainName.stage4HeatingAirflowTempUpperOffset, hayStack);

        Point stage5HeatingAirflowTempUpperOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "stage5HeatingAirflowTempUpperOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("stage5").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz).build();
        String stage5HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage5HeatingAirflowTempUpperOffset);
        TunerUtil.copyDefaultBuildingTunerVal(stage5HeatingAirflowTempUpperOffsetId, DomainName.stage5HeatingAirflowTempUpperOffset, hayStack);

        Point clockUpdateInterval = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "clockUpdateInterval")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("clock").addMarker("update").addMarker("interval").addMarker("sp")
                .setMinVal("1").setMaxVal("120").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz).build();
        String clockUpdateIntervalId = hayStack.addPoint(clockUpdateInterval);
        TunerUtil.copyDefaultBuildingTunerVal(clockUpdateIntervalId, DomainName.clockUpdateInterval, hayStack);

        Point perDegreeHumidityFactor = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "perDegreeHumidityFactor")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("per").addMarker("degree").addMarker("humidity").addMarker("factor").addMarker("sp")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("%")
                .setTz(tz).build();
        String perDegreeHumidityFactorId = hayStack.addPoint(perDegreeHumidityFactor);
        TunerUtil.copyDefaultBuildingTunerVal(perDegreeHumidityFactorId, DomainName.perDegreeHumidityFactor, hayStack);

        Point ccuAlarmVolumeLevel = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "ccuAlarmVolumeLevel")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("alarm").addMarker("volume").addMarker("level").addMarker("sp")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String ccuAlarmVolumeLevelId = hayStack.addPoint(ccuAlarmVolumeLevel);
        TunerUtil.copyDefaultBuildingTunerVal(ccuAlarmVolumeLevelId, DomainName.ccuAlarmVolumeLevel, hayStack);

        Point cmHeartBeatInterval = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "cmHeartBeatInterval")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("cm").addMarker("heartbeat").addMarker("interval").addMarker("level").addMarker("sp")
                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz).build();
        String cmHeartBeatIntervalId = hayStack.addPoint(cmHeartBeatInterval);
        TunerUtil.copyDefaultBuildingTunerVal(cmHeartBeatIntervalId, DomainName.cmHeartBeatInterval, hayStack);

        Point heartBeatsToSkip = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "heartBeatsToSkip")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("heartbeat").addMarker("sp")
                .setMinVal("3").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String heartBeatsToSkipId = hayStack.addPoint(heartBeatsToSkip);
        TunerUtil.copyDefaultBuildingTunerVal(heartBeatsToSkipId, DomainName.heartBeatsToSkip, hayStack);

        Point cmResetCommandTime = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "cmResetCommandTimer")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("reset").addMarker("command").addMarker("time")
                .setMinVal("0").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER).setUnit("m")
                .setTz(tz).build();
        String cmResetCommandTimerId = hayStack.addPoint(cmResetCommandTime);
        TunerUtil.copyDefaultBuildingTunerVal(cmResetCommandTimerId, DomainName.cmResetCommandTimer, hayStack);

        Point zoneTempDeadLeeway = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "zoneTemperatureDeadLeeway")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("temp").addMarker("dead").addMarker("leeway").addMarker("sp")
                .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP).setUnit("\u00B0F")
                .setTz(tz).build();
        String zoneTemperatureDeadLeewayId = hayStack.addPoint(zoneTempDeadLeeway);
        TunerUtil.copyDefaultBuildingTunerVal(zoneTemperatureDeadLeewayId, DomainName.zoneTemperatureDeadLeeway, hayStack);

        Point humidityCompensationOffset = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(HSUtil.getDis(equipRef) + "-" + "humidityCompensationOffset")).setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system").addMarker("tuner").addMarker("writable").addMarker("his").addMarker("humidity").addMarker("compensation").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz).build();
        String humidityCompensationOffsetId = hayStack.addPoint(humidityCompensationOffset);
        TunerUtil.copyDefaultBuildingTunerVal(humidityCompensationOffsetId, DomainName.humidityCompensationOffset, hayStack);
        DemandResponseMode.createDemandResponseSetBackTuner(hayStack, equipRef, equipDis, true,
                null, null);
    }

    public void addCMPoints(String siteRef, String equipref, String equipDis , String tz) {
        Point cmCoolDesiredTemp = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "cmCoolingDesiredTemp"))
                .setBacnetId(BacnetIdKt.CMCOOLINGDESIREDTEMPID).setBacnetType(BacnetUtilKt.ANALOG_VALUE).setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("cm").addMarker("cooling").addMarker("desired").addMarker("temp").addMarker("writable").addMarker("his").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        String cmCoolDesiredTempId = CCUHsApi.getInstance().addPoint(cmCoolDesiredTemp);
        CCUHsApi.getInstance().writeDefaultValById(cmCoolDesiredTempId, 0.0);
        CCUHsApi.getInstance().writeHisValById(cmCoolDesiredTempId, 0.0);
        Point cmHeatDesiredTemp = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "cmHeatingDesiredTemp"))
                .setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("cm")
                .addMarker("heating").addMarker("desired").addMarker("temp").addMarker("writable").addMarker("his")
                .addMarker("sp").setUnit("\u00B0F").setBacnetId(BacnetIdKt.CMHEATINGDESIREDTEMPID).setBacnetType(BacnetUtilKt.ANALOG_VALUE).setTz(tz).build();
        String cmHeatDesiredTempId = CCUHsApi.getInstance().addPoint(cmHeatDesiredTemp);
        CCUHsApi.getInstance().writeDefaultValById(cmHeatDesiredTempId, 0.0);
        CCUHsApi.getInstance().writeHisValById(cmHeatDesiredTempId, 0.0);
    }


    public void addDefaultSystemPoints(String siteRef, String equipref, String equipDis, String tz)
    {
        Point cmCurrentTemp = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "cmCurrentTemp")).setSiteRef(siteRef)
                .setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("cm").addMarker("current")
                .addMarker("temp").addMarker("his").addMarker("sp").setUnit("\u00B0F").setBacnetId(BacnetIdKt.CURRENTTEMPID).setBacnetType(BacnetUtilKt.ANALOG_VALUE).setTz(tz).build();
        String ctID = CCUHsApi.getInstance().addPoint(cmCurrentTemp);
        CCUHsApi.getInstance().writeHisValById(ctID, 0.0);
        Point systemStatusMessage = new Point.Builder()
                .setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-StatusMessage"))
                .setEquipRef(equipref)
                .setSiteRef(siteRef)
                .addMarker("system")
                .addMarker("status")
                .addMarker("message")
                .addMarker("writable")
                .setTz(tz)
                .setKind(Kind.STRING).build();
        CCUHsApi.getInstance().addPoint(systemStatusMessage);
        Point systemScheduleStatus = new Point.Builder()
                .setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-ScheduleStatus"))
                .setEquipRef(equipref).setSiteRef(siteRef)
                .addMarker("system")
                .addMarker("scheduleStatus")
                .addMarker("writable")
                .setTz(tz)
                .setKind(Kind.STRING).build();
        CCUHsApi.getInstance().addPoint(systemScheduleStatus);

        Point outsideTemperature = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "outsideTemperature"))
                .setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system")
                .addMarker("outside").addMarker("temp").addMarker("his").addMarker("sp").setUnit("\u00B0F")
                .setBacnetId(BacnetIdKt.OUTSIDETEMPERATUREID).setBacnetType(BacnetUtilKt.ANALOG_VALUE).setTz(tz).build();
        String outsideTempId = CCUHsApi.getInstance().addPoint(outsideTemperature);
        CCUHsApi.getInstance().writeHisValById(outsideTempId, 0.0);
        
        Point outsideHumidity = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "outsideHumidity")).setSiteRef(siteRef)
                .setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("outside").addMarker("humidity")
                .addMarker("his").addMarker("sp").setUnit("%").setBacnetId(BacnetIdKt.OUTSIDEHUMIDITYID).setBacnetType(BacnetUtilKt.ANALOG_VALUE).setTz(tz).build();
        String outsideHumidityId = CCUHsApi.getInstance().addPoint(outsideHumidity);
        CCUHsApi.getInstance().writeHisValById(outsideHumidityId, 0.0);

    }

    //VAV & DAB System profile common points are added here.
    public void addRTUSystemPoints(String siteRef, String equipref, String equipDis, String tz) {
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        addDefaultSystemPoints(siteRef, equipref, equipDis, tz);
        Point systemOccupancy =
                new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "occupancy")).setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("occupancy").addMarker("mode").addMarker("his").addMarker("sp")
                                   .setEnums(Occupancy.getEnumStringDefinition()).setTz(tz).build();
        String sysOccupancyId = ccuHsApi.addPoint(systemOccupancy);
        ccuHsApi.writeHisValById(sysOccupancyId, 0.0);
        Point systemOperatingMode = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "operatingMode"))
                .setBacnetId(BacnetIdKt.OPERATIONMODEID).setBacnetType(BacnetUtilKt.MULTI_STATE_VALUE)
                .setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").
                addMarker("operating").addMarker("mode").addMarker("his").addMarker("sp").setEnums("off,cooling,heating").setTz(tz).build();
        ccuHsApi.addPoint(systemOperatingMode);
        Point ciRunning = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "systemCI")).setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("ci").addMarker("running").addMarker("his").addMarker("sp").setTz(tz).build();
        ccuHsApi.addPoint(ciRunning);
        Point averageHumidity = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "averageHumidity"))
                .setBacnetId(BacnetIdKt.AVERAGEHUMIDITYID).setBacnetType(BacnetUtilKt.ANALOG_VALUE).setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("average").addMarker("humidity").addMarker("his").addMarker("sp").setUnit("%").setTz(tz).build();
        ccuHsApi.addPoint(averageHumidity);
        Point cmHumidity = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "cmHumidity")).setSiteRef(siteRef).setEquipRef(equipref).
                setHisInterpolate("cov").addMarker("system").addMarker("cm").addMarker("humidity").addMarker("his").addMarker("sp").
                setTz(tz).setUnit("%").setBacnetId(BacnetIdKt.HUMIDITYID).setBacnetType(BacnetUtilKt.ANALOG_VALUE).build();
        String cmHumidityId = ccuHsApi.addPoint(cmHumidity);
        ccuHsApi.writeHisValById(cmHumidityId, 0.0);
        Point averageTemperature = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "averageTemperature"))
                .setBacnetId(BacnetIdKt.AVERAGETEMPERATUREID).setBacnetType(BacnetUtilKt.ANALOG_VALUE).setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("average").addMarker("temp").addMarker("his").addMarker("sp").setUnit("\u00B0F").setTz(tz).build();
        ccuHsApi.addPoint(averageTemperature);
        addCMPoints(siteRef, equipref, equipDis, tz);
        addNewSystemUserIntentPoints(equipref);
    }
    private boolean verifyPointsAvailability(String tags, String equipRef){
        ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point and system and "+tags+" and equipRef == \"" + equipRef + "\"");
        return points != null && points.size() > 0;
    }
    public void addNewSystemUserIntentPoints(String equipref){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        if(!verifyPointsAvailability("epidemic and mode and state",equipref)){

            Point epidemicModeSystemState = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "epidemicModeSystemState")).setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("epidemic").addMarker("mode").addMarker("state").addMarker("his").addMarker("sp").setEnums("off,prepurge,postpurge,enhancedventilation").setTz(tz).build();
            String epidemicModeSystemStateId = hayStack.addPoint(epidemicModeSystemState);
            hayStack.writeHisValById(epidemicModeSystemStateId, 0.0);
        }
        if(!verifyPointsAvailability("userIntent and prePurge and enabled",equipref)) {
            Point smartPrePurgePoint = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "systemPrePurgeEnabled")).setSiteRef(siteRef).setEquipRef(equipref).addMarker("sp").addMarker("system").setHisInterpolate("cov").addMarker("userIntent").addMarker("writable").addMarker("his").addMarker("prePurge").addMarker("enabled").setEnums("false,true").setTz(tz).build();
            String smartPrePurgePointId = hayStack.addPoint(smartPrePurgePoint);
            hayStack.writePointForCcuUser(smartPrePurgePointId, TunerConstants.UI_DEFAULT_VAL_LEVEL, 0.0, 0);
            hayStack.writeHisValById(smartPrePurgePointId, 0.0);
        }
        if(!verifyPointsAvailability("userIntent and postPurge and enabled",equipref)) {
            Point smartPostPurgePoint = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "systemPostPurgeEnabled")).setSiteRef(siteRef).setEquipRef(equipref).addMarker("sp").addMarker("system").setHisInterpolate("cov").addMarker("userIntent").addMarker("writable").addMarker("his").addMarker("postPurge").addMarker("enabled").setEnums("false,true").setTz(tz).build();
            String smartPostPurgePointId = hayStack.addPoint(smartPostPurgePoint);
            hayStack.writePointForCcuUser(smartPostPurgePointId, TunerConstants.UI_DEFAULT_VAL_LEVEL, 0.0, 0);
            hayStack.writeHisValById(smartPostPurgePointId, 0.0);
        }

        if(!verifyPointsAvailability("userIntent and enhanced and ventilation and enabled",equipref)) {
            Point enhancedVentilationPoint = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "systemEnhancedVentilationEnabled")).setSiteRef(siteRef).setEquipRef(equipref).addMarker("sp").addMarker("system").setHisInterpolate("cov").addMarker("userIntent").addMarker("writable").addMarker("his").addMarker("enhanced").addMarker("ventilation").addMarker("enabled").setEnums("false,true").setTz(tz).build();
            String enhancedVentilationPointId = hayStack.addPoint(enhancedVentilationPoint);
            hayStack.writePointForCcuUser(enhancedVentilationPointId, TunerConstants.UI_DEFAULT_VAL_LEVEL, 0.0, 0);
            hayStack.writeHisValById(enhancedVentilationPointId, 0.0);
        }

        createOutsideTempLockoutPoints(hayStack, siteRef, equipref, equipDis, tz);
    }

    private void createOutsideTempLockoutPoints(CCUHsApi hayStack, String siteRef, String equipref, String equipDis,
                                                String tz) {
        if(!verifyPointsAvailability("config and outsideTemp and cooling and lockout",equipref)) {
            Point useOutsideTempLockoutCooling = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "useOutsideTempLockoutCooling"))
                                                                .setSiteRef(siteRef)
                                                                .setEquipRef(equipref)
                                                                .addMarker("sp").addMarker("system").setHisInterpolate("cov")
                                                                .addMarker("config").addMarker("writable")
                                                                .addMarker("outsideTemp").addMarker("cooling")
                                                                .addMarker("lockout").addMarker("enabled").addMarker("his")
                                                                .setEnums("false,true").setTz(tz).build();
            String useOutsideTempLockoutCoolingId = hayStack.addPoint(useOutsideTempLockoutCooling);
            double defaultVal = ccu().oaoProfile == null ? 0 : 1.0;
            hayStack.writeDefaultValById(useOutsideTempLockoutCoolingId, defaultVal);
            hayStack.writeHisValueByIdWithoutCOV(useOutsideTempLockoutCoolingId, defaultVal);
        }
    
        if(!verifyPointsAvailability("config and outsideTemp and heating and lockout",equipref)) {
            Point useOutsideTempLockoutHeating = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" +
                                                                                    "useOutsideTempLockoutHeating"))
                                                                    .setSiteRef(siteRef)
                                                                    .setEquipRef(equipref)
                                                                    .addMarker("sp").addMarker("system").setHisInterpolate("cov")
                                                                    .addMarker("config").addMarker("writable")
                                                                    .addMarker("outsideTemp").addMarker("heating")
                                                                    .addMarker("lockout").addMarker("enabled").addMarker("his")
                                                                    .setEnums("false,true").setTz(tz).build();
            String useOutsideTempLockoutHeatingId = hayStack.addPoint(useOutsideTempLockoutHeating);
            hayStack.writeDefaultValById(useOutsideTempLockoutHeatingId, 0.0);
            hayStack.writeHisValueByIdWithoutCOV(useOutsideTempLockoutHeatingId, 0.0);
        }
    
        if(!verifyPointsAvailability("mechanical and cooling and available",equipref)) {
            Point mechanicalCoolingAvailablePoint = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" +
                                                                                    "mechanicalCoolingAvailable"))
                                                                    .setSiteRef(siteRef)
                                                                    .setEquipRef(equipref)
                                                                    .addMarker("sp").addMarker("system").setHisInterpolate("cov")
                                                                    .addMarker("his").addMarker("mechanical").addMarker("cooling")
                                                                    .addMarker("available")
                                                                    .setEnums("false,true").setTz(tz).build();
            String mechanicalCoolingAvailableId = hayStack.addPoint(mechanicalCoolingAvailablePoint);
            hayStack.writeHisValueByIdWithoutCOV(mechanicalCoolingAvailableId, 0.0);
        }
        if(!verifyPointsAvailability("mechanical and heating and available",equipref)) {
            Point mechanicalHeatingAvailablePoint = new Point.Builder().setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" +
                                                                                  "mechanicalHeatingAvailable"))
                                                                  .setSiteRef(siteRef)
                                                                  .setEquipRef(equipref)
                                                                  .addMarker("sp").addMarker("system").setHisInterpolate("cov")
                                                                  .addMarker("his").addMarker("mechanical").addMarker("heating")
                                                                  .addMarker("available")
                                                                  .setEnums("false,true").setTz(tz).build();
            String mechanicalHeatingAvailableId = hayStack.addPoint(mechanicalHeatingAvailablePoint);
            hayStack.writeHisValueByIdWithoutCOV(mechanicalHeatingAvailableId, 0.0);
        }
        String profileTag = getProfileName().contains(Tags.VAV.toUpperCase()) ?Tags.VAV : Tags.DAB;
        addLockoutTempTuners(hayStack, siteRef, equipref, equipDis, profileTag, tz);
    }
    
    private void addLockoutTempTuners(CCUHsApi hayStack, String siteRef, String equipref, String equipDis,
                                      String profileTag, String tz) {
        HashMap<Object, Object> coolingLockoutTemp = hayStack
                                                             .readEntity("tuner and system and outsideTemp " +
                                                                     "and cooling and lockout and equipRef == \""+equipref+"\"");
        
        if (coolingLockoutTemp.isEmpty()) {
            SystemTuners.createCoolingTempLockoutPoint(hayStack, siteRef, equipref, equipDis,
                                                       tz, profileTag, false);
            
        }
        
        HashMap<Object, Object> heatingLockoutTemp = hayStack
                                                             .readEntity("tuner and system and outsideTemp " +
                                                                     "and heating and lockout and equipRef == \""+equipref+"\"");
        
        if (heatingLockoutTemp.isEmpty()) {
            SystemTuners.createHeatingTempLockoutPoint(hayStack, siteRef, equipref, equipDis,
                                                       tz, profileTag, false);
        }
    }
    
    public void updateOutsideWeatherParams() {
        hayStack = CCUHsApi.getInstance();
        double externalTemp = 0;
        double externalHumidity = 0;
        try {
            if (Globals.getInstance().isWeatherTest()) {
                externalTemp = Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                        .getInt("outside_temp", 0);
                externalHumidity = Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                        .getInt("outside_humidity", 0);
            } else {
                externalTemp = hayStack.getExternalTemp();
                externalHumidity = hayStack.getExternalHumidity();
            }
        } catch (Exception e) {
            CcuLog.d(L.TAG_CCU_OAO, " Failed to read external Temp or Humidity ",e);
        }
        
        if (ccu().oaoProfile != null) {
            //Successfully read weather info. Update to OAO weather point
            ccu().oaoProfile.getOAOEquip().getWeatherOutsideTemp().writeHisVal(externalTemp);
            ccu().oaoProfile.getOAOEquip().getWeatherOutsideHumidity().writeHisVal(externalHumidity);
            CcuLog.i(L.TAG_CCU_OAO, "Setting external temp and humidity to OAO");
        }

        // Update weather points on all HyperStat Split equips
        ArrayList<HashMap<Object, Object>> hssEquips = hayStack.readAllEntities("equip and hyperstatsplit");
        if (!hssEquips.isEmpty()) {
            for(HashMap<Object, Object> hssEquip : hssEquips) {
                String equipId = hssEquip.get(Tags.ID).toString();
                hayStack.writeHisValByQuery("outsideWeather and air and temp and equipRef == \"" + equipId + "\"", externalTemp);
                hayStack.writeHisValByQuery("outsideWeather and air and humidity and equipRef == \"" + equipId + "\"", externalHumidity);
            }
        }
        if (getConnectEquip() != null && getConnectEquip().getEnableOutsideAirOptimization().readDefaultVal() == 1.0) {
            // Update weather points on connect Module equips
            if (!getConnectEquip().getOutsideHumidity().pointExists()) {
                getConnectEquip().getOutsideHumidity().writeHisVal(externalHumidity);
            }
            if (!getConnectEquip().getOutsideTemperature().pointExists()) {
                getConnectEquip().getOutsideTemperature().writeHisVal(externalTemp);
            }
        }
        hayStack.writeHisValByQuery("system and not connectModule and outside and temp and not lockout", externalTemp);
        hayStack.writeHisValByQuery("system and not connectModule and outside and humidity", externalHumidity);
    }
    
    /**
     * System occupancy mapped to actual AHU-FAN Turning ON requirement.
     * @return
     */
    public boolean isSystemOccupied() {
        return ScheduleManager.getInstance().getSystemOccupancy() != Occupancy.UNOCCUPIED &&
                ScheduleManager.getInstance().getSystemOccupancy() != Occupancy.VACATION &&
                ScheduleManager.getInstance().getSystemOccupancy() != Occupancy.AUTOAWAY &&
                ScheduleManager.getInstance().getSystemOccupancy() != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                ScheduleManager.getInstance().getSystemOccupancy() != Occupancy.NONE;
    }

    public boolean isSystemOccupiedForDcv() {
        return ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.OCCUPIED ||
                ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.FORCEDOCCUPIED ||
                ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.AUTOFORCEOCCUPIED ||
                ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.DEMAND_RESPONSE_OCCUPIED;
    }

    public void reset() {
    }
    
    public boolean isOutsideTempCoolingLockoutEnabled(CCUHsApi hayStack) {
        return hayStack.readDefaultVal("system and config and " +
                                              "cooling and lockout") > 0;
    }
    
    public boolean isOutsideTempHeatingLockoutEnabled(CCUHsApi hayStack) {
        return hayStack.readDefaultVal("system and config and " +
                                       "heating and lockout") > 0;
    }
    
    public void setOutsideTempCoolingLockoutEnabled(CCUHsApi hayStack, boolean enabled) {
        hayStack.writeDefaultVal("system and config and cooling and lockout", enabled ?
                                                                                                  1.0: 0);
        hayStack.writeHisValByQuery("system and config and cooling and lockout", enabled ?
                                                                                                     1.0: 0);
    }
    
    public void setOutsideTempHeatingLockoutEnabled(CCUHsApi hayStack, boolean enabled) {
        hayStack.writeDefaultVal("system and config and heating and lockout", enabled ?
                                                                                                  1.0 : 0);
        hayStack.writeHisValByQuery("system and config and heating and lockout", enabled ?
                                                                                                     1.0 : 0);
    }


    public void setCoolingLockoutVal(CCUHsApi hayStack, double val) {
        HashMap<Object, Object> coolingLockoutPoint = hayStack.readEntity("point and tuner " +
                "and cooling and lockout and equipRef ==\""
                +ccu().systemProfile.getSystemEquipRef()+"\"");
        if (!coolingLockoutPoint.isEmpty()) {
            ExecutorTask.executeBackground(() ->hayStack.writePointForCcuUser(coolingLockoutPoint.get("id").toString(),
                    HayStackConstants.SYSTEM_POINT_LEVEL, val, 0));
        }
    }

    public void setHeatingLockoutVal(CCUHsApi hayStack, double val) {
        HashMap<Object, Object> heatingLockoutPoint = hayStack.readEntity("point and tuner " +
                "and heating and lockout and equipRef ==\""
                +ccu().systemProfile.getSystemEquipRef()+"\"");

        if (!heatingLockoutPoint.isEmpty()) {
            ExecutorTask.executeBackground(() ->hayStack.writePointForCcuUser(heatingLockoutPoint.get("id").toString(),
                    HayStackConstants.SYSTEM_POINT_LEVEL, val, 0));
        }
    }
    
    public double getOutsideAirTemp(CCUHsApi hayStack) {
        double outsideAirTemp = hayStack.readHisValByQuery("system and outside and temp and not lockout");
        if (outsideAirTemp == 0 && ccu().oaoProfile != null) {
            //We could be here when weather API fails or the device is offline and oao is paired.
            //Try to read LocalOAT fed by the thermistor.
            outsideAirTemp = ccu().oaoProfile.getOAOEquip().getOutsideTemperature().readHisVal();
        }
        return outsideAirTemp;
    }

    public double getCoolingLockoutVal() {
        if(isVavSystemProfile()) {
            return TunerUtil.readTunerValByQuery("((domainName == \"vavOutsideTempCoolingLockout\") or (outsideTemp and cooling and lockout))", getSystemEquipRef());
        }
        return TunerUtil.readTunerValByQuery("((domainName == \"dabOutsideTempCoolingLockout\") or (outsideTemp and cooling and lockout))", getSystemEquipRef());
    }
    
    public double getHeatingLockoutVal() {
        if(isVavSystemProfile()) {
            return TunerUtil.readTunerValByQuery("((domainName == \"vavOutsideTempHeatingLockout\") or (outsideTemp and heating and lockout))", getSystemEquipRef());
        }
        return TunerUtil.readTunerValByQuery("((domainName == \"dabOutsideTempHeatingLockout\") or (outsideTemp and heating and lockout))", getSystemEquipRef());
    }

    public void updateBypassSatLockouts(CCUHsApi hayStack) {
        if (L.ccu().bypassDamperProfile != null) {
            double bypassSat = L.ccu().bypassDamperProfile.getBdEquip().getSupplyAirTemp().readHisVal();
            double satMinThreshold = L.ccu().bypassDamperProfile.getBdEquip().getSatMinThreshold().readPriorityVal();
            double satMaxThreshold = L.ccu().bypassDamperProfile.getBdEquip().getSatMaxThreshold().readPriorityVal();

            CcuLog.d("CCU_BYPASS", "handle Bypass SAT lockout: SAT = " + bypassSat + " °F");

            if (bypassSat < satMinThreshold && bypassSat > 0.0) {
                CcuLog.d("CCU_BYPASS", "SAT is below satMinThreshold of " + satMinThreshold + " °F");
                handleBypassHeatingLockoutNormal();
                handleBypassCoolingLockoutTripped();
            } else if (bypassSat > satMaxThreshold && bypassSat < 200.0) {
                CcuLog.d("CCU_BYPASS", "SAT is above satMaxThreshold of " + satMaxThreshold + " °F");
                handleBypassHeatingLockoutTripped();
                handleBypassCoolingLockoutNormal();
            } else {
                CcuLog.d("CCU_BYPASS", "SAT is in-range of " + satMinThreshold + "-" + satMaxThreshold + " or is invalid");
                handleBypassHeatingLockoutNormal();
                handleBypassCoolingLockoutNormal();
            }
        } else {
            handleBypassHeatingLockoutNormal();
            handleBypassCoolingLockoutNormal();
        }

        hayStack.writeHisValByQuery("point and domainName == \"" + DomainName.bypassHeatingLockout + "\"", isBypassHeatingLockoutActive ? 1.0 : 0.0);
        hayStack.writeHisValByQuery("point and domainName == \"" + DomainName.bypassCoolingLockout + "\"", isBypassCoolingLockoutActive ? 1.0 : 0.0);
    }

    private void handleBypassHeatingLockoutTripped() {
        if (bypassHeatingLockoutTimer == null) {
            long satLimitBreachTime = (long)(60000 * ccu().bypassDamperProfile.getBdEquip().getBypassDamperSATTimeDelay().readPriorityVal());
            if (satLimitBreachTime == 0) { satLimitBreachTime = 5 * 60000; }
            bypassHeatingLockoutTimer = new Timer();
            heatingTimerTask = new TimerTask() {
                @Override
                public void run() {
                    isBypassHeatingLockoutActive = true;
                }
            };
            bypassHeatingLockoutTimer.schedule(heatingTimerTask, satLimitBreachTime);
            CcuLog.d("CCU_BYPASS", "Starting heating lockout timer of " + satLimitBreachTime + " ms");
        }
    }

    private void handleBypassCoolingLockoutTripped() {
        if (bypassCoolingLockoutTimer == null) {
            long satLimitBreachTime = (long)(60000 * ccu().bypassDamperProfile.getBdEquip().getBypassDamperSATTimeDelay().readPriorityVal());
            if (satLimitBreachTime == 0) { satLimitBreachTime = 5 * 60000; }
            bypassCoolingLockoutTimer = new Timer();
            coolingTimerTask = new TimerTask() {
                @Override
                public void run() {
                    isBypassCoolingLockoutActive = true;
                }
            };
            bypassCoolingLockoutTimer.schedule(coolingTimerTask, satLimitBreachTime);
            CcuLog.d("CCU_BYPASS", "Starting cooling lockout timer of " + satLimitBreachTime + " ms");
        }
    }

    private void handleBypassHeatingLockoutNormal() {
        isBypassHeatingLockoutActive = false;

        if (bypassHeatingLockoutTimer != null) {
            bypassHeatingLockoutTimer.cancel();
            bypassHeatingLockoutTimer = null;
            CcuLog.d("CCU_BYPASS", "Heating lockout timer cancelled");
        }
    }

    private void handleBypassCoolingLockoutNormal() {
        isBypassCoolingLockoutActive = false;

        if (bypassCoolingLockoutTimer != null) {
            bypassCoolingLockoutTimer.cancel();
            bypassCoolingLockoutTimer = null;
            CcuLog.d("CCU_BYPASS", "Cooling lockout timer cancelled");
        }
    }

    public void updateMechanicalConditioning(CCUHsApi hayStack) {
        updateBypassSatLockouts(hayStack);
        CcuLog.e("CCU_BYPASS", "Updating conditioning: isBypassHeatingLockoutActive " + isBypassHeatingLockoutActive + ", isBypassCoolingLockoutActive " + isBypassCoolingLockoutActive);

        double outsideAirTemp = getOutsideAirTemp(hayStack);
        if (isOutsideTempCoolingLockoutEnabled(hayStack)) {
            mechanicalCoolingAvailable = outsideAirTemp > getCoolingLockoutVal() && !isBypassCoolingLockoutActive;
        } else {
            mechanicalCoolingAvailable = !isBypassCoolingLockoutActive;
        }
        hayStack.writeHisValByQuery("system and cooling and available", mechanicalCoolingAvailable ?
                                                                                           1.0 : 0);
    
        if (isOutsideTempHeatingLockoutEnabled(hayStack)) {
            mechanicalHeatingAvailable = outsideAirTemp < getHeatingLockoutVal() && !isBypassHeatingLockoutActive;
        } else {
            mechanicalHeatingAvailable = !isBypassHeatingLockoutActive;
        }
        hayStack.writeHisValByQuery("system and heating and available", mechanicalHeatingAvailable ?
                                                                                           1.0 : 0);
        CcuLog.i(L.TAG_CCU_SYSTEM,
                 "outsideAirTemp "+outsideAirTemp+ " mechanicalCoolingAvailable "+mechanicalCoolingAvailable+
                                  " mechanicalHeatingAvailable "+mechanicalHeatingAvailable+" coolingLockoutActive ");
    }
    
    public boolean isCoolingLockoutActive() {
        return !mechanicalCoolingAvailable;
    }
    
    public boolean isHeatingLockoutActive() {
        return !mechanicalHeatingAvailable;
    }



    public double getSystemLoopOutputValue(String state){
         double systemLoopOPValue = CCUHsApi.getInstance().readPointPriorityValByQuery(state+" and system and loop and output and point");
         CcuLog.i(L.TAG_CCU_AUTO_COMMISSIONING, "getSystemLoopOutputValue- "+state+": "+systemLoopOPValue);
         return systemLoopOPValue;
    }

    public void writeSystemLoopOutputValue(String state, double value){
        CcuLog.i(L.TAG_CCU_AUTO_COMMISSIONING, "writing "+state+" Loop Output value to HS (default level) "+value);
        CCUHsApi.getInstance().writeDefaultVal(state+" and system and loop and output and point",value);
    }

    public void removeSystemEquipModbus() {
        // TODO if it has modbus Equip Revisit when we add Bacnet support
        HashMap modbusEquip = CCUHsApi.getInstance().readEntity("system and equip and modbus and not emr and not btu and not equipRef");
        if (modbusEquip != null && !modbusEquip.isEmpty()) {
            // Taking sub equip points before deleting the modbus equip
            ArrayList<HashMap<Object, Object>> modbusSubEquips = CCUHsApi.getInstance().readAllEntities("system and equip and modbus and not emr and not btu and equipRef == \"" + Objects.requireNonNull(modbusEquip.get("id")) + "\"");

            CCUHsApi.getInstance().deleteEntityTree(Objects.requireNonNull(modbusEquip.get("id")).toString());
            HashMap modbusDevice = CCUHsApi.getInstance().readEntity("modbus and device and equipRef == \"" + Objects.requireNonNull(modbusEquip.get("id")) + "\"");
            if (modbusDevice != null && !modbusEquip.isEmpty()) {
                CCUHsApi.getInstance().deleteEntityTree(Objects.requireNonNull(modbusDevice.get("id")).toString());
            }
            //Deleting sub equip device points
            for (HashMap modbusSubEquip : modbusSubEquips) {
                HashMap modbusSubEquipDevice = CCUHsApi.getInstance().readEntity("modbus and device and equipRef == \"" + Objects.requireNonNull(modbusSubEquip.get("id")) + "\"");
                if (modbusSubEquipDevice != null && !modbusSubEquipDevice.isEmpty()) {
                    CCUHsApi.getInstance().deleteEntityTree(Objects.requireNonNull(modbusSubEquipDevice.get("id")).toString());
                }
            }

        }

    }
     public void deleteOAODamperEquip() {
        if((ccu().oaoProfile != null)) {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            Device oaoSN = HSUtil.getDevice((short) ccu().oaoProfile.getNodeAddress());
            hayStack.deleteEntityTree(ccu().oaoProfile.getEquipRef());
            hayStack.deleteEntityTree((oaoSN.getId()));
            ccu().oaoProfile = null;
            CcuLog.i(L.TAG_CCU_SYSTEM, "OAO Equip deleted successfully");
        }
        else {
            CcuLog.i(L.TAG_CCU_SYSTEM, "OAO Equip not found");
        }
    }
    public void deleteBypassDamperEquip() {
        if(ccu().bypassDamperProfile != null) {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            Device bypassDamper = HSUtil.getDevice((short) ccu().bypassDamperProfile.getNodeAddr());
            hayStack.deleteEntityTree(ccu().bypassDamperProfile.getEquipRef());
            hayStack.deleteEntityTree((bypassDamper.getId()));
            ccu().bypassDamperProfile = null;
            CcuLog.i(L.TAG_CCU_SYSTEM, "BypassDamper Equip deleted successfully");
        }
        else {
            CcuLog.i(L.TAG_CCU_SYSTEM, "BypassDamper Equip not found");
        }
    }


    public void deleteSystemConnectModule() {
        AdvancedAhuUtilKt.deleteSystemConnectModule();
    }


    public Map<a75f.io.domain.api.Point, PhysicalPoint> getLogicalPhysicalMap() {
        return new HashMap<a75f.io.domain.api.Point, PhysicalPoint>();
    }

    public boolean isVavSystemProfile() {
        if(ccu().systemProfile.getProfileName().contains("VAV")) {
            return true;
        }
        return false;
    }

    public boolean isLockoutActiveDuringUnoccupied() {
        return ((isCoolingLockoutActive() && getSystemController().getSystemState() == COOLING) ||
                (isHeatingLockoutActive() && getSystemController().getSystemState() == HEATING));
    }
}
