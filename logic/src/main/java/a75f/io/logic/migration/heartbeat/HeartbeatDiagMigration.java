package a75f.io.logic.migration.heartbeat;

import android.util.Log;
import java.util.HashMap;
import java.util.List;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;
import a75f.io.logic.util.PreferenceUtil;

public class HeartbeatDiagMigration {
    private final static String CCU_HEART_BEAT_DIAG_MIGRATION = "CCU_HEART_BEAT_DIAG_MIGRATION";
    private final static String OAO = "oao";

    public static void initHeartbeatDiagMigration() {
        new HeartbeatDiagMigration().checkForHeartbeatDiagMigration();
    }

    private void checkForHeartbeatDiagMigration(){
        if (!PreferenceUtil.isHeartbeatMigrationAsDiagDone()) {
            Log.i(CCU_HEART_BEAT_DIAG_MIGRATION,"heartbeat diag migration started ");
            upgradeEquipsWithHeartbeatPoints(CCUHsApi.getInstance());
            PreferenceUtil.setHeartbeatMigrationAsDiagStatus(true);
            Log.i(CCU_HEART_BEAT_DIAG_MIGRATION,"heartbeat diag migration completed ");
        }
    }

    private void upgradeEquipsWithHeartbeatPoints(CCUHsApi hayStack){
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteDis = (String) siteMap.get("dis");

        //zone level and standalone
        addHeartbeatDiagToEquip(hayStack, "cpu", siteDis+"-CPU-", true);
        addHeartbeatDiagToEquip(hayStack, "hpu", siteDis+"-HPU-", true);
        addHeartbeatDiagToEquip(hayStack, "pipe2", siteDis+"-2PFCU-", true);
        addHeartbeatDiagToEquip(hayStack, "pipe4", siteDis+"-4PFCU-", true);
        addHeartbeatDiagToEquip(hayStack, "sse", siteDis+"-SSE-", true);

        //zone level and not standalone
        addHeartbeatDiagToEquip(hayStack, "ti", siteDis+"-TI-", false);
        addHeartbeatDiagToEquip(hayStack, "dab", siteDis+"-DAB-", false);
        addHeartbeatDiagToEquip(hayStack, "dualDuct", siteDis+"-DualDuct-", false);
        addHeartbeatDiagToEquip(hayStack, "emr", siteDis+"-EMR-", false);
        addHeartbeatDiagToEquip(hayStack, OAO, siteDis+"-OAO-", false);
        addHeartbeatDiagToEquip(hayStack, "pid", siteDis+"-PID-", false);
        addHeartbeatDiagToEquip(hayStack, "vav", siteDis+"-VAV-", false);
        addHeartbeatDiagToEquip(hayStack, "sense", siteDis+"-SENSE-", false);

        //modbus
        addHeartbeatDiagToModbus(hayStack);
    }

    private void addHeartbeatDiagToModbus(CCUHsApi hayStack){
        List<HashMap> modbusEquips = hayStack.readAll("equip and modbus");
        modbusEquips.forEach(equipment -> {
            Equip modbusEquip = new Equip.Builder().setHashMap(equipment).build();
            String slaveId = modbusEquip.getGroup();
            String equipDisplayName = modbusEquip.getDisplayName();
            if(!isHeartbeatDiagCreated(hayStack, slaveId)) {
                deleteNonDiagHeartbeatPoint(hayStack, slaveId);
                deleteRssiPointReferringNonDiagHeartbeatPoint(hayStack, slaveId);
                hayStack.addPoint(HeartBeat.getHeartBeatPoint(equipDisplayName, modbusEquip.getId(),
                        modbusEquip.getSiteRef(), modbusEquip.getRoomRef(), modbusEquip.getFloorRef(),
                        Integer.parseInt(slaveId), "modbus", ProfileType.valueOf(modbusEquip.getProfile()),
                        modbusEquip.getTz()));
                Log.i(CCU_HEART_BEAT_DIAG_MIGRATION,"heartbeat diag point added for modbus with the address "+slaveId);
            }
            else if(isHeartbeatDiagCreated(hayStack, slaveId) && isRssiPointMoreThanOne(hayStack, slaveId)){
                deleteDanglingRssiPoint(hayStack, slaveId);

            }
        });
    }

