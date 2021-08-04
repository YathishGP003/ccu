package a75f.io.logic.heartbeat;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.Globals;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;
import a75f.io.logic.util.PreferenceUtil;

public class HeartbeatMigration {

    private final static String CCU_HEART_BEAT_MIGRATION = "CCU_HEART_BEAT_MIGRATION";
    private final static String OAO = "oao";

    public static void initHeartbeatMigration() {
        new HeartbeatMigration().checkForHeartbeatMigration();
    }

    private void checkForHeartbeatMigration(){
        if (!PreferenceUtil.isHeartbeatMigrationDone()) {
            Log.i(CCU_HEART_BEAT_MIGRATION,"heartbeat migration started ");
            upgradeEquipsWithHeartbeatPoints(CCUHsApi.getInstance());
            PreferenceUtil.setHeartbeatMigrationStatus(true);
        }
    }

    private void upgradeEquipsWithHeartbeatPoints(CCUHsApi hayStack){
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteDis = (String) siteMap.get("dis");

        //zone level and standalone
        addHeartbeatToEquip(hayStack, "cpu", siteDis+"-CPU-", true);
        addHeartbeatToEquip(hayStack, "hpu", siteDis+"-HPU-", true);
        addHeartbeatToEquip(hayStack, "pipe2", siteDis+"-2PFCU-", true);
        addHeartbeatToEquip(hayStack, "pipe4", siteDis+"-4PFCU-", true);
        addHeartbeatToEquip(hayStack, "sse", siteDis+"-SSE-", true);

        //zone level and not standalone
        addHeartbeatToEquip(hayStack, "ti", siteDis+"-TI-", false);
        addHeartbeatToEquip(hayStack, "dab", siteDis+"-DAB-", false);
        addHeartbeatToEquip(hayStack, "dualDuct", siteDis+"-DualDuct-", false);
        addHeartbeatToEquip(hayStack, "emr", siteDis+"-EMR-", false);
        addHeartbeatToEquip(hayStack, OAO, siteDis+"-OAO-", false);
        addHeartbeatToEquip(hayStack, "pid", siteDis+"-PID-", false);
        addHeartbeatToEquip(hayStack, "vav", siteDis+"-VAV-", false);
        addHeartbeatToEquip(hayStack, "sense", siteDis+"-SENSE-", false);

        //modbus
       addHeartbeatToModbus(hayStack);
    }

    private void  addHeartbeatToModbus(CCUHsApi hayStack){
        List<HashMap> modbusEquips = hayStack.readAll("equip and modbus");
        modbusEquips.forEach(equipment -> {
            Equip modbusEquip = new Equip.Builder().setHashMap(equipment).build();
            String slaveId = modbusEquip.getGroup();
            String equipDisplayName = modbusEquip.getDisplayName();
            if(!isHeartbeatCreated(hayStack, slaveId)) {
                hayStack.addPoint(HeartBeat.getHeartBeatPoint(equipDisplayName, modbusEquip.getId(),
                        modbusEquip.getSiteRef(), modbusEquip.getRoomRef(), modbusEquip.getFloorRef(),
                        Integer.parseInt(slaveId), "modbus", ProfileType.valueOf(modbusEquip.getProfile()),
                        modbusEquip.getTz()));
                Log.i(CCU_HEART_BEAT_MIGRATION,"heartbeat point added for modbus with the address "+slaveId);
          }
        });
    }

    private void addHeartbeatToEquip(CCUHsApi hayStack, String profile, String equipDis, boolean isStandAlone){
        String query = "equip and zone and "+profile;
        if(profile.equals(OAO)){
            query = "equip and "+OAO;
        }
        List<HashMap> equips = hayStack.readAll(query);
        equips.forEach(equipment -> {
            Equip equip = new Equip.Builder().setHashMap(equipment).build();
            String nodeAddress = equip.getGroup();
            String equipDisplay = equipDis+nodeAddress;
            if(!isHeartbeatCreated(hayStack, nodeAddress)){
                String heartBeatId = "";
                if(isStandAlone){
                    heartBeatId = hayStack.addPoint(HeartBeat.getHeartBeatPoint(equipDisplay, equip.getId(),
                            equip.getSiteRef(), equip.getRoomRef(), equip.getFloorRef(), Integer.parseInt(nodeAddress),
                            profile, equip.getTz()));
                }
                else{
                    heartBeatId = CCUHsApi.getInstance().addPoint(HeartBeat.getHeartBeatPoint(equipDisplay, equip.getId(),
                            equip.getSiteRef(), equip.getRoomRef(), equip.getFloorRef(), Integer.parseInt(nodeAddress),
                            profile, equip.getTz(), false));
                }
                Log.i(CCU_HEART_BEAT_MIGRATION,"heartbeat point added for "+ profile +" with the address  "+nodeAddress);
                addRssiPointToDevice(hayStack, profile, nodeAddress, equip.getTz(), heartBeatId);
            }
        });
    }

    private boolean isHeartbeatCreated(CCUHsApi hayStack, String nodeAddress){
        return hayStack.read("point and heartbeat and group == \""+nodeAddress+"\"").size() > 0;
    }

    private void addRssiPointToDevice(CCUHsApi hayStack, String profile, String nodeAddress, String timeZone,
                                      String heartBeatId){
        HashMap device = hayStack.read("device and addr == \""+nodeAddress+"\"");
        RawPoint rawPoint = HeartBeat.getHeartBeatRawPoint(Integer.parseInt(nodeAddress), device.get("id").toString(),
                device.get("siteRef").toString(), device.get("roomRef").toString(), device.get("floorRef").toString(),
                timeZone);
        rawPoint.setPointRef(heartBeatId);
        hayStack.addPoint(rawPoint);
        Log.i(CCU_HEART_BEAT_MIGRATION,"rssi point added for "+ profile +" with the address "+nodeAddress);
    }
}
