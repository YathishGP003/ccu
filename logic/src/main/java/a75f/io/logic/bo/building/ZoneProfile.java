package a75f.io.logic.bo.building;

import static a75f.io.logic.bo.building.ZoneState.COOLING;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HisItem;
import a75f.io.domain.config.ProfileConfiguration;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.RoomDataInterface;
import a75f.io.logic.bo.building.schedules.EquipOccupancyHandler;
import a75f.io.logic.bo.building.schedules.EquipScheduleHandler;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;

public abstract class ZoneProfile
{
    public static final String TAG = ZoneProfile.class.getSimpleName();
    protected HashMap<Short, BaseProfileConfiguration> mProfileConfiguration = new HashMap<>();
    protected UUID uuid = UUID.randomUUID();
    public ZoneState    state    = COOLING;
    
    private EquipOccupancyHandler equipOccupancyHandler = null;
    private EquipScheduleHandler equipScheduleHandler = null;
    public String RFDead = "RF Signal Dead";
    public String ZoneTempDead = "Zone Temp Dead";
    public ZoneProfile()
    {
    }

    public ZoneProfile(String equipRef, short nodeAddress) {}
    
    public abstract void updateZonePoints();
    
    public abstract ProfileType getProfileType();
    
    public abstract <T extends BaseProfileConfiguration> T getProfileConfiguration(short address);
    
    protected RoomDataInterface mInterface;
    
    public void setZoneProfileInterface(RoomDataInterface zoneProfileInterface)
    {
        mInterface = zoneProfileInterface;
    }
    
    public Set<Short> getNodeAddresses()
    {
        return mProfileConfiguration.keySet();
    }
    
    
    public HashMap<Short, BaseProfileConfiguration> getProfileConfiguration()
    {
        return mProfileConfiguration;
    }
    
    public ProfileConfiguration getDomainProfileConfiguration() {
        return null;
    }
    public void setProfileConfiguration(HashMap<Short, BaseProfileConfiguration> baseProfileConfiguration)
    {
        this.mProfileConfiguration = baseProfileConfiguration;
    }
    
    
    public void removeProfileConfiguration(Short selectedModule)
    {
        this.mProfileConfiguration.remove(selectedModule);
    }
    
    
    public void refreshRoomDataInterface() {
        if (mInterface != null)
            mInterface.refreshView();
    }
    public boolean isRFDead() {
        Equip equip = getEquip();
        if (equip == null) {
            CcuLog.e(L.TAG_CCU_ZONE, "Profile does not have linked equip , assume RF is dead");
            return true;
        }

        HashMap<Object, Object> point = CCUHsApi.getInstance().readEntity("point and (heartbeat or heartBeat) and equipRef == \""+equip.getId()+"\"");
        if(!point.isEmpty()){
            HisItem hisItem = CCUHsApi.getInstance().curRead(point.get("id").toString());
            if (hisItem == null) {
                CcuLog.e(L.TAG_CCU_ZONE, "RF dead! , Heartbeat does not exist for "+equip.getDisplayName());
                return true;
            }
            double zoneDeadTime = TunerUtil.readTunerValByQuery("zone and dead and time",
                                                            equip.getId());
            if (zoneDeadTime == 0) {
                CcuLog.e(L.TAG_CCU_ZONE, "Invalid value for zoneDeadTime tuner, use default "+equip.getDisplayName());
                zoneDeadTime = 15;
            }
            if ((System.currentTimeMillis() - hisItem.getDateInMillis()) > zoneDeadTime * 60 * 1000) {
                CcuLog.e(L.TAG_CCU_ZONE, "RF dead! , Heartbeat "+hisItem.getDate().toString()+" "+equip.getDisplayName()+" "+zoneDeadTime);
                return true;
            }
        }
        return false;
    }
    /**
     * TODO - Refactor.
     * Currently this is repeated it all the Zone profile's. It should be replaced by a single common implementation
     * here.
     */
    public boolean isZoneDead() {

        Equip equip = getEquip();
        double buildingLimitMax =  BuildingTunerCache.getInstance().getBuildingLimitMax();
        double buildingLimitMin =  BuildingTunerCache.getInstance().getBuildingLimitMin();
        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
        double currentTemp = CCUHsApi.getInstance().readHisValByQuery("sensor and (current or space) and temp and equipRef == \""+equip.getId()+"\"");

        if (currentTemp > (buildingLimitMax + tempDeadLeeway)
                || currentTemp < (buildingLimitMin - tempDeadLeeway)) {
            CcuLog.e(L.TAG_CCU_ZONE, "Equip dead : "+equip.getDisplayName()+" currentTemp "+currentTemp);
            return true;
        }
        return false;
    }
    
    public double getDisplayCurrentTemp()
    {
        return 0;
    }
    
    public ZonePriority getPriority() {
        return ZonePriority.LOW;
    }
    
    public double getAverageZoneTemp() {
        return 72; //TODO- TEMP
    }
    
    public double getCurrentTemp() {
        return 0;
    }
    
    public Equip getEquip() {
        return null;
    }
    
    public ZoneState getState() {
        return state;
    }
    
    public boolean buildingLimitMinBreached() {
        double buildingLimitMin = BuildingTunerCache.getInstance().getBuildingLimitMin();
        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
    
        double currentTemp = getCurrentTemp();
        return currentTemp < buildingLimitMin && currentTemp > (buildingLimitMin - tempDeadLeeway);
    }
    
    public boolean buildingLimitMaxBreached() {
        double buildingLimitMax =  BuildingTunerCache.getInstance().getBuildingLimitMax();
        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
    
        double currentTemp = getCurrentTemp();
        return currentTemp > buildingLimitMax && currentTemp < (buildingLimitMax + tempDeadLeeway);
    }
    
    public void reset(){
    }
    
    /**
     * When a Zone has damperOverride active , System does not normazlize or adjust the damper further.
     */
    public boolean isDamperOverrideActive() {
        return false;
    }
    
    public void updateOccupancy(CCUHsApi hayStack, boolean drActivated) {
        if (equipScheduleHandler == null || equipOccupancyHandler == null) {
            Equip currentEquip = getEquip();
            if (currentEquip == null) {
                CcuLog.e(L.TAG_CCU_SCHEDULER, "updateOccupancy failed: No equip for ZoneProfile");
                return;
            }
            equipScheduleHandler = new EquipScheduleHandler(hayStack, currentEquip.getId());
            equipOccupancyHandler = new EquipOccupancyHandler(hayStack, currentEquip.getId());
        }
        
        equipOccupancyHandler.updateOccupancy(drActivated);
    }
    
    public EquipScheduleHandler getEquipScheduleHandler() {
        return equipScheduleHandler;
    }
    
    public EquipOccupancyHandler getEquipOccupancyHandler() {
        return equipOccupancyHandler;
    }
    
}

