package a75f.io.logic.bo.building.sscpu;

import java.util.ArrayList;
import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.standalone.Stage;
import a75f.io.logic.bo.haystack.device.SmartStat;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.tuners.TunerConstants;

public class ConventionalUnitLogicalMap {

    int nodeAddr;
    ProfileType profileType;
    ConventionalUnitProfile smartStatCpuUnit;

    double      currentTemp;
    double      humidity;
    double desiredTemp;
    double supplyAirTemp;
    double dischargeTemp;


    public ConventionalUnitLogicalMap(ProfileType T, int node) {

        profileType = T;
        smartStatCpuUnit = new ConventionalUnitProfile();
        nodeAddr = node;
    }
    public void createHaystackPoints(ConventionalUnitConfiguration config, String floor, String room) {

        //Create Logical points
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-CPU-"+nodeAddr;
        String profile = "smartstat";
		String gatewayRef = null;
        HashMap systemEquip = CCUHsApi.getInstance().read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0) {
            gatewayRef = systemEquip.get("id").toString();
        }
        Equip.Builder b = new Equip.Builder()
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .setProfile(profileType.name())
                .setPriority(config.getPriority().name())
                .addMarker("equip").addMarker("standalone").addMarker("equipHis").addMarker("smartstat").addMarker("zone")
                .setAhuRef(gatewayRef)
                .setTz(tz)
                .setGroup(String.valueOf(nodeAddr));
            b.setDisplayName(equipDis);
            b.addMarker("cpu");
            profile = "cpu";
        String equipRef = CCUHsApi.getInstance().addEquip(b.build());
        BuildingTuners.getInstance().addEquipStandaloneTuners(siteDis+"-CPU-"+nodeAddr, equipRef);

        createConventionalConfigPoints(config, equipRef,floor,room);

