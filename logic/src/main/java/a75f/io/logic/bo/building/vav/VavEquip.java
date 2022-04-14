package a75f.io.logic.bo.building.vav;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.VOCLoop;
import a75.io.algos.tr.TrimResponseRequest;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ReheatType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.VavUnit;
import a75f.io.logic.bo.building.truecfm.TrueCFMConfigPoints;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TrueCFMTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTuners;
import a75f.io.logic.util.RxTask;
/**
 * Created by samjithsadasivan on 6/21/18.
 */

/**
 * It acts as a container of profile's PI controllers and TR Request objects and it interfaces profile with the haystack.
 * Current design requires only one equip/map per profile, but map/list of LogicalMap
 * per profile is maintained to support any requirement of adding multiple equips/devices per profile.
 */
public class VavEquip
{
    //TODO - Tuners
    int    integralMaxTimeout = 30;
    int proportionalSpread = 20;
    double proportionalGain = 0.2;
    double integralGain = 0.8;
    
    double      currentTemp;
    double      humidity;
    double desiredTemp;
    double supplyAirTemp;
    double dischargeTemp;
    double co2;
    double dischargeSp;
    
    double staticPressure;
    
    VavUnit             vavUnit;
    ControlLoop         coolingLoop;
    ControlLoop         heatingLoop;
    CO2Loop             co2Loop;
    VOCLoop             vocLoop;
    double voc;
    GenericPIController valveController;// Use GenericPI as we need unmodulated op.
    
    ControlLoop cfmController;
    
    public TrimResponseRequest satResetRequest;
    public TrimResponseRequest co2ResetRequest;
    public TrimResponseRequest spResetRequest;
    public TrimResponseRequest hwstResetRequest;
    
    int nodeAddr;
    ProfileType profileType;
    
    double co2Target = TunerConstants.ZONE_CO2_TARGET;
    double co2Threshold = TunerConstants.ZONE_CO2_THRESHOLD;
    double vocTarget = TunerConstants.ZONE_VOC_TARGET;
    double vocThreshold = TunerConstants.ZONE_VOC_THRESHOLD;
    CCUHsApi hayStack= CCUHsApi.getInstance();
    
    public VavEquip(ProfileType T, int node) {
        
        coolingLoop = new ControlLoop();
        heatingLoop = new ControlLoop();
        co2Loop = new CO2Loop();
        vocLoop = new VOCLoop();
        valveController = new GenericPIController();
        valveController.setIntegralMaxTimeout(integralMaxTimeout);
        valveController.setMaxAllowedError(proportionalSpread);
        valveController.setProportionalGain(proportionalGain);
        valveController.setIntegralGain(integralGain);
        
        satResetRequest = new TrimResponseRequest();
        co2ResetRequest = new TrimResponseRequest();
        spResetRequest = new TrimResponseRequest();
        hwstResetRequest = new TrimResponseRequest();
    
        profileType = T;
        switch (T) {
            case VAV_REHEAT:
                vavUnit = new VavUnit();
                break;
            case VAV_SERIES_FAN:
                vavUnit = new SeriesFanVavUnit();
                break;
            case VAV_PARALLEL_FAN:
                vavUnit = new ParallelFanVavUnit();
                break;
        }
        nodeAddr = node;
        
        vavUnit.vavDamper.minPosition = (int)getDamperLimit("cooling", "min");
        vavUnit.vavDamper.maxPosition = (int)getDamperLimit("cooling", "max");
        //createHaystackPoints();
    }
    
    public void init() {
        
        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddr + "\"");
        
        if (equipMap != null && equipMap.size() > 0)
        {
            String equipId = equipMap.get("id").toString();
            proportionalGain = TunerUtil.readTunerValByQuery("pgain and not cfm",equipId);
            integralGain = TunerUtil.readTunerValByQuery("igain and not cfm",equipId);
            proportionalSpread = (int) TunerUtil.readTunerValByQuery("pspread and not cfm",equipId);
            integralMaxTimeout = (int) TunerUtil.readTunerValByQuery("itimeout and not cfm",equipId);
            
            co2Target = (int) TunerUtil.readTunerValByQuery("zone and vav and co2 and target and equipRef == \""+equipId+"\"");
            co2Threshold = (int) TunerUtil.readTunerValByQuery("zone and vav and co2 and threshold and equipRef == \""+equipId+"\"");
            vocTarget = (int) TunerUtil.readTunerValByQuery("zone and vav and voc and target and equipRef == \""+equipId+"\"");
            vocThreshold = (int) TunerUtil.readTunerValByQuery("zone and vav and voc and threshold and equipRef == \""+equipId+"\"");
    
            initializeCfmController(equipId);
        }
    
        coolingLoop.setProportionalGain(proportionalGain);
        coolingLoop.setIntegralGain(integralGain);
        coolingLoop.setProportionalSpread(proportionalSpread);
        coolingLoop.setIntegralMaxTimeout(integralMaxTimeout);
        coolingLoop.reset();
    
        heatingLoop.setProportionalGain(proportionalGain);
        heatingLoop.setIntegralGain(integralGain);
        heatingLoop.setProportionalSpread(proportionalSpread);
        heatingLoop.setIntegralMaxTimeout(integralMaxTimeout);
        heatingLoop.reset();
    
