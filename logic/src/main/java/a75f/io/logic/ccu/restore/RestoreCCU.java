package a75f.io.logic.ccu.restore;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HGrid;
import org.projecthaystack.HRow;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.RestoreCCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.exception.NullHGridException;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.ModbusEquipsInfo;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.modbusbox.EquipsManager;

public class RestoreCCU {

    private RestoreCCUHsApi restoreCCUHsApi = RestoreCCUHsApi.getInstance();
    private final String TAG = "CCU_REPLACE";
    private HGrid ccuHGrid;

    private HGrid getCcuHGrid(String siteCode) {
        if(ccuHGrid == null){
            ccuHGrid =  restoreCCUHsApi.getAllCCUs(siteCode);
            if(ccuHGrid == null){
                throw new NullHGridException("Null occurred while fetching CCU List.");
            }
        }
        return ccuHGrid;
    }

    private boolean isCCUOnline(Date lastUpdatedDate){
        return new Date().getTime() - lastUpdatedDate.getTime() <= (15*60000);
    }

    public List<CCU> getCCUList(String siteCode, JSONArray ccuArray) {
      Log.i(TAG, "Fetching all the CCU details");
        HGrid ccuGrid = getCcuHGrid(siteCode);
        Iterator ccuGridIterator = ccuGrid.iterator();
        List<String> equipRefs = new LinkedList<>();
        /*
        key - ccuId
        Value - equipRef
         */
        Map<String, String> ccuIdMap = new HashMap<>();
        while (ccuGridIterator.hasNext()) {
            HRow ccuRow = (HRow) ccuGridIterator.next();
            String equipRef = ccuRow.get("equipRef").toString();
            equipRefs.add(equipRef);
            ccuIdMap.put(ccuRow.get("id").toString(), equipRef);
        }
        /*
        key - equipRef
        Value - ccuVersion
         */
        Map<String, String> ccuVersionMap = restoreCCUHsApi.getCCUVersion(equipRefs);
        List<CCU> ccuList = new ArrayList<>();
        for (int index = 0; index < ccuArray.length(); index++) {
            try {
                JSONObject ccuDetails = ccuArray.getJSONObject(index);
                Date lastUpdatedDatetime = new Date(HDateTime.make(ccuDetails.getString("lastUpdatedDatetime")).millis());
                String ccuId = ccuDetails.getString("deviceId");
                ccuList.add(new CCU(siteCode, ccuId, ccuDetails.getString("deviceName"),
                        ccuVersionMap.get(ccuIdMap.get(ccuId)),
                        new SimpleDateFormat("MMM dd, yyyy | HH:mm:ss").format(lastUpdatedDatetime),
                        isCCUOnline(lastUpdatedDatetime)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        ccuList.sort(Comparator.comparing(CCU::getName));
        return ccuList;
    }

    public void getCCUEquip(String ccuId){
        restoreCCUHsApi.readCCUEquip(ccuId);
    }

    private HGrid getAllEquips(String ccuId, String siteCode){
        String gatewayRef = getGatewayRefFromCCU(ccuId, siteCode);
        String ahuRef = getAhuRefFromCCU(ccuId, siteCode);
        return restoreCCUHsApi.getAllEquips(ahuRef, gatewayRef);
    }

    public int equipCountsInCCU(String ccuId, String siteCode){
        HGrid equipGrid = getAllEquips(ccuId, siteCode);
        if(equipGrid == null){
            throw new NullHGridException("Null occurred while fetching count of Zone equips.");
        }
        int count = 0;
        Iterator equipGridIterator = equipGrid.iterator();
        while(equipGridIterator.hasNext()) {
            count++;
            equipGridIterator.next();
        }
        return count;
    }

    private String getAhuRefFromCCU(String ccuId, String siteCode){
        HGrid ccuGrid = getCcuHGrid(siteCode);
        Iterator ccuGridIterator = ccuGrid.iterator();
        while (ccuGridIterator.hasNext()) {
            HRow ccuRow = (HRow) ccuGridIterator.next();
            if(ccuId.equals(ccuRow.get("id").toString())){
                return ccuRow.get("ahuRef").toString();
            }
        }
        return null;
    }

    private String getGatewayRefFromCCU(String ccuId, String siteCode){
        HGrid ccuGrid = getCcuHGrid(siteCode);
        Iterator ccuGridIterator = ccuGrid.iterator();
        while (ccuGridIterator.hasNext()) {
            HRow ccuRow = (HRow) ccuGridIterator.next();
            if(ccuId.equals(ccuRow.get("id").toString())){
                return ccuRow.get("gatewayRef").toString();
            }
        }
        return null;
    }

    public void getSystemProfileOfCCU(String ccuId, String siteCode){
        Log.i(TAG, "Restoring system profile of CCU started");
        String ahuRef = getAhuRefFromCCU(ccuId, siteCode);
        HGrid systemGrid = restoreCCUHsApi.getCCUSystemEquip(ahuRef);
        if (systemGrid == null){
            throw new NullHGridException("Null occurred while fetching system profile.");
        }
        Iterator systemGridIterator = systemGrid.iterator();
        while(systemGridIterator.hasNext()){
            HRow systemEquipRow = (HRow) systemGridIterator.next();
            getEquipAndPoints(systemEquipRow);
        }
        Log.i(TAG, "Restoring system profile of CCU completed");
    }

    public void getCMDeviceOfCCU(String ccuId, String siteCode){
        Log.i(TAG, "Restoring CM device is started");
        String ahuRef = getAhuRefFromCCU(ccuId, siteCode);
        HGrid cmDeviceGrid = restoreCCUHsApi.getDevice(ahuRef);
        if(cmDeviceGrid == null){
            throw new NullHGridException("Null occurred while fetching CM device details.");
        }
        Iterator cmDeviceGridIterator = cmDeviceGrid.iterator();
        while(cmDeviceGridIterator.hasNext()) {
            HRow cmDeviceRow = (HRow) cmDeviceGridIterator.next();
            getDeviceAndPoints(cmDeviceRow);
        }
        Log.i(TAG, "Restoring CM device is completed");
    }

    public void getDiagEquipOfCCU(String ccuId, String siteCode){
        Log.i(TAG, "Restoring Diag equip is started");
        String gatewayRef = getGatewayRefFromCCU(ccuId, siteCode);
        HGrid diagEquipGrid = restoreCCUHsApi.getDiagEquip(gatewayRef);
        if(diagEquipGrid == null){
            throw new NullHGridException("Null occurred while fetching diag equip.");
        }
        Iterator diagEquipGridIterator = diagEquipGrid.iterator();
        while(diagEquipGridIterator.hasNext()) {
            HRow diagEquipRow = (HRow) diagEquipGridIterator.next();
            getEquipAndPoints(diagEquipRow);
        }
        Log.i(TAG, "Restoring Diag equip is completed");
    }

    public void getZoneEquipsOfCCU(String ccuId, String siteCode, int deviceCount,
                                   EquipResponseCallback equipResponseCallback){
        Set<String> equipRefSet = new HashSet<>();
        Set<String> floorRefSet = new HashSet<>();
        Set<String> roomRefSet = new HashSet<>();
        HGrid zoneEquipGrid = getZoneEquipGridWithAhuRef(ccuId, siteCode);
        if(zoneEquipGrid == null){
            throw new NullHGridException("Null occurred while fetching zone equips with AhuRef");
        }
        getZoneEquipsAndPoints(zoneEquipGrid, equipRefSet, floorRefSet, roomRefSet);
        HGrid zoneEquipGrid1 = getZoneEquipGridWithGatewayRef(ccuId, siteCode);
        if(zoneEquipGrid1 == null){
            throw new NullHGridException("Null occurred while fetching zone equips with GatewayRef");
        }
        getZoneEquipsAndPoints(zoneEquipGrid1, equipRefSet, floorRefSet, roomRefSet);
        restoreCCUHsApi.importFloors(floorRefSet);
        restoreCCUHsApi.importZones(roomRefSet);
        getZoneSchedules(roomRefSet);
        getDevicesFromEquips(equipRefSet, deviceCount, equipResponseCallback);
    }

    private HGrid getZoneEquipGridWithAhuRef(String ccuId, String siteCode){
        String ahuRef = getAhuRefFromCCU(ccuId, siteCode);
        return restoreCCUHsApi.getZoneEquipWithAhuRef(ahuRef);
    }

    private HGrid getZoneEquipGridWithGatewayRef(String ccuId, String siteCode){
        String gatewayRef = getGatewayRefFromCCU(ccuId, siteCode);
        return restoreCCUHsApi.getZoneEquipWithGatewayRef(gatewayRef);
    }

    public void getOAOEquip(String ccuId, String siteCode, int deviceCount,
                            EquipResponseCallback equipResponseCallback){
        HGrid oaoEquipGrid = getOAOEQuipGrid(ccuId, siteCode);
        if(oaoEquipGrid == null){
            throw new NullHGridException("Null occurred while fetching oao Equip grid.");
        }
        getDeviceFromEquip(oaoEquipGrid, deviceCount, equipResponseCallback);
    }

    private HGrid getOAOEQuipGrid(String ccuId, String siteCode){
        String ahuRef = getAhuRefFromCCU(ccuId, siteCode);
        return restoreCCUHsApi.getOAOEquip(ahuRef);
    }

    private void getDeviceFromEquip(HGrid equipGrid, int deviceCount, EquipResponseCallback equipResponseCallback) {
        if(equipGrid != null){
            Set<String> equipRefSet = new HashSet<>();
            Iterator equipGridIterator = equipGrid.iterator();
            while(equipGridIterator.hasNext()){
                HRow equipRow = (HRow) equipGridIterator.next();
                equipRefSet.add(equipRow.get("id").toString());
                getEquipAndPoints(equipRow);
                if(equipRow.has("modbus")){
                    saveToBox(equipRow);
                }
            }
            getDevicesFromEquips(equipRefSet, deviceCount, equipResponseCallback);
        }
    }

    private EquipmentDevice getFromBoxByOnEquipType(String equipType, String name) {
        return EquipsManager.getInstance().fetchProfileByEquipTypeAndName(equipType, name);
    }

    private EquipmentDevice getFromBoxByVendorAndModel(String vendor, String model) {
        return EquipsManager.getInstance().fetchProfileByVendorAndModel(vendor, model);
    }

    private String getModbusName(String equipDispName, String slaveId, String modbusEquipType){
        /*
        Name is fetched under the assumption of format in which dis(display name of modbus equip) is stored in
        siteDis + "-"+modbusName+"-"+modbusEquipType+"-" + slaveId
         */
        String zone = "_ZONE";
        if(modbusEquipType.endsWith(zone)){
            int end = modbusEquipType.lastIndexOf(zone);
            modbusEquipType = modbusEquipType.substring(0,end);

        }
        String siteDis = (String) CCUHsApi.getInstance().read(Tags.SITE).get("dis");
        return equipDispName.replace(siteDis+"-", "").replace("-"+modbusEquipType, "")
                .replace("-"+slaveId,"");
    }

    private void saveToBox(HRow equipRow){
        ArrayList<HashMap> mbDispPointList = CCUHsApi.getInstance().readAll("point and modbus and displayInUi and " +
                "shortDis  and equipRef == \""+equipRow.get("id").toString()+ "\"");
        String profile =  equipRow.get("profile").toString().replace("MODBUS_","");
        String modbusDisplayName = equipRow.get("dis").toString();
        String modbusName = getModbusName(modbusDisplayName, equipRow.get("group").toString(),
                profile);

        EquipmentDevice modbusDevice = (profile.equalsIgnoreCase("DEFAULT"))?
        getFromBoxByVendorAndModel(equipRow.get("vendor").toString(), equipRow.get("model").toString()):
                getFromBoxByOnEquipType(profile, modbusName);
        if(modbusDevice == null){
            throw new NullHGridException("Error while restoring Modbus with the display name : "+modbusDisplayName);
        }
        modbusDevice.setId(0);
        modbusDevice.setPaired(true);
        String zoneRef = equipRow.get("roomRef").toString().replace("@SYSTEM","SYSTEM");
        String floorRef = equipRow.get("floorRef").toString().replace("@SYSTEM","SYSTEM");
        modbusDevice.setEquipRef(equipRow.get("id").toString());
        modbusDevice.setZoneRef(zoneRef);
        modbusDevice.setFloorRef(floorRef);
        modbusDevice.setSlaveId(Integer.parseInt(equipRow.get("group").toString()));
        ModbusEquipsInfo modbusEquipsInfo = new ModbusEquipsInfo();
        modbusEquipsInfo.equipmentDevices = modbusDevice;
        modbusEquipsInfo.zoneRef = zoneRef;
        modbusEquipsInfo.equipRef = equipRow.get("id").toString();
        for(Register register :modbusDevice.getRegisters()){
            String desc = register.getParameters().get(0).name;
            for(HashMap mbDispPoint : mbDispPointList){
                if(mbDispPoint.get("shortDis").toString().equals(desc)){
                    register.getParameters().get(0).setDisplayInUI(true);
                }
                register.getParameters().get(0).setRegisterType(register.getRegisterType());
                register.getParameters().get(0).setRegisterAddress(register.getRegisterAddress());
            }
        }
        EquipsManager.getInstance().saveProfile(modbusDevice);
    }

    public void getModbusSystemEquip(String ccuId, String siteCode, int deviceCount,
                                     EquipResponseCallback equipResponseCallback){
        HGrid modbusEquipGrid = getModBusSystemEquipGrid(ccuId, siteCode);
        if(modbusEquipGrid  == null){
            throw new NullHGridException("Null occurred while fetching modbus");
        }
        getDeviceFromEquip(modbusEquipGrid, deviceCount, equipResponseCallback);
    }

    private HGrid getModBusSystemEquipGrid(String ccuId, String siteCode){
        String gatewayRef = getGatewayRefFromCCU(ccuId, siteCode);
        return restoreCCUHsApi.getModbusSystemEquip(gatewayRef);
    }

    private void getDevicesFromEquips(Set<String> equipRefList, int deviceCount,
                                      EquipResponseCallback equipResponseCallback){
       for(String equipRef : equipRefList){
           HGrid deviceGrid = restoreCCUHsApi.getDevice(equipRef);
           if(deviceGrid == null){
               throw new NullHGridException("Null occurred while fetching device.");
           }
           Iterator deviceGridIterator = deviceGrid.iterator();
           while(deviceGridIterator.hasNext()) {
               HRow zoneDeviceRow = (HRow) deviceGridIterator.next();
               getDeviceAndPoints(zoneDeviceRow);
               equipResponseCallback.onEquipRestoreComplete(--deviceCount);
           }
       }
    }

    private void getZoneEquipsAndPoints(HGrid zoneEquipGrid, Set<String> equipRefSet, Set<String> floorRefSet,
                                        Set<String> roomRefSet){
        if(zoneEquipGrid != null){
            Iterator zoneEquipGridIterator = zoneEquipGrid.iterator();
            while(zoneEquipGridIterator.hasNext()) {
                HRow zoneEquipRow = (HRow) zoneEquipGridIterator.next();
                equipRefSet.add(zoneEquipRow.get("id").toString());
                floorRefSet.add(zoneEquipRow.get("floorRef").toString());
                roomRefSet.add(zoneEquipRow.get("roomRef").toString());
                getEquipAndPoints(zoneEquipRow);
                if(zoneEquipRow.has("modbus")){
                    saveToBox(zoneEquipRow);
                }
            }
        }
    }

    private void getDeviceAndPoints(HRow deviceRow){
        restoreCCUHsApi.importDevice(deviceRow);
    }

    private void getEquipAndPoints(HRow equipRow){
        restoreCCUHsApi.importEquip(equipRow);
    }

    private void getZoneSchedules(Set<String> roomRefSet){
        restoreCCUHsApi.importZoneSchedule(roomRefSet);
    }

    public void syncExistingSite(String siteCode){
        Log.i(TAG, "Saving site details started");
        restoreCCUHsApi.syncExistingSite(siteCode);
        Log.i(TAG, "Saving site details completed");
    }

}