    private void addHeartbeatDiagToEquip(CCUHsApi hayStack, String profile, String equipDis, boolean isStandAlone){
        String query = "equip and zone and "+profile;
        if(profile.equals(OAO)){
            query = "equip and "+OAO;
        }
        List<HashMap> equips = hayStack.readAll(query);
        equips.forEach(equipment -> {
            Equip equip = new Equip.Builder().setHashMap(equipment).build();
            String nodeAddress = equip.getGroup();
            String equipDisplay = equipDis+nodeAddress;
            if(!isHeartbeatDiagCreated(hayStack, nodeAddress)){
                deleteNonDiagHeartbeatPoint(hayStack, nodeAddress);
                deleteRssiPointReferringNonDiagHeartbeatPoint(hayStack, nodeAddress);
                String heartBeatId = "";
                if(isStandAlone){
                    heartBeatId = hayStack.addPoint(HeartBeat.getHeartBeatPoint(equipDisplay, equip.getId(), equip.getSiteRef(),
                            equip.getRoomRef(), equip.getFloorRef(), Integer.parseInt(nodeAddress),
                            profile, equip.getTz()));
                }
                else{
                    heartBeatId = hayStack.addPoint(HeartBeat.getHeartBeatPoint(equipDisplay, equip.getId(),
                            equip.getSiteRef(), equip.getRoomRef(), equip.getFloorRef(), Integer.parseInt(nodeAddress),
                            profile, equip.getTz(), false));
                }
                Log.i(CCU_HEART_BEAT_DIAG_MIGRATION,
                        "heartbeat diag point added for "+ profile +" with the address  "+nodeAddress);
                addRssiPointToDevice(hayStack, profile, nodeAddress, equip.getTz(), heartBeatId);
            }
            else if(isHeartbeatDiagCreated(hayStack, nodeAddress) && isRssiPointMoreThanOne(hayStack, nodeAddress)){
                deleteDanglingRssiPoint(hayStack, nodeAddress);

            }
        });
    }

    private void deleteDanglingRssiPoint(CCUHsApi hayStack, String nodeAddress){
        String heartbeatId =
                hayStack.read("point and heartbeat and diag and group == \""+nodeAddress+"\"").get("id").toString();
        String rssiDis = "rssi-"+nodeAddress;
        List<HashMap> rssiPoints = hayStack.readAll("point and rssi and dis == \""+rssiDis+"\"");
        for(HashMap rssiPoint : rssiPoints){
            if(!heartbeatId.equals(rssiPoint.get("pointRef").toString())){
                String rssiId = rssiPoint.get("id").toString();
                hayStack.deleteEntityTree(rssiId);
                Log.i(CCU_HEART_BEAT_DIAG_MIGRATION,
                        "Dangling rssi point with id  "+ rssiId +" for the address "+nodeAddress +" is deleted");
            }
        }
    }

    private boolean isHeartbeatDiagCreated(CCUHsApi hayStack, String nodeAddress){
        return hayStack.read("point and heartbeat and diag and group == \""+nodeAddress+"\"").size() > 0;
    }

    private void deleteNonDiagHeartbeatPoint(CCUHsApi hayStack, String nodeAddress){
        HashMap heartbeatPoint = hayStack.read("point and heartbeat and group == \""+nodeAddress+"\"");
        if(heartbeatPoint.size() > 0){
            hayStack.deleteEntityTree(heartbeatPoint.get("id").toString());
        }
    }

    private boolean isRssiPointMoreThanOne(CCUHsApi hayStack, String nodeAddress){
        String rssiDis = "rssi-"+nodeAddress;
        return hayStack.readAll("point and rssi and dis == \""+rssiDis+"\"").size() > 1;
    }

    private void deleteRssiPointReferringNonDiagHeartbeatPoint(CCUHsApi hayStack, String nodeAddress){
        String rssiDis = "rssi-"+nodeAddress;
        HashMap rssiHeartbeatPoint = hayStack.read("point and rssi and dis == \""+rssiDis+"\"");
        if(rssiHeartbeatPoint.size() > 0){
            hayStack.deleteEntityTree(rssiHeartbeatPoint.get("id").toString());
        }
    }

    private void addRssiPointToDevice(CCUHsApi hayStack, String profile, String nodeAddress, String timeZone,
                                      String heartBeatId){
        HashMap device = hayStack.read("device and addr == \""+nodeAddress+"\"");
        RawPoint rawPoint = HeartBeat.getHeartBeatRawPoint(Integer.parseInt(nodeAddress), device.get("id").toString(),
                device.get("siteRef").toString(), device.get("roomRef").toString(), device.get("floorRef").toString(),
                timeZone);
        rawPoint.setPointRef(heartBeatId);
        hayStack.addPoint(rawPoint);
        Log.i(CCU_HEART_BEAT_DIAG_MIGRATION,"rssi point added for "+ profile +" with the address "+nodeAddress);
    }

}
