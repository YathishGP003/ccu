package a75f.io.logic.bo.haystack.device;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.RawPoint;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.config.ProfileConfiguration;
import a75f.io.domain.util.PointsUtil;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration;

/**
 * Models a HyperStat Split device Haystack entity.
 */
public class HyperStatSplitDevice {

    int hyperStatNodeAddress;

    public RawPoint relay1;
    public RawPoint relay2;
    public RawPoint relay3;
    public RawPoint relay4;
    public RawPoint relay5;
    public RawPoint relay6;
    public RawPoint relay7;
    public RawPoint relay8;
    public RawPoint analog1Out;
    public RawPoint analog2Out;
    public RawPoint analog3Out;
    public RawPoint analog4Out;

    public RawPoint rssi;
    public RawPoint currentTemp;
    public RawPoint desiredTemp;

    public String deviceRef;
    public String siteRef;
    public String floorRef;
    public String roomRef;

    String tz;

    public void addSensor(Port p, String pointRef) {
        RawPoint sensor = new RawPoint.Builder()
                .setDisplayName(p.getPortSensor()+"Sensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setPointRef(pointRef)
                .setEnabled(true)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .setPort(p.toString())
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(sensor);
    }

    public void addSensor(Port p, String pointRef, String unit) {
        RawPoint sensor = new RawPoint.Builder()
                .setDisplayName(p.getPortSensor()+"Sensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setPointRef(pointRef)
                .setEnabled(true)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .setPort(p.toString())
                .setUnit(unit)
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(sensor);
    }

    public void addSensor(Port p, String pointRef, String unit, boolean enabled) {
        RawPoint sensor = new RawPoint.Builder()
                .setDisplayName(p.getPortSensor()+"Sensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setPointRef(pointRef)
                .setEnabled(enabled)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .setPort(p.toString())
                .setUnit(unit)
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(sensor);
    }

    public RawPoint addEquipSensorFromRawPoint(RawPoint deviceSensor, Port p, int nodeAddress) {
        Equip q = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \""+nodeAddress+"\"")).build();

        PointsUtil pointsUtil = new PointsUtil(CCUHsApi.getInstance());
        String pointRef = pointsUtil.createDynamicSensorEquipPoint(q, getEquipDomainNameFromPort(p), getConfigurationFromEquip(q, pointsUtil));
        updatePhysicalPointRef(nodeAddress, getDeviceDomainNameFromPort(p), pointRef);

        CCUHsApi.getInstance().scheduleSync();

        return deviceSensor;
    }

    public static String getEquipDomainNameFromPort(Port p) {
        switch(p) {
            case SENSOR_RH: return DomainName.zoneHumidity;
            case SENSOR_CO2: return DomainName.zoneCo2;
            case SENSOR_VOC: return DomainName.zoneVoc;
            case SENSOR_ILLUMINANCE: return DomainName.zoneIlluminance;
            case SENSOR_SOUND: return DomainName.zoneSound;
            case SENSOR_PM2P5: return DomainName.zonePm25;
            case SENSOR_CO: return DomainName.zoneCo;
            case SENSOR_NO: return DomainName.zoneNo;
            case SENSOR_UVI: return DomainName.zoneUvi;
            case SENSOR_CO2_EQUIVALENT: return DomainName.zoneCo2Equivalent;
            case SENSOR_OCCUPANCY: return DomainName.zoneOccupancy;

            default: return null;
        }
    }

    public static String getDeviceDomainNameFromPort(Port p) {
        switch(p) {
            case SENSOR_PM2P5: return DomainName.pm25Sensor;
            default: return null;
        }
    }

    private static void updatePhysicalPointRef(int addr, String domainName, String pointRef) {
        CcuLog.d("CCU"," Update Physical point "+domainName);

        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device.isEmpty())
        {
            return ;
        }

        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and domainName == \""+domainName+"\"");
        RawPoint p = new RawPoint.Builder().setHashMap(point).build();
        p.setPointRef(pointRef);
        CCUHsApi.getInstance().updatePoint(p,p.getId());

    }

    private ProfileConfiguration getConfigurationFromEquip(Equip equip, PointsUtil pointsUtil) {
        if (equip.getDomainName().equals(DomainName.hyperstatSplitCPU)) {
            return new HyperStatSplitCpuConfiguration(Integer.parseInt(equip.getGroup()), NodeType.HYPERSTATSPLIT.name(), 0, equip.getRoomRef(), equip.getFloorRef(), ProfileType.HYPERSTATSPLIT_CPU, pointsUtil.getModelFromEquip(equip)).getActiveConfiguration();
        } else {
            return null;
        }
    }

}