        addProfilePoints(siteRef,equipRef,equipDis,tz,floor,room);
        Point currentTemp = new Point.Builder()
                .setDisplayName(equipDis+"-currentTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("zone").addMarker("standalone").addMarker("cpu")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical").addMarker("equipHis")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);

        Point humidity = new Point.Builder()
                .setDisplayName(equipDis+"-humidity")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("zone").addMarker("standalone").addMarker("cpu")
                .addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical").addMarker("equipHis")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);

        Point co2 = new Point.Builder()
                .setDisplayName(equipDis+"-co2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("zone").addMarker("standalone").addMarker("cpu")
                .addMarker("air").addMarker("co2").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical").addMarker("equipHis")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String co2Id = CCUHsApi.getInstance().addPoint(co2);

        Point voc = new Point.Builder()
                .setDisplayName(equipDis+"-voc")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("zone").addMarker("standalone").addMarker("cpu")
                .addMarker("air").addMarker("voc").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical").addMarker("equipHis")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String vocId = CCUHsApi.getInstance().addPoint(voc);
        Point desiredTemp = new Point.Builder()
                .setDisplayName(equipDis+"-desiredTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("standalone").addMarker("average")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("userIntent").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String dtId = CCUHsApi.getInstance().addPoint(desiredTemp);

        Point desiredTempCooling = new Point.Builder()
                .setDisplayName(equipDis+"-desiredTempCooling")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("standalone").addMarker("cooling")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("userIntent").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(desiredTempCooling);

        Point desiredTempHeating = new Point.Builder()
                .setDisplayName(equipDis+"-desiredTempHeating")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("standalone").addMarker("heating")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("userIntent").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(desiredTempHeating);

        Point datPoint = new Point.Builder()
                .setDisplayName(equipDis+"-dischargeAirTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("discharge").addMarker("standalone").addMarker(profile).addMarker("equipHis")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();

        String datID = CCUHsApi.getInstance().addPoint(datPoint);

        Point eatPoint = new Point.Builder()
                .setDisplayName(equipDis+"-enteringAirTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("entering").addMarker("standalone").addMarker(profile).addMarker("equipHis")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String eatID = CCUHsApi.getInstance().addPoint(eatPoint);

        Point coolingStage1 = new Point.Builder()
                .setDisplayName(equipDis+"-coolingStage1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("standalone").addMarker("cooling").addMarker("stage1").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("equipHis").addMarker("cmd")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r1ID = CCUHsApi.getInstance().addPoint(coolingStage1);

        Point coolingStage2 = new Point.Builder()
                .setDisplayName(equipDis+"-coolingStage2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("standalone").addMarker("cooling").addMarker("stage2").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("equipHis").addMarker("cmd")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r2ID = CCUHsApi.getInstance().addPoint(coolingStage2);

        Point heatingStage1 = new Point.Builder()
                .setDisplayName(equipDis+"-heatingStage1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("standalone").addMarker("heating").addMarker("stage1").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("equipHis").addMarker("cmd")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r4ID = CCUHsApi.getInstance().addPoint(heatingStage1);

        Point heatingStage2 = new Point.Builder()
                .setDisplayName(equipDis+"-heatingStage2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("standalone").addMarker("heating").addMarker("stage2").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("equipHis").addMarker("cmd")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r5ID = CCUHsApi.getInstance().addPoint(heatingStage2);

        Point fanStage1 = new Point.Builder()
                .setDisplayName(equipDis+"-fanStage1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("standalone").addMarker("fan").addMarker("stage1").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("equipHis").addMarker("cmd")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r3ID = CCUHsApi.getInstance().addPoint(fanStage1);

        Point fanStage2 = new Point.Builder()
                .setDisplayName(equipDis+"-fanStage2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("standalone").addMarker("fan").addMarker("stage2").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("equipHis").addMarker("cmd")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r6ID = CCUHsApi.getInstance().addPoint(fanStage2);

        //Create Physical points and map
        SmartStat device = new SmartStat(nodeAddr, siteRef, floor, room,equipRef);
        //TODO Need to set it for default if not enabled, currently set it as enabled //kumar
      
            device.th1In.setPointRef(datID);
            device.th1In.setEnabled(config.enableThermistor1);
            device.th2In.setPointRef(eatID);
            device.th2In.setEnabled(config.enableThermistor2);
            device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        device.humidity.setPointRef(humidityId);
        device.humidity.setEnabled(true);
        device.co2.setPointRef(co2Id);
        device.co2.setEnabled(true);
        device.voc.setPointRef(vocId);
        device.voc.setEnabled(true);
        device.desiredTemp.setPointRef(dtId);
        device.desiredTemp.setEnabled(true);
        device.relay1.setPointRef(r1ID);
        device.relay2.setPointRef(r2ID);
        device.relay3.setPointRef(r3ID);
        device.relay4.setPointRef(r4ID);
        device.relay5.setPointRef(r5ID);
        device.relay6.setPointRef(r6ID);
        device.relay1.setEnabled(config.isOpConfigured(Port.RELAY_ONE));
        device.relay2.setEnabled(config.isOpConfigured(Port.RELAY_TWO));
        device.relay3.setEnabled(config.isOpConfigured(Port.RELAY_THREE));
        device.relay4.setEnabled(config.isOpConfigured(Port.RELAY_FOUR));
        device.relay5.setEnabled(config.isOpConfigured(Port.RELAY_FIVE));
        device.relay6.setEnabled(config.isOpConfigured(Port.RELAY_SIX));

        device.addPointsToDb();

        //Initialize write array for points, otherwise a read before write will throw exception
        setCurrentTemp(0);
        setDischargeTemp(0);
        setSupplyAirTemp(0);
        setDesiredTempCooling(73.0);
        setDesiredTemp(72.0);
        setDesiredTempHeating(71.0);
        setHumidity(0);
        setCO2(0);
        setVOC(0);

        CCUHsApi.getInstance().syncEntityTree();


    }
    private void addProfilePoints(String siteRef, String equipref, String equipDis, String tz,String floor, String room) {
        Point cpuOccupancy = new Point.Builder()
                .setDisplayName(equipDis + "-" + "occupancy")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("standalone").addMarker("occupancy").addMarker("status").addMarker("his").addMarker("equipHis").addMarker("sp").addMarker("zone")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(cpuOccupancy);

        Point cpuOperatingMode = new Point.Builder()
                .setDisplayName(equipDis + "-" + "ConditionMode")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("standalone").addMarker("temp").addMarker("conditioning").addMarker("mode").addMarker("his").addMarker("equipHis").addMarker("sp").addMarker("zone")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(cpuOperatingMode);
    }

    public void setProfilePoint(String tags, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and zone and standalone and his and "+tags, val);
    }
    public void createConventionalConfigPoints(ConventionalUnitConfiguration config, String equipRef, String floor, String room) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis+"-CPU-"+nodeAddr;
        String tz = siteMap.get("tz").toString();
        String profile = "cpu";
        Point enableOccupancyControl = new Point.Builder()
                .setDisplayName(equipDis + "-enableOccupancyControl")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("enable").addMarker("occupancy").addMarker("control").addMarker("sp").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableOccupancyControlId = CCUHsApi.getInstance().addPoint(enableOccupancyControl);
        CCUHsApi.getInstance().writeDefaultValById(enableOccupancyControlId, config.enableOccupancyControl == true ? 1.0 : 0);

        Point temperatureOffset = new Point.Builder()
                .setDisplayName(equipDis+"-temperatureOffset")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("temperature").addMarker("offset").addMarker("sp").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String temperatureOffsetId = CCUHsApi.getInstance().addPoint(temperatureOffset);
        CCUHsApi.getInstance().writeDefaultValById(temperatureOffsetId, (double)config.temperatureOffset);

        Point airflowTh1 = new Point.Builder()
                .setDisplayName(equipDis+"-enableAirflowTh1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("air").addMarker("temp").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableAirflowId = CCUHsApi.getInstance().addPoint(airflowTh1);
        CCUHsApi.getInstance().writeDefaultValById(enableAirflowId, (double)(config.enableThermistor1 == true? 1.0 : 0));

        Point external10KProbeTh2 = new Point.Builder()
                .setDisplayName(equipDis+"-enableExternal10kTempSensorTh2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone").addMarker("current")
                .addMarker("air").addMarker("temp").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableexternal10KProbeTh2Id = CCUHsApi.getInstance().addPoint(external10KProbeTh2);
        CCUHsApi.getInstance().writeDefaultValById(enableexternal10KProbeTh2Id, (double)(config.enableThermistor2 == true? 1.0 : 0));

        Point enableRelay1 = new Point.Builder()
                .setDisplayName(equipDis+"-enable"+ Stage.COOLING_1.displayName)
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay1").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay1Id = CCUHsApi.getInstance().addPoint(enableRelay1);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay1Id, (double)(config.enableRelay1 == true? 1.0 : 0));

        Point enableRelay2 = new Point.Builder()
                .setDisplayName(equipDis+"-enableCoolingStage2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay2").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay2Id = CCUHsApi.getInstance().addPoint(enableRelay2);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay2Id, (double)(config.enableRelay2 == true? 1.0 : 0));

        Point enableRelay3 = new Point.Builder()
                .setDisplayName(equipDis+"-enableFanStage1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay3").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay3Id = CCUHsApi.getInstance().addPoint(enableRelay3);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay3Id, (double)(config.enableRelay3 == true? 1.0 : 0));

        Point enableRelay4 = new Point.Builder()
                .setDisplayName(equipDis+"-enableHeatingStage1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay4").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay4Id = CCUHsApi.getInstance().addPoint(enableRelay4);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay4Id, (double)(config.enableRelay4 == true? 1.0 : 0));

        Point enableRelay5 = new Point.Builder()
                .setDisplayName(equipDis+"-enableHeatingStage2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay5").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay5Id = CCUHsApi.getInstance().addPoint(enableRelay5);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay5Id, (double)(config.enableRelay5 == true? 1.0 : 0));

        Point enableRelay6 = new Point.Builder()
                .setDisplayName(equipDis+"-enableFanStage2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay6").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay6Id = CCUHsApi.getInstance().addPoint(enableRelay6);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay6Id, (double)(config.enableRelay6 == true? 1.0 : 0));
        addUserIntentPoints(equipRef,equipDis);


    }

    public void updateHaystackPoints(ConventionalUnitConfiguration config) {
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case RELAY_ONE:
                case RELAY_TWO:
                case RELAY_THREE:
                case RELAY_FOUR:
                case RELAY_FIVE:
                case RELAY_SIX:
                    SmartStat.updatePhysicalPointType(nodeAddr, op.getPort().toString(), op.getRelayActuatorType());
                    break;
            }
        }
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_ONE.name(), config.isOpConfigured(Port.RELAY_ONE) );
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_TWO.name(), config.isOpConfigured(Port.RELAY_TWO) );
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_THREE.name(), config.isOpConfigured(Port.RELAY_THREE) );
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_FOUR.name(), config.isOpConfigured(Port.RELAY_FOUR) );
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_FIVE.name(), config.isOpConfigured(Port.RELAY_FIVE) );
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_SIX.name(), config.isOpConfigured(Port.RELAY_SIX) );
        SmartStat.setPointEnabled(nodeAddr, Port.TH1_IN.name(),config.enableThermistor1);
        SmartStat.setPointEnabled(nodeAddr, Port.TH2_IN.name(), config.enableThermistor2);

        setConfigNumVal("enable and occupancy",config.enableOccupancyControl == true ? 1.0 : 0);
        setConfigNumVal("temperature and offset",config.temperatureOffset);
        setConfigNumVal("enable and air and temp",config.enableThermistor1 == true ? 1.0 : 0);
        setConfigNumVal("enable and current and temp",config.enableThermistor2 == true ? 1.0 : 0);
    }



    public ConventionalUnitConfiguration getProfileConfiguration() {
        ConventionalUnitConfiguration config = new ConventionalUnitConfiguration();
        config.enableOccupancyControl = getConfigNumVal("enable and occupancy") > 0 ? true : false ;
        config.temperatureOffset = getConfigNumVal("temperature and offset");
        config.enableThermistor1 = getConfigNumVal("enable and air and temp") >  0 ? true : false;
        config.enableThermistor2 = getConfigNumVal("enable and current and temp") > 0 ? true : false;
        config.setNodeType(NodeType.SMART_STAT);//TODO - revisit


        RawPoint r1 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_ONE.toString());
        if (r1 != null && r1.getEnabled()) {
            Output relay1 = new Output();
            relay1.setAddress((short)nodeAddr);
            relay1.setPort(Port.RELAY_ONE);
            relay1.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r1.getType());
            config.getOutputs().add(relay1);
        }

        RawPoint r2 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_TWO.toString());
        if (r2 != null && r2.getEnabled()) {
            Output relay2 = new Output();
            relay2.setAddress((short)nodeAddr);
            relay2.setPort(Port.RELAY_TWO);
            relay2.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r2.getType());
            config.getOutputs().add(relay2);
        }
        RawPoint r3 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_THREE.toString());
        if (r3 != null && r3.getEnabled()) {
            Output relayThree = new Output();
            relayThree.setAddress((short)nodeAddr);
            relayThree.setPort(Port.RELAY_THREE);
            relayThree.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r3.getType());
            config.getOutputs().add(relayThree);
        }
        RawPoint r4 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_FOUR.toString());
        if (r4 != null && r4.getEnabled()) {
            Output relayFour = new Output();
            relayFour.setAddress((short)nodeAddr);
            relayFour.setPort(Port.RELAY_FOUR);
            relayFour.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r4.getType());
            config.getOutputs().add(relayFour);
        }
        RawPoint r5 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_FIVE.toString());
        if (r5 != null && r5.getEnabled()) {
            Output relayFive = new Output();
            relayFive.setAddress((short)nodeAddr);
            relayFive.setPort(Port.RELAY_FIVE);
            relayFive.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r5.getType());
            config.getOutputs().add(relayFive);
        }
        RawPoint r6 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_SIX.toString());
        if (r6 != null && r6.getEnabled()) {
            Output relaySix = new Output();
            relaySix.setAddress((short)nodeAddr);
            relaySix.setPort(Port.RELAY_SIX);
            relaySix.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r6.getType());
            config.getOutputs().add(relaySix);
        }
        return config;
    }
    public double getCurrentTemp()
    {
        currentTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and standalone and group == \""+nodeAddr+"\"");
        return currentTemp;
    }
    public void setCurrentTemp(double roomTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and current and standalone and group == \""+nodeAddr+"\"", roomTemp);
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
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and average and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        desiredTemp = CCUHsApi.getInstance().readDefaultValById(id);
        return desiredTemp;
    }
    public void setDesiredTemp(double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and average and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
        this.desiredTemp = desiredTemp;
    }

    public double getDesiredTempCooling()
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and cooling and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
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
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and cooling and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
        this.desiredTemp = desiredTemp;
    }

    public double getDesiredTempHeating()
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and heating and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
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
    public void setDesiredTempHeating(double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and heating and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
        this.desiredTemp = desiredTemp;
    }
    
    public double getDischargeTemp()
    {
        dischargeTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and discharge and standalone and group == \""+nodeAddr+"\"");
        return dischargeTemp;
    }
    public void setDischargeTemp(double dischargeTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and discharge and standalone and group == \""+nodeAddr+"\"", dischargeTemp);
        this.dischargeTemp = dischargeTemp;
    }

    public double getSupplyAirTemp()
    {
        supplyAirTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and entering and standalone and group == \""+nodeAddr+"\"");
        return supplyAirTemp;
    }
    public void setSupplyAirTemp(double supplyAirTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and entering and standalone and group == \""+nodeAddr+"\"", supplyAirTemp);
        this.supplyAirTemp = supplyAirTemp;
    }

    public void setConfigNumVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and standalone and cpu and "+tags+" and group == \""+nodeAddr+"\"", val);
    }

    public double getConfigNumVal(String tags) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and cpu and "+tags+" and group == \""+nodeAddr+"\"");
    }

    protected void addUserIntentPoints(String equipref, String equipDis) {

        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();

        Point fanOpMode = new Point.Builder()
                .setDisplayName(equipDis+"-"+"FanOpMode")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .addMarker("standalone").addMarker("userIntent").addMarker("writable").addMarker("fan").addMarker("operation").addMarker("mode").addMarker("his").addMarker("equipHis")
                .addMarker("cpu").addMarker("zone")
                .setTz(tz)
                .build();
        String fanOpModeId = CCUHsApi.getInstance().addPoint(fanOpMode);
        CCUHsApi.getInstance().writePoint(fanOpModeId, TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_DEFAULT_FAN_OPERATIONAL_MODE, 0);
        CCUHsApi.getInstance().writeHisValById(fanOpModeId, TunerConstants.STANDALONE_DEFAULT_FAN_OPERATIONAL_MODE);

        Point operationalMode = new Point.Builder()
                .setDisplayName(equipDis+"-"+"OperationalMode")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .addMarker("standalone").addMarker("userIntent").addMarker("writable").addMarker("operation").addMarker("mode").addMarker("zone").addMarker("his").addMarker("equipHis")
                .addMarker("cpu").addMarker("temp")
                .setTz(tz)
                .build();
        String operationalModeId = CCUHsApi.getInstance().addPoint(operationalMode);
        CCUHsApi.getInstance().writePoint(operationalModeId, TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_DEFAULT_OPERATIONAL_MODE, 0);
        CCUHsApi.getInstance().writeHisValById(operationalModeId, TunerConstants.STANDALONE_DEFAULT_OPERATIONAL_MODE);


    }
}