        co2Loop.setCo2Target(co2Target);
        co2Loop.setCo2Threshold(co2Threshold);
        vocLoop.setVOCTarget(vocTarget);
        vocLoop.setVOCThreshold(vocThreshold);
    }
    
    private void initializeCfmController(String equipId) {
        double cfmProportionalGain = TunerUtil.readTunerValByQuery("pgain and cfm",equipId);
        double cfmIntegralGain = TunerUtil.readTunerValByQuery("igain and cfm",equipId);
        int cfmProportionalSpread = (int) TunerUtil.readTunerValByQuery("pspread and cfm",equipId);
        int cfmIntegralMaxTimeout = (int) TunerUtil.readTunerValByQuery("itimeout and cfm",equipId);
        
        cfmController.setProportionalGain(cfmProportionalGain);
        cfmController.setIntegralGain(cfmIntegralGain);
        cfmController.setProportionalSpread(cfmProportionalSpread);
        cfmController.setIntegralMaxTimeout(cfmIntegralMaxTimeout);
        cfmController.reset();
    }
    
    public void createHaystackPoints(VavProfileConfiguration config, String floor, String room) {
    
        //Create Logical points
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-VAV-"+nodeAddr;
        String ahuRef = null;
        HashMap systemEquip = CCUHsApi.getInstance().read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0) {
            ahuRef = systemEquip.get("id").toString();
        }
        boolean isElectric = config.reheatType == ReheatType.Pulse.ordinal() ||
                             config.reheatType == ReheatType.OneStage.ordinal() ||
                             config.reheatType == ReheatType.TwoStage.ordinal();
        
        Equip.Builder b = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(equipDis)
                          .setRoomRef(room)
                          .setFloorRef(floor)
                          .setProfile(profileType.name())
                          .setPriority(config.getPriority().name())
                          .addMarker("equip").addMarker("vav").addMarker("zone").addMarker("singleDuct").addMarker("pressureDependent")
                          .addMarker(isElectric ? "elecReheat" : "hotWaterReheat")
                          .setAhuRef(ahuRef)
                          .setTz(tz)
                          .setGroup(String.valueOf(nodeAddr));
        String fanMarker = "";
        if (profileType == ProfileType.VAV_SERIES_FAN) {
            b.addMarker("fanPowered").addMarker("series");
            fanMarker = "series";
        } else if (profileType == ProfileType.VAV_PARALLEL_FAN) {
            b.addMarker("fanPowered").addMarker("parallel");
            fanMarker = "parallel";
        }
        String equipRef = CCUHsApi.getInstance().addEquip(b.build());
    
        RxTask.executeAsync(() -> VavTuners.addVavEquipTuners( CCUHsApi.getInstance(),
                                                               siteRef,
                                                               equipDis,
                                                               equipRef,
                                                               room,
                                                               floor,
                                                               tz));
        
        createVavConfigPoints(config, equipRef, floor, room);
        
        List<HisItem> hisItems = new ArrayList<>();
        Point datPoint = new Point.Builder()
                                .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-dischargeAirTemp")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .setRoomRef(room)
                                .setFloorRef(floor).setHisInterpolate("cov")
                                .addMarker("discharge").addMarker("vav").addMarker(fanMarker)
                                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his").addMarker("cur").addMarker("logical").addMarker("zone")
                                .setGroup(String.valueOf(nodeAddr))
                                .setUnit("\u00B0F")
                                .setTz(tz)
                                .build();
        
        String datID = CCUHsApi.getInstance().addPoint(datPoint);
        hisItems.add(new HisItem(datID, new Date(System.currentTimeMillis()), 0.0));
        
        Point eatPoint = new Point.Builder()
                                .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-enteringAirTemp")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .setRoomRef(room)
                                .setFloorRef(floor).setHisInterpolate("cov")
                                .addMarker("entering").addMarker("vav").addMarker(fanMarker)
                                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his").addMarker("cur").addMarker("logical").addMarker("zone")
                                .setGroup(String.valueOf(nodeAddr))
                                .setUnit("\u00B0F")
                                .setTz(tz)
                                .build();
        String eatID = CCUHsApi.getInstance().addPoint(eatPoint);
        hisItems.add(new HisItem(eatID, new Date(System.currentTimeMillis()), 0.0));
        
        Point damperPos = new Point.Builder()
                                .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-damperPos")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .setRoomRef(room)
                                .setFloorRef(floor).setHisInterpolate("cov")
                                .addMarker("damper").addMarker("vav").addMarker(fanMarker)
                                .addMarker("base").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone")
                                .setGroup(String.valueOf(nodeAddr))
                                .setUnit("%")
                                .setTz(tz)
                                .build();
        String dpID = CCUHsApi.getInstance().addPoint(damperPos);
        hisItems.add(new HisItem(dpID, new Date(System.currentTimeMillis()), 0.0));
        
        Point normalizedDamperPos = new Point.Builder()
                                  .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-normalizedDamperPos")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(room)
                                  .setFloorRef(floor).setHisInterpolate("cov")
                                  .addMarker("damper").addMarker("vav").addMarker(fanMarker)
                                  .addMarker("normalized").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("%")
                                  .setTz(tz)
                                  .build();
        String normalizedDPId = CCUHsApi.getInstance().addPoint(normalizedDamperPos);
    
        Point reheatPos = new Point.Builder()
                                  .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-reheatPos")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(room)
                                  .setFloorRef(floor).setHisInterpolate("cov")
                                  .addMarker("reheat").addMarker("vav").addMarker(fanMarker)
                                  .addMarker("water").addMarker("valve").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("%")
                                  .setTz(tz)
                                  .build();
        String rhID = CCUHsApi.getInstance().addPoint(reheatPos);
        hisItems.add(new HisItem(rhID, new Date(System.currentTimeMillis()), 0.0));
        
        Point currentTemp = new Point.Builder()
                                  .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-currentTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(room)
                                  .setFloorRef(floor).setHisInterpolate("cov")
                                  .addMarker("zone").addMarker("vav").addMarker(fanMarker)
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
        hisItems.add(new HisItem(ctID, new Date(System.currentTimeMillis()), 0.0));
        
        Point humidity = new Point.Builder()
                                    .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-humidity")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(room)
                                    .setFloorRef(floor).setHisInterpolate("cov")
                                    .addMarker("zone").addMarker("vav").addMarker(fanMarker)
                                    .addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("%")
                                    .setTz(tz)
                                    .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);
        hisItems.add(new HisItem(humidityId, new Date(System.currentTimeMillis()), 0.0));
    
        Point co2 = new Point.Builder()
                                 .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-co2")
                                 .setEquipRef(equipRef)
                                 .setSiteRef(siteRef)
                                 .setRoomRef(room)
                                 .setFloorRef(floor).setHisInterpolate("cov")
                                 .addMarker("zone").addMarker("vav").addMarker(fanMarker)
                                 .addMarker("air").addMarker("co2").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                                 .setGroup(String.valueOf(nodeAddr))
                                 .setUnit("ppm")
                                 .setTz(tz)
                                 .build();
        String co2Id = CCUHsApi.getInstance().addPoint(co2);
        hisItems.add(new HisItem(co2Id, new Date(System.currentTimeMillis()), 0.0));
        
        Point voc = new Point.Builder()
                                 .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-voc")
                                 .setEquipRef(equipRef)
                                 .setSiteRef(siteRef)
                                 .setRoomRef(room)
                                 .setFloorRef(floor).setHisInterpolate("cov")
                                 .addMarker("zone").addMarker("vav").addMarker(fanMarker)
                                 .addMarker("air").addMarker("voc").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                                 .setGroup(String.valueOf(nodeAddr))
                                 .setUnit("ppb")
                                 .setTz(tz)
                                 .build();
        String vocId = CCUHsApi.getInstance().addPoint(voc);
        hisItems.add(new HisItem(vocId, new Date(System.currentTimeMillis()), 0.0));
        
        Point desiredTemp = new Point.Builder()
                                           .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-desiredTemp")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(room)
                                           .setFloorRef(floor).setHisInterpolate("cov")
                                           .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("vav").addMarker(fanMarker)
                                           .addMarker("average").addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        String dtId = CCUHsApi.getInstance().addPoint(desiredTemp);
        
        Point desiredTempCooling = new Point.Builder()
                                    .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-desiredTempCooling")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(room)
                                    .setFloorRef(floor).setHisInterpolate("cov")
                                    .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("vav").addMarker(fanMarker)
                                    .addMarker("cooling").addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        String dtCoolingId = CCUHsApi.getInstance().addPoint(desiredTempCooling);
        
        Point desiredTempHeating = new Point.Builder()
                                           .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-desiredTempHeating")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(room)
                                           .setFloorRef(floor).setHisInterpolate("cov")
                                           .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("vav").addMarker(fanMarker)
                                           .addMarker("heating").addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        String dtHeatingId = CCUHsApi.getInstance().addPoint(desiredTempHeating);
        
        Point heatingLoopOp = new Point.Builder()
                                    .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-heatingLoopOp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(room)
                                    .setFloorRef(floor).setHisInterpolate("cov")
                                    .addMarker("heating").addMarker("loop").addMarker("sp").addMarker("his").addMarker("vav").addMarker(fanMarker)
                                    .addMarker("zone")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("%")
                                    .setTz(tz)
                                    .build();
        CCUHsApi.getInstance().addPoint(heatingLoopOp);
    
        Point coolingLoopOp = new Point.Builder()
                                      .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-coolingLoopOp")
                                      .setEquipRef(equipRef)
                                      .setSiteRef(siteRef)
                                      .setRoomRef(room)
                                      .setFloorRef(floor).setHisInterpolate("cov")
                                      .addMarker("cooling").addMarker("loop").addMarker("sp").addMarker("his").addMarker("vav").addMarker(fanMarker)
                                      .addMarker("zone")
                                      .setGroup(String.valueOf(nodeAddr))
                                      .setUnit("%")
                                      .setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(coolingLoopOp);
    
        Point dischargeSp = new Point.Builder()
                                      .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-dischargeSp")
                                      .setEquipRef(equipRef)
                                      .setSiteRef(siteRef)
                                      .setRoomRef(room)
                                      .setFloorRef(floor).setHisInterpolate("cov")
                                      .addMarker("discharge").addMarker("air").addMarker("temp").addMarker("zone")
                                      .addMarker("sp").addMarker("his").addMarker("vav").addMarker(fanMarker)
                                      .setGroup(String.valueOf(nodeAddr))
                                      .setUnit("\u00B0F")
                                      .setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(dischargeSp);
    
        Point satRequestPercentage = new Point.Builder()
                                    .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-satRequestPercentage")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(room)
                                    .setFloorRef(floor).setHisInterpolate("cov")
                                    .addMarker("request").addMarker("hour").addMarker("cumulative").addMarker("vav").addMarker(fanMarker)
                                    .addMarker("tr").addMarker("supply").addMarker("air").addMarker("temp").addMarker("his").addMarker("zone")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("%")
                                    .setTz(tz)
                                    .build();
        CCUHsApi.getInstance().addPoint(satRequestPercentage);
    
        Point co2RequestPercentage = new Point.Builder()
                                             .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-co2RequestPercentage")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(room)
                                             .setFloorRef(floor).setHisInterpolate("cov")
                                             .addMarker("request").addMarker("hour").addMarker("cumulative").addMarker("vav").addMarker(fanMarker)
                                             .addMarker("tr").addMarker("co2").addMarker("temp").addMarker("his").addMarker("zone")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("%")
                                             .setTz(tz)
                                             .build();
        CCUHsApi.getInstance().addPoint(co2RequestPercentage);
    
        Point pressureRequestPercentage = new Point.Builder()
                                             .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-staticRequestPercentage")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(room)
                                             .setFloorRef(floor).setHisInterpolate("cov")
                                             .addMarker("request").addMarker("hour").addMarker("cumulative").addMarker("vav").addMarker(fanMarker)
                                             .addMarker("tr").addMarker("staticPressure").addMarker("his").addMarker("zone")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("%")
                                             .setTz(tz)
                                             .build();
        CCUHsApi.getInstance().addPoint(pressureRequestPercentage);
    
        Point satCurrentRequest = new Point.Builder()
                                          .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-satCurrentRequest")
                                          .setEquipRef(equipRef)
                                          .setSiteRef(siteRef)
                                          .setRoomRef(room)
                                          .setFloorRef(floor).setHisInterpolate("cov")
                                          .addMarker("sat").addMarker("current").addMarker("request").addMarker("tr").addMarker("sp").addMarker("his")
                                          .addMarker("vav").addMarker(fanMarker).addMarker("zone")
                                          .setGroup(String.valueOf(nodeAddr))
                                          .setTz(tz)
                                          .build();
        CCUHsApi.getInstance().addPoint(satCurrentRequest);
    
        Point co2CurrentRequest = new Point.Builder()
                                          .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-co2CurrentRequest")
                                          .setEquipRef(equipRef)
                                          .setSiteRef(siteRef)
                                          .setRoomRef(room)
                                          .setFloorRef(floor).setHisInterpolate("cov")
                                          .addMarker("co2").addMarker("current").addMarker("request").addMarker("tr").addMarker("sp").addMarker("his")
                                          .addMarker("vav").addMarker(fanMarker).addMarker("zone")
                                          .setGroup(String.valueOf(nodeAddr))
                                          .setTz(tz)
                                          .build();
        CCUHsApi.getInstance().addPoint(co2CurrentRequest);
    
        Point spCurrentRequest = new Point.Builder()
                                         .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-spCurrentRequest")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(room)
                                         .setFloorRef(floor).setHisInterpolate("cov")
                                         .addMarker("staticPressure").addMarker("current").addMarker("request").addMarker("tr").addMarker("sp")
                                         .addMarker("his").addMarker("vav").addMarker(fanMarker).addMarker("zone")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        CCUHsApi.getInstance().addPoint(spCurrentRequest);
    
        Point equipStatus = new Point.Builder()
                                  .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-equipStatus")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(room)
                                  .setFloorRef(floor).setHisInterpolate("cov")
                                  .addMarker("status").addMarker("vav").addMarker(fanMarker).addMarker("his").addMarker("zone")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setEnums("deadband,cooling,heating,tempdead")
                                  .setTz(tz)
                                  .build();
        String equipStatusId = CCUHsApi.getInstance().addPoint(equipStatus);
        hisItems.add(new HisItem(equipStatusId, new Date(System.currentTimeMillis()), 0.0));
        
        Point equipStatusMessage = new Point.Builder()
                                    .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-equipStatusMessage")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(room)
                                    .setFloorRef(floor)
                                    .addMarker("status").addMarker("message").addMarker("vav").addMarker(fanMarker)
                                    .addMarker("writable").addMarker("zone")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setTz(tz)
                                    .setKind(Kind.STRING)
                                    .build();
        String equipStatusMessageLd = CCUHsApi.getInstance().addPoint(equipStatusMessage);
        Point equipScheduleStatus = new Point.Builder()
                                    .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-equipScheduleStatus")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(room)
                                    .setFloorRef(floor).setHisInterpolate("cov")
                                    .addMarker("vav").addMarker(fanMarker).addMarker("scheduleStatus")
                                    .addMarker("zone").addMarker("writable").addMarker("his")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setTz(tz)
                                    .setKind(Kind.STRING)
                                    .build();
        String equipScheduleStatusId = CCUHsApi.getInstance().addPoint(equipScheduleStatus);
    
        Point occupancy = new Point.Builder()
                                    .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-occupancy")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(room)
                                    .setFloorRef(floor).setHisInterpolate("cov")
                                    .addMarker("vav").addMarker(fanMarker).addMarker("occupancy").addMarker("mode")
                                    .addMarker("zone").addMarker("his")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setEnums("unoccupied,occupied,preconditioning,forcedoccupied,vacation," +
                                            "occupancysensing,autoforceoccupy,autoaway")
                                    .setTz(tz)
                                    .build();
        String occupancyId = CCUHsApi.getInstance().addPoint(occupancy);
        hisItems.add(new HisItem(occupancyId, new Date(System.currentTimeMillis()), 0.0));
        
        Point equipScheduleType = new Point.Builder()
                                           .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-scheduleType")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(room)
                                           .setFloorRef(floor).setHisInterpolate("cov")
                                           .addMarker("zone").addMarker("vav").addMarker(fanMarker)
                                           .addMarker("scheduleType").addMarker("writable").addMarker("his")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setTz(tz)
                                           .setEnums("building,zone,named")
                                           .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        hisItems.add(new HisItem(equipScheduleTypeId, new Date(System.currentTimeMillis()), 0.0));

        Point zoneDynamicPriorityPoint = new Point.Builder()
                .setDisplayName(equipDis+"-zoneDynamicPriority")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("vav").addMarker(fanMarker).addMarker("zone").addMarker("dynamic")
                .addMarker("priority").addMarker("writable").addMarker("sp").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String zoneDynamicPriorityPointID = CCUHsApi.getInstance().addPoint(zoneDynamicPriorityPoint);
        hisItems.add(new HisItem(equipStatusId, new Date(System.currentTimeMillis()), 10.0));

        Point airflowCfm = new Point.Builder()
                .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-airflowCfm")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("vav").addMarker("cmd").addMarker("cfm").addMarker(fanMarker)
                .addMarker("airflow").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .build();
        CCUHsApi.getInstance().addPoint(airflowCfm);

        Point flowVelocity = new Point.Builder()
                .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-flowVelocity")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("vav").addMarker("flow").addMarker("velocity").addMarker(fanMarker)
                .addMarker("sp").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .build();
        CCUHsApi.getInstance().addPoint(flowVelocity);

        Point pressure = new Point.Builder()
                .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-pressure")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("pressure").addMarker("his").addMarker("sensor").addMarker(fanMarker)
                .setGroup(String.valueOf(nodeAddr))
                .build();
        CCUHsApi.getInstance().addPoint(pressure);
        
        String heartBeatId = CCUHsApi.getInstance().addPoint(HeartBeat.getHeartBeatPoint(equipDis, equipRef,
                siteRef, room, floor, nodeAddr, "vav", tz, false));
        
        //Create Physical points and map
        SmartNode device = new SmartNode(nodeAddr, siteRef, floor, room, equipRef);
        device.th1In.setPointRef(datID);
        device.th1In.setEnabled(true);
        device.th2In.setPointRef(eatID);
        device.th2In.setEnabled(true);
        device.analog1Out.setPointRef(normalizedDPId);
        //device.analog1Out.setEnabled(true);
        device.analog2Out.setPointRef(rhID);
        device.relay1.setPointRef(rhID);
        device.rssi.setPointRef(heartBeatId);
        device.rssi.setEnabled(true);


        if (profileType != ProfileType.VAV_REHEAT) {
            createFanTuner(siteDis, equipRef, siteRef, floor, room, tz);
            String fanPointId = createFanOutPoint(siteDis, equipRef, siteRef, floor, room, tz, fanMarker);
            device.relay2.setPointRef(fanPointId);
        } else {
            device.relay2.setPointRef(rhID);
        }
        //device.analog2Out.setEnabled(true);
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        device.desiredTemp.setPointRef(dtId);
        device.desiredTemp.setEnabled(true);
        
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case ANALOG_OUT_ONE:
                    device.analog1Out.setType(op.getAnalogActuatorType());
                    break;
                case ANALOG_OUT_TWO:
                    device.analog2Out.setType(op.getAnalogActuatorType());
                    break;
                case RELAY_ONE:
                    device.relay1.setType(op.getRelayActuatorType());
                    break;
                case RELAY_TWO:
                    device.relay2.setType(op.getRelayActuatorType());
                    break;
            }
        }
        device.analog1Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_ONE));
        device.analog2Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_TWO));
        device.relay1.setEnabled(config.isOpConfigured(Port.RELAY_ONE));
        device.relay2.setEnabled(config.isOpConfigured(Port.RELAY_TWO));
        device.addPointsToDb();
        
        device.addSensor(Port.SENSOR_RH, humidityId);
        device.addSensor(Port.SENSOR_CO2, co2Id);
        device.addSensor(Port.SENSOR_VOC, vocId);
        
        //Initialize write array for points, otherwise a read before write will throw exception
        setDesiredTempCooling(74.0);
        setDesiredTemp(72.0);
        setDesiredTempHeating(70.0);
        setScheduleStatus("");
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(hisItems);
    }
    
    private String createFanOutPoint(String siteDis,
                                   String equipRef,
                                   String siteRef,
                                   String floorRef,
                                   String roomRef,
                                   String tz,
                                   String fanType
    ) {
        Point fan = new Point.Builder()
                              .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-"+fanType+"Fan")
                              .setEquipRef(equipRef)
                              .setSiteRef(siteRef)
                              .setRoomRef(roomRef)
                              .setFloorRef(floorRef).setHisInterpolate("cov")
                              .addMarker("vav").addMarker("logical").addMarker("zone").addMarker("his")
                              .addMarker(fanType).addMarker("fan").addMarker("cmd").addMarker("his")
                              .setGroup(String.valueOf(nodeAddr))
                              .setEnums("Off, On")
                              .setTz(tz)
                              .build();
        String fanId = CCUHsApi.getInstance().addPoint(fan);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(fanId, 0.0);
        return fanId;
    }
    
    private String getFanMarker() {
    
        if (profileType == ProfileType.VAV_SERIES_FAN) {
            return Tags.SERIES;
        } else if (profileType == ProfileType.VAV_PARALLEL_FAN) {
            return Tags.PARALLEL;
        }
        return "";
    }

    private void createFanTuner(String siteDis,
                                   String equipRef,
                                   String siteRef,
                                   String floorRef,
                                   String roomRef,
                                   String tz
    ) {
        Point fanControlOnFixedTimeDelay  = new Point.Builder()
                                                .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-"+"fanControlOnFixedTimeDelay ")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef)
                                                .setRoomRef(roomRef)
                                                .setFloorRef(floorRef).setHisInterpolate("cov")
                                                .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                                .addMarker("fan").addMarker("control").addMarker("time").addMarker("delay").addMarker("sp")
                                                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                .setUnit("m")
                                                .setTz(tz)
                                                .build();
        String fanControlOnFixedTimeDelayId = CCUHsApi.getInstance().addPoint(fanControlOnFixedTimeDelay);

        HashMap<Object, Object> fanControlOnFixedTimeDelayPoint = CCUHsApi.getInstance()
                                                                          .read("tuner and default and fan and " +
                                                                                "control and time and delay");
        ArrayList<HashMap> fanControlOnFixedTimeDelayPointArr =
            CCUHsApi.getInstance().readPoint(fanControlOnFixedTimeDelayPoint.get("id").toString());
        for (HashMap valMap : fanControlOnFixedTimeDelayPointArr) {
            if (valMap.get("val") != null) {
                CCUHsApi.getInstance().pointWrite(HRef.copy(fanControlOnFixedTimeDelayId),
                             (int) Double.parseDouble(valMap.get("level").toString()),
                                                       valMap.get("who").toString(),
                                                       HNum.make(Double.parseDouble(valMap.get("val").toString())),
                                                       HNum.make(0));
            }
        }

        CCUHsApi.getInstance().writeDefaultValById(fanControlOnFixedTimeDelayId, 1.0);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(fanControlOnFixedTimeDelayId, 1.0);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(fanControlOnFixedTimeDelayId, HSUtil.getPriorityVal(fanControlOnFixedTimeDelayId));
    }

    public void createVavConfigPoints(VavProfileConfiguration config, String equipRef, String floor, String room) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis+"-VAV-"+nodeAddr;
        String tz = siteMap.get("tz").toString();
    
        String fanMarker = "";
        if (profileType == ProfileType.VAV_SERIES_FAN) {
            fanMarker = "series";
        } else if (profileType == ProfileType.VAV_PARALLEL_FAN) {
            fanMarker = "parallel";
        }
        Point damperType = new Point.Builder()
                                         .setDisplayName(equipDis+"-damperType")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(room)
                                         .setFloorRef(floor)
                                         .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("writable").addMarker("zone")
                                         .addMarker("damper").addMarker("type").addMarker("sp")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setEnums("ZeroToTenV,TwoToTenV,TenToTwov,TenToZeroV,mat")
                                         .setTz(tz)
                                         .build();
        String damperTypeId = CCUHsApi.getInstance().addPoint(damperType);
        CCUHsApi.getInstance().writeDefaultValById(damperTypeId, (double)config.damperType);
    
        Point damperSize = new Point.Builder()
                                   .setDisplayName(equipDis+"-damperSize")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef(room)
                                   .setFloorRef(floor)
                                   .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("writable").addMarker("zone")
                                   .addMarker("damper").addMarker("size").addMarker("sp")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz)
                                   .build();
        String damperSizeId = CCUHsApi.getInstance().addPoint(damperSize);
        CCUHsApi.getInstance().writeDefaultValById(damperSizeId, (double)config.damperSize);
    
        Point damperShape = new Point.Builder()
                                   .setDisplayName(equipDis+"-damperShape")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef(room)
                                   .setFloorRef(floor)
                                   .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("writable").addMarker("zone")
                                   .addMarker("damper").addMarker("shape").addMarker("sp")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setEnums("round,square,rectangular")
                                   .setTz(tz)
                                   .build();
        String damperShapeId = CCUHsApi.getInstance().addPoint(damperShape);
        CCUHsApi.getInstance().writeDefaultValById(damperShapeId, (double)config.damperShape);
    
        Point.Builder reheatTypeBuilder = new Point.Builder()
                                   .setDisplayName(equipDis+"-reheatType")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef(room)
                                   .setFloorRef(floor)
                                   .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("writable").addMarker("zone")
                                   .addMarker("reheat").addMarker("type").addMarker("sp")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz);
        if (profileType == ProfileType.VAV_REHEAT) {
            reheatTypeBuilder.setEnums("ZeroToTenV,TwoToTenV,TenToTwoV,TenToZeroV,Pulse,OneStage,TwoStage");
        } else {
            reheatTypeBuilder.setEnums("0-10V, 2-10V, 10-2V, 10-0V , PulsedElectric, StagedElectric");
        }
        String reheatTypeId = CCUHsApi.getInstance().addPoint(reheatTypeBuilder.build());
        CCUHsApi.getInstance().writeDefaultValById(reheatTypeId, (double)config.reheatType);
    
        Point enableOccupancyControl = new Point.Builder()
                                   .setDisplayName(equipDis+"-enableOccupancyControl")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef(room)
                                   .setFloorRef(floor).setHisInterpolate("cov")
                                   .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("writable").addMarker("zone")
                                   .addMarker("enable").addMarker("occupancy").addMarker("control").addMarker("sp").addMarker("his")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setEnums("false,true")
                                   .setTz(tz)
                                   .build();
        String enableOccupancyControlId = CCUHsApi.getInstance().addPoint(enableOccupancyControl);
        CCUHsApi.getInstance().writeDefaultValById(enableOccupancyControlId, config.enableOccupancyControl == true ? 1.0 :0);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(enableOccupancyControlId, config.enableOccupancyControl == true ? 1.0 :0);
    
        Point enableCO2Control = new Point.Builder()
                                               .setDisplayName(equipDis+"-enableCO2Control")
                                               .setEquipRef(equipRef)
                                               .setSiteRef(siteRef)
                                               .setRoomRef(room)
                                               .setFloorRef(floor).setHisInterpolate("cov")
                                               .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("writable").addMarker("zone")
                                               .addMarker("enable").addMarker("co2").addMarker("control").addMarker("sp").addMarker("his")
                                               .setGroup(String.valueOf(nodeAddr))
                                               .setEnums("false,true")
                                               .setTz(tz)
                                               .build();
        String enableCO2ControlId = CCUHsApi.getInstance().addPoint(enableCO2Control);
        CCUHsApi.getInstance().writeDefaultValById(enableCO2ControlId, config.enableCO2Control == true ? 1.0 :0);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(enableCO2ControlId, config.enableCO2Control == true ? 1.0 :0);
    
        Point enableIAQControl = new Point.Builder()
                                               .setDisplayName(equipDis+"-enableIAQControl")
                                               .setEquipRef(equipRef)
                                               .setSiteRef(siteRef)
                                               .setRoomRef(room)
                                               .setFloorRef(floor).setHisInterpolate("cov")
                                               .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("writable").addMarker("zone")
                                               .addMarker("enable").addMarker("iaq").addMarker("control").addMarker("sp").addMarker("his")
                                               .setGroup(String.valueOf(nodeAddr))
                                               .setEnums("false,true")
                                               .setTz(tz)
                                               .build();
        String enableIAQControlId = CCUHsApi.getInstance().addPoint(enableIAQControl);
        CCUHsApi.getInstance().writeDefaultValById(enableIAQControlId, config.enableIAQControl == true ? 1.0 :0);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(enableIAQControlId, config.enableIAQControl == true ? 1.0 :0);
    
        Point zonePriority = new Point.Builder()
                                         .setDisplayName(equipDis+"-zonePriority")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(room)
                                         .setFloorRef(floor).setHisInterpolate("cov")
                                         .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("writable").addMarker("zone")
                                         .addMarker("priority").addMarker("userIntent").addMarker("sp").addMarker("his")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setEnums("none,low,normal,high")
                                         .setTz(tz)
                                         .build();
        String zonePriorityId = CCUHsApi.getInstance().addPoint(zonePriority);
        CCUHsApi.getInstance().writeDefaultValById(zonePriorityId, (double)config.getPriority().ordinal());
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(zonePriorityId, (double)config.getPriority().ordinal());
    
        Point temperatureOffset = new Point.Builder()
                                     .setDisplayName(equipDis+"-temperatureOffset")
                                     .setEquipRef(equipRef)
                                     .setSiteRef(siteRef)
                                     .setRoomRef(room)
                                     .setFloorRef(floor)
                                     .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("writable").addMarker("zone")
                                     .addMarker("temperature").addMarker("offset").addMarker("sp")
                                     .setGroup(String.valueOf(nodeAddr))
                                     .setUnit("\u00B0F")
                                     .setTz(tz)
                                     .build();
        String temperatureOffsetId = CCUHsApi.getInstance().addPoint(temperatureOffset);
        CCUHsApi.getInstance().writeDefaultValById(temperatureOffsetId, config.temperaturOffset);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(temperatureOffsetId, config.temperaturOffset);
    
        //Damper max heating is required irrespective of trueCfm is enabled or not.
        Point damperMaxHeating = new Point.Builder()
                                         .setDisplayName(equipDis+"-maxHeatingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(room)
                                         .setFloorRef(floor).setHisInterpolate("cov")
                                         .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("damper").addMarker("max")
                                         .addMarker("heating").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setUnit("%")
                                         .setTz(tz)
                                         .build();
        String damperMaxHeatingId = CCUHsApi.getInstance().addPoint(damperMaxHeating);
        CCUHsApi.getInstance().writeDefaultValById(damperMaxHeatingId, (double) config.maxDamperHeating);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(damperMaxHeatingId, (double) config.maxDamperHeating);
    
        Equip equip = HSUtil.getEquipInfo(equipRef);
        TrueCFMConfigPoints.createTrueCFMControlPoint(hayStack, equip, Tags.VAV,
                                                      config.enableCFMControl ? 1.0 : 0);
    
        if (config.enableCFMControl) {
            TrueCFMConfigPoints.createTrueCFMVavConfigPoints( hayStack, equipRef,  config);
            TrueCFMTuners.createTrueCfmTuners(hayStack,siteRef,equipDis,equipRef,room,floor,tz,TunerConstants.VAV_TAG,TunerConstants.VAV_TUNER_GROUP);
        } else {
            createNonCfmDamperConfigPoints(hayStack, equip, config, fanMarker);
        }
        
    }
    
    /**
     * Create damperSize configuration points when TrueCfm is disabled.
     * @param hayStack
     * @param equip
     * @param config
     * @param fanMarker
     */
    public void createNonCfmDamperConfigPoints(CCUHsApi hayStack, Equip equip, VavProfileConfiguration config,
                                               String fanMarker) {
        Point damperMinCooling = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName()+"-minCoolingDamperPos")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("damper").addMarker("min")
                                     .addMarker("cooling").addMarker("pos")
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setGroup(String.valueOf(nodeAddr))
                                     .setUnit("%")
                                     .setTz(equip.getTz())
                                     .build();
        String damperMinCoolingId = hayStack.addPoint(damperMinCooling);
        hayStack.writeDefaultValById(damperMinCoolingId, (double)config.minDamperCooling);
        hayStack.writeHisValueByIdWithoutCOV(damperMinCoolingId, (double)config.minDamperCooling);
    
        Point damperMaxCooling = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName()+"-maxCoolingDamperPos")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("damper").addMarker("max")
                                     .addMarker("cooling").addMarker("pos")
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setGroup(String.valueOf(nodeAddr))
                                     .setUnit("%")
                                     .setTz(equip.getTz())
                                     .build();
        String damperMaxCoolingId = hayStack.addPoint(damperMaxCooling);
        hayStack.writeDefaultValById(damperMaxCoolingId, (double)config.maxDamperCooling);
        hayStack.writeHisValueByIdWithoutCOV(damperMaxCoolingId, (double)config.maxDamperCooling);
    
        Point damperMinHeating = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName()+"-minHeatingDamperPos")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker("vav").addMarker(fanMarker).addMarker("damper").addMarker("min")
                                     .addMarker("heating").addMarker("pos")
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setGroup(String.valueOf(nodeAddr))
                                     .setUnit("%")
                                     .setTz(equip.getTz())
                                     .build();
        String damperMinHeatingId = hayStack.addPoint(damperMinHeating);
        hayStack.writeDefaultValById(damperMinHeatingId, (double)config.minDamperHeating);
        hayStack.writeHisValueByIdWithoutCOV(damperMinHeatingId, (double)config.minDamperHeating);
    }
    
    public void deleteNonCfmDamperPoints(CCUHsApi hayStack, String equipRef) {
        HashMap<Object, Object> damperMinCooling = hayStack.readEntity("config and min and damper and pos and " +
                                                                       "cooling and equipRef == \""+equipRef+"\"");
        if (!damperMinCooling.isEmpty()) {
            hayStack.deleteWritablePoint(damperMinCooling.get("id").toString());
        }
    
        HashMap<Object, Object> damperMaxCooling = hayStack.readEntity("config and max and damper and pos and " +
                                                                       "cooling and equipRef == \""+equipRef+"\"");
        if (!damperMaxCooling.isEmpty()) {
            hayStack.deleteWritablePoint(damperMaxCooling.get("id").toString());
        }
    
        HashMap<Object, Object> damperMinHeating = hayStack.readEntity("config and min and damper and pos and " +
                                                                       "heating and equipRef == \""+equipRef+"\"");
        if (!damperMinHeating.isEmpty()) {
            hayStack.deleteWritablePoint(damperMinHeating.get("id").toString());
        }
    }
    
    public void setConfigNumVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and vav and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public double getConfigNumVal(String tags) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and vav and "+tags+" and group == \""+nodeAddr+"\"");
    }
    
    public void setConfigStrVal(String tags,String val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and vav and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public String getConfigStrVal(String tags) {
        return CCUHsApi.getInstance().readDefaultStrVal("point and zone and config and vav and "+tags+" and group == \""+nodeAddr+"\"");
    }


    public void updateHaystackPoints(VavProfileConfiguration config,String floorRef, String roomRef)  {
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case ANALOG_OUT_ONE:
                case ANALOG_OUT_TWO:
                    CcuLog.d(L.TAG_CCU_ZONE," Update analog" + op.getPort() + " type " + op.getAnalogActuatorType());
                    SmartNode.updatePhysicalPointType(nodeAddr, op.getPort().toString(), op.getAnalogActuatorType());
                    break;
                case RELAY_ONE:
                case RELAY_TWO:
                    SmartNode.updatePhysicalPointType(nodeAddr, op.getPort().toString(), op.getRelayActuatorType());
                    break;
            }
        }
        
        SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_OUT_TWO.name(), config.isOpConfigured(Port.ANALOG_OUT_TWO) );
        SmartNode.setPointEnabled(nodeAddr, Port.RELAY_ONE.name(), config.isOpConfigured(Port.RELAY_ONE) );
        SmartNode.setPointEnabled(nodeAddr, Port.RELAY_TWO.name(), config.isOpConfigured(Port.RELAY_TWO) );
    
        handleTrueCfmConfiguration(config);
        
        setConfigNumVal("damper and type",config.damperType);
        setConfigNumVal("damper and size",config.damperSize);
        setConfigNumVal("damper and shape",config.damperShape);
        setConfigNumVal("reheat and type",config.reheatType);
        setConfigNumVal("enable and occupancy",config.enableOccupancyControl == true ? 1.0 : 0);
        setHisVal("enable and occupancy",config.enableOccupancyControl == true ? 1.0 : 0);
        setConfigNumVal("enable and co2",config.enableCO2Control == true ? 1.0 : 0);
        setHisVal("enable and co2",config.enableCO2Control == true ? 1.0 : 0);
        setConfigNumVal("enable and iaq",config.enableIAQControl == true ? 1.0 : 0);
        setHisVal("enable and iaq",config.enableIAQControl == true ? 1.0 : 0);
        setConfigNumVal("priority",config.getPriority().ordinal());
        setHisVal("priority",config.getPriority().ordinal());
        setConfigNumVal("temperature and offset",config.temperaturOffset);
        
        setDamperLimit("heating","max",config.maxDamperHeating);
        setHisVal("heating and max and damper and pos",config.maxDamperHeating);
        
        if (config.enableCFMControl) {
            setConfigNumVal("min and cfm and cooling", config.numMinCFMCooling);
            setHisVal("min and cfm and cooling", config.numMinCFMCooling);
            setConfigNumVal("max and cfm and cooling", config.nuMaxCFMCooling);
            setHisVal("max and cfm and cooling", config.nuMaxCFMCooling);
            setConfigNumVal("max and cfm and heating", config.numMaxCFMReheating);
            setConfigNumVal("min and cfm and heating", config.numMinCFMReheating);
            setConfigNumVal("cfm and vav and config and kfactor", config.kFactor);
            setHisVal("cfm and vav and config and kfactor", config.kFactor);
            setConfigNumVal("cfm and enabled ", config.enableCFMControl ? 1.0 : 0);
            setHisVal("cfm and enabled ", config.enableCFMControl ? 1.0 : 0);
        } else {
            setDamperLimit("cooling","min",config.minDamperCooling);
            setHisVal("cooling and min and damper and pos",config.minDamperCooling);
            setDamperLimit("cooling","max",config.maxDamperCooling);
            setHisVal("cooling and max and damper and pos",config.maxDamperCooling);
            setDamperLimit("heating","min",config.minDamperHeating);
            setHisVal("heating and min and damper and pos",config.minDamperHeating);
        }
    }
    
    private void handleTrueCfmConfiguration(VavProfileConfiguration config) {
    
        HashMap<Object, Object> equipMap = hayStack.readEntity("equip and group== \"" + nodeAddr + "\"");
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
    
        String fanMarker = getFanMarker();
        boolean curTrueCfmEnabled = getConfigNumVal("cfm and enabled") > 0;
        if (curTrueCfmEnabled && !config.enableCFMControl) {
            TrueCFMConfigPoints.deleteTrueCFMPoints(hayStack, equip.getId());
            createNonCfmDamperConfigPoints(hayStack, equip, config, fanMarker);
        } else if (!curTrueCfmEnabled && config.enableCFMControl) {
            TrueCFMConfigPoints.createTrueCFMVavConfigPoints( hayStack, equip.getId(), config);
            TrueCFMTuners.createTrueCfmTuners(hayStack,
                                              equip.getSiteRef(),
                                              equip.getDisplayName(),
                                              equip.getId(),
                                              equip.getRoomRef(),
                                              equip.getFloorRef(),
                                              equip.getTz(),
                                              Tags.VAV,TunerConstants.VAV_TUNER_GROUP);
            deleteNonCfmDamperPoints(hayStack, equip.getId());
        }
    }
    
    public void setHisVal(String tags,double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and zone and config and vav and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public VavProfileConfiguration getProfileConfiguration() {
        VavProfileConfiguration config = new VavProfileConfiguration();
        config.minDamperCooling = ((int)getDamperLimit("cooling","min"));
        config.maxDamperCooling = ((int)getDamperLimit("cooling","max"));
        config.minDamperHeating = ((int)getDamperLimit("heating","min"));
        config.maxDamperHeating = ((int)getDamperLimit("heating","max"));
    
        config.damperType = (int)getConfigNumVal("damper and type");
        config.damperSize = (int)getConfigNumVal("damper and size");
        config.damperShape = (int)getConfigNumVal("damper and shape");
        config.reheatType = (int)getConfigNumVal("reheat and type");
        config.enableOccupancyControl = getConfigNumVal("enable and occupancy") > 0 ? true : false ;
        config.enableCO2Control = getConfigNumVal("enable and co2") > 0 ? true : false ;
        config.enableIAQControl = getConfigNumVal("enable and iaq") > 0 ? true : false ;
        //config.setPriority(ZonePriority.values()[(int)getConfigNumVal("priority")]);
        config.setPriority(ZonePriority.values()[(int) getZonePriorityValue()]);
        config.temperaturOffset = getConfigNumVal("temperature and offset");
        config.numMinCFMCooling=(int)getConfigNumVal("min and cfm and cooling");
        config.nuMaxCFMCooling= (int) getConfigNumVal("max and cfm and cooling");
        config.numMaxCFMReheating=(int)getConfigNumVal("max and cfm and heating");
        config.numMinCFMReheating=(int)getConfigNumVal("min and cfm and heating");
        config.enableCFMControl = getConfigNumVal("cfm and enabled") > 0;
        config.kFactor=getConfigNumVal("cfm and vav and config and kfactor");
        
        config.setNodeType(NodeType.SMART_NODE);//TODO - revisit
        
        
        RawPoint a1 = SmartNode.getPhysicalPoint(nodeAddr, Port.ANALOG_OUT_ONE.toString());
        if (a1 != null && a1.getEnabled()) {
            Output analogOne = new Output();
            analogOne.setAddress((short)nodeAddr);
            analogOne.setPort(Port.ANALOG_OUT_ONE);
            analogOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(a1.getType());
            config.getOutputs().add(analogOne);
        }
    
        RawPoint a2 = SmartNode.getPhysicalPoint(nodeAddr, Port.ANALOG_OUT_TWO.toString());
        if (a2 != null  && a2.getEnabled()) {
            Output analogTwo = new Output();
            analogTwo.setAddress((short)nodeAddr);
            analogTwo.setPort(Port.ANALOG_OUT_TWO);
            Log.d("CCU"," Get analog out 2 type "+a2.getType());
            analogTwo.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(a2.getType());
            config.getOutputs().add(analogTwo);
        }
    
        RawPoint r1 = SmartNode.getPhysicalPoint(nodeAddr, Port.RELAY_ONE.toString());
        if (r1 != null && r1.getEnabled()) {
            Output relay1 = new Output();
            relay1.setAddress((short)nodeAddr);
            relay1.setPort(Port.RELAY_ONE);
            relay1.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r1.getType());
            config.getOutputs().add(relay1);
        }
    
        RawPoint r2 = SmartNode.getPhysicalPoint(nodeAddr, Port.RELAY_TWO.toString());
        if (r2 != null && r2.getEnabled()) {
            Output relay2 = new Output();
            relay2.setAddress((short)nodeAddr);
            relay2.setPort(Port.RELAY_TWO);
            relay2.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r2.getType());
            config.getOutputs().add(relay2);
        }
        return config;
    }

    public double getCurrentTemp()
    {
        currentTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and group == \""+nodeAddr+"\"");
        return currentTemp;
    }
    public void setCurrentTemp(double roomTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and current and group == \""+nodeAddr+"\"", roomTemp);
        this.currentTemp = roomTemp;
    }
    
    public double getHumidity()
    {
        humidity = CCUHsApi.getInstance().readHisValByQuery("point and air and humidity and sensor and current and group == \""+nodeAddr+"\"");
        return humidity;
    }
    public void setHumidity(double humidity)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and humidity and sensor and current and group == \""+nodeAddr+"\"", humidity);
        this.humidity = humidity;
    }
    
    public double getCO2()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and co2 and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setCO2(double co2)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and co2 and sensor and current and group == \""+nodeAddr+"\"", co2);
    }
    
    public double getVOC()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and voc and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setVOC(double voc)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and voc and sensor and current and group == \""+nodeAddr+"\"", voc);
    }
    
    public double getDesiredTemp()
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and average and sp and " +
                                                      "group == \""+nodeAddr+"\"");
        String id = point.get("id").toString();
        if (id == null || id == "") {
            Log.e(L.TAG_CCU_ZONE,"Invalid getDesiredTemp");
            return 72.0;
        }
        desiredTemp = CCUHsApi.getInstance().readDefaultValById(id);
        return desiredTemp;
    }
    public void setDesiredTemp(double desiredTemp)
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and average and sp and " +
                                                       "group == \""+nodeAddr+"\"");
        String id = point.get("id").toString();
        if (id == null || id == "") {
            Log.e(L.TAG_CCU_ZONE,"Invalid setDesiredTemp");
            return;
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
        this.desiredTemp = desiredTemp;
    }
    
    public double getDesiredTempCooling()
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and group " +
                                                   "== \""+nodeAddr+"\"");
        String id = point.get("id").toString();
        if (id == null || id == "") {
            Log.e(L.TAG_CCU_ZONE,"Invalid getDesiredTempCooling");
            return 74.0;
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    public void setDesiredTempCooling(double desiredTemp)
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and " +
                                                      "group == \""+nodeAddr+"\"");
        String id = point.get("id").toString();
        if (id == null || id == "") {
            Log.e(L.TAG_CCU_ZONE,"Invalid setDesiredTempCooling");
            return;
        }
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make(desiredTemp), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }
    
    public double getDesiredTempHeating()
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and " +
                                                      "group == \""+nodeAddr+"\"");
        if (point.isEmpty()) {
            Log.e(L.TAG_CCU_ZONE,"Invalid getDesiredTempHeating");
            return 0;
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(point.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    
    public void setDesiredTempHeating(double desiredTemp)
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and " +
                                                      "group == \""+nodeAddr+"\"");
        String id = point.get("id").toString();
        if (id == null || id == "") {
            Log.e(L.TAG_CCU_ZONE,"Invalid setDesiredTempHeating");
            return;
        }
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make(desiredTemp), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }
    
    public double getDamperLimit(String coolHeat, String minMax)
    {
        HashMap point = CCUHsApi.getInstance().read("point and config and damper and pos and "+coolHeat+" and "+minMax+" and " +
                                        "group == \""+nodeAddr+"\"");
        if (point.isEmpty()) {
            Log.e(L.TAG_CCU_ZONE,"Invalid getDamperLimit");
            return 0;
        }
        
        String id =point.get("id").toString();
        return CCUHsApi.getInstance().readDefaultValById(id);
    }
    public void setDamperLimit(String coolHeat, String minMax, double val)
    {
        HashMap point = CCUHsApi.getInstance().read("point and damper and pos and "+coolHeat+" and "+minMax+" and " +
                                                   "group == \""+nodeAddr+"\"");
        String id = point.get("id").toString();
        if (id == null || id == "") {
            Log.e(L.TAG_CCU_ZONE,"Invalid setDamperLimit");
            return;
        }
        CCUHsApi.getInstance().writeDefaultValById(id, val);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(id, val);
    }
    
    public double getSupplyAirTemp()
    {
        supplyAirTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and entering and group == \""+nodeAddr+"\"");
        return supplyAirTemp;
    }
    public void setSupplyAirTemp(double supplyAirTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and entering and group == \""+nodeAddr+"\"", supplyAirTemp);
        this.supplyAirTemp = supplyAirTemp;
    }
    public double getDischargeTemp()
    {
        dischargeTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and discharge and group == \""+nodeAddr+"\"");
        return dischargeTemp;
    }
    public void setDischargeTemp(double dischargeTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and discharge and group == \""+nodeAddr+"\"", dischargeTemp);
        this.dischargeTemp = dischargeTemp;
    }
    
    public double getDamperPos()
    {
        Double damperpos = CCUHsApi.getInstance().readHisValByQuery("point and air and damper and base and cmd and group == \""+nodeAddr+"\"");
        this.vavUnit.vavDamper.currentPosition = damperpos.intValue();
        return this.vavUnit.vavDamper.currentPosition;
    }
    public void setDamperPos(double damperPos)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and damper and base and cmd and group == \""+nodeAddr+"\"", damperPos);
        this.vavUnit.vavDamper.currentPosition = (int)damperPos;
    }
    
    public void setNormalizedDamperPos(double damperPos)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and damper and normalized and cmd and group == \""+nodeAddr+"\"", damperPos);
    }
    
    public double getReheatPos()
    {
        Double damperpos = CCUHsApi.getInstance().readHisValByQuery("point and reheat and cmd and group == \""+nodeAddr+"\"");
        this.vavUnit.reheatValve.currentPosition = damperpos.intValue();
        return this.vavUnit.reheatValve.currentPosition;
    }
    public void setReheatPos(double reheatPos)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and reheat and cmd and group == \""+nodeAddr+"\"", reheatPos);
        this.vavUnit.reheatValve.currentPosition = (int)reheatPos;
    }
    
    public boolean isFanOn(String type)
    {
        double fanState = CCUHsApi.getInstance().readHisValByQuery(type+" and point and " +
                                                                    "fan and cmd and group == \""+nodeAddr+ "\"");
        return fanState > 0;
    }
    public void setFanOn(String type, boolean state)
    {
        CCUHsApi.getInstance().writeHisValByQuery(type+" and point and fan and cmd and group == \""+nodeAddr+"\"",
                                                  state ? 1.0 : 0);
    }

    public double getDischargeSp()
    {
        return dischargeSp;
    }
    public void setDischargeSp(double dischargeSp)
    {
        this.dischargeSp = dischargeSp;
        CCUHsApi.getInstance().writeHisValByQuery("point and discharge and temp and sp and group == \""+nodeAddr+"\"",
                                                  dischargeSp);
    }
    public VavUnit getVavUnit()
    {
        return vavUnit;
    }
    public ControlLoop getCoolingLoop()
    {
        return coolingLoop;
    }
    public ControlLoop getHeatingLoop()
    {
        return heatingLoop;
    }
    public GenericPIController getValveController()
    {
        return valveController;
    }
    public void setValveController(GenericPIController valveController)
    {
        this.valveController = valveController;
    }
    public CO2Loop getCo2Loop()
    {
        return co2Loop;
    }
    public VOCLoop getVOCLoop()
    {
        return vocLoop;
    }
    public double getStaticPressure()
    {
        return staticPressure;
    }
    public void setStaticPressure(double staticPressure)
    {
        this.staticPressure = staticPressure;
    }
    
    public double getStatus() {
        return CCUHsApi.getInstance().readHisValByQuery("point and status and his and group == \""+nodeAddr+"\"");
    }
    
    public ControlLoop getCfmController()
    {
        return cfmController;
    }
    
    public void setStatus(double status, boolean emergency) {
        
        if (getStatus() != status )
        {
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + nodeAddr + "\"", status);
        }
        
        String message;
        if (emergency) {
            message = (status == 0 ? "Recirculating Air" : status == 1 ? "Emergency Cooling" : "Emergency Heating");
        } else
        {
            if (ScheduleProcessJob.getSystemOccupancy() == Occupancy.PRECONDITIONING) {
                message = "In Preconditioning ";
            } else
            {
                message = (status == 0 ? "Recirculating Air" : status == 1 ? "Cooling Space" : "Warming Space");
            }
        }

        message += getFanStatusMessage();

        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+nodeAddr+"\"");
        if (!curStatus.equals(message))
        {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + nodeAddr + "\"", message);
        }
    }
    
    public String getFanStatusMessage() {
        if (profileType == ProfileType.VAV_SERIES_FAN) {
            return isFanOn("series") ? ", Fan ON" : ", Fan OFF";
        } else if (profileType == ProfileType.VAV_PARALLEL_FAN) {
            return isFanOn("parallel") ? ", Fan ON" : ", Fan OFF";
        }
        return "";
    }

    public void setScheduleStatus(String status)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and scheduleStatus and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, status);
    }
    
    public void updateLoopParams() {
    
        CCUHsApi.getInstance().writeHisValByQuery("point and heating and loop and sp and his and group == \""+nodeAddr+"\"", heatingLoop.getLoopOutput());
    
    
        CCUHsApi.getInstance().writeHisValByQuery("point and cooling and loop and sp and his and group == \""+nodeAddr+"\"", coolingLoop.getLoopOutput());
    
        CCUHsApi.getInstance().writeHisValByQuery("point and discharge and air and temp and sp and his and group == \""+nodeAddr+"\"", dischargeSp);
    
    
        CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                                                  "supply and air and temp and his and group == \""+nodeAddr+"\"", satResetRequest.cumulativeRequestHoursPercent);
    
        CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                                                  "co2 and his and group == \""+nodeAddr+"\"", co2ResetRequest.cumulativeRequestHoursPercent);
        /*CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                                                  "hwst and his and group == \""+nodeAddr+"\"", hwstResetRequest.cumulativeRequestHoursPercent);*/
        CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                                                  "staticPressure and his and group == \""+nodeAddr+"\"", spResetRequest.cumulativeRequestHoursPercent);
    
        CCUHsApi.getInstance().writeHisValByQuery("point and request and current and tr and " +
                                                  "sat and his and group == \""+nodeAddr+"\"", (double)satResetRequest.currentRequests);
    
        CCUHsApi.getInstance().writeHisValByQuery("point and request and current and tr and " +
                                                  "co2 and his and group == \""+nodeAddr+"\"", (double)co2ResetRequest.currentRequests);
    
        CCUHsApi.getInstance().writeHisValByQuery("point and request and current and tr and " +
                                                  "staticPressure and his and group == \""+nodeAddr+"\"", (double)spResetRequest.currentRequests);
    
    }

    public double getZonePriorityValue(){
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+nodeAddr+"\"");
       return CCUHsApi.getInstance().readPointPriorityValByQuery("zone and priority and config and equipRef == \""+equip.get("id")+"\"");
    }
}
