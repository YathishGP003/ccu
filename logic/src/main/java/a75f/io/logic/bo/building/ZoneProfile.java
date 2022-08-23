package a75f.io.logic.bo.building;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.RoomDataInterface;
import a75f.io.logic.bo.building.schedules.EquipOccupancyHandler;
import a75f.io.logic.bo.building.schedules.EquipScheduleHandler;
import a75f.io.logic.tuners.BuildingTunerCache;

import static a75f.io.logic.L.TAG_CCU_SCHEDULER;
import static a75f.io.logic.bo.building.ZoneState.COOLING;

public abstract class ZoneProfile
{
    public static final String TAG = ZoneProfile.class.getSimpleName();
    protected HashMap<Short, BaseProfileConfiguration> mProfileConfiguration = new HashMap<>();
    protected UUID uuid = UUID.randomUUID();
    public ZoneState    state    = COOLING;
    
    private EquipOccupancyHandler equipOccupancyHandler = null;
    private EquipScheduleHandler equipScheduleHandler = null;
    public ZoneProfile()
    {
    }
    
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
    
    /**
     * TODO - Refactor.
     * Currently this is repeated it all the Zone profile's. It should be replaced by a single common implementation
     * here.
     */
    public boolean isZoneDead() {
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
    
    public void updateOccupancy(CCUHsApi hayStack) {
        CcuLog.i(TAG_CCU_SCHEDULER, "UpdateOccupancy for Profile: "+getEquip().getDisplayName());
        if (equipScheduleHandler == null) {
            Equip currentEquip = getEquip();
            if (currentEquip == null) {
                CcuLog.e(L.TAG_CCU_SCHEDULER, "updateOccupancy failed: No equip for ZoneProfile");
                return;
            }
            equipScheduleHandler = new EquipScheduleHandler(hayStack, currentEquip.getId());
            equipOccupancyHandler = new EquipOccupancyHandler(hayStack, currentEquip.getId());
        }
        
        equipOccupancyHandler.updateOccupancy();
    }
    
    public EquipScheduleHandler getEquipScheduleHandler() {
        return equipScheduleHandler;
    }
    
    public EquipOccupancyHandler getEquipOccupancyHandler() {
        return equipOccupancyHandler;
    }
    
}

