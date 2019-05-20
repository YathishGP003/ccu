package a75f.io.logic.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import a75f.io.api.haystack.Equip;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.RoomDataInterface;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.DEADBAND;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "clazz")
public abstract class ZoneProfile extends Schedulable
{
    public static final String TAG = ZoneProfile.class.getSimpleName();
    protected HashMap<Short, BaseProfileConfiguration> mProfileConfiguration = new HashMap<>();
    @JsonIgnore
    protected UUID uuid = UUID.randomUUID();
    @JsonIgnore
    protected boolean mIsCircuitTest = false;
    
    @JsonIgnore
    public ZoneState    state    = DEADBAND;
    
    public ZoneProfile()
    {
    }
    
    
    public abstract void updateZonePoints();
    
    
    public abstract ProfileType getProfileType();
    
    
    public abstract <T extends BaseProfileConfiguration> T getProfileConfiguration(short address);
    
    @JsonIgnore
    public boolean isCircuitTest()
    {
        return mIsCircuitTest;
    }
    
    @JsonIgnore
    public void setCircuitTest(boolean isCircuitTest)
    {
        this.mIsCircuitTest = isCircuitTest;
    }
    
    //MARK
    @JsonIgnore
    protected RoomDataInterface mInterface;
    //MARK
    @JsonIgnore
    public void setZoneProfileInterface(RoomDataInterface zoneProfileInterface)
    {
        mInterface = zoneProfileInterface;
    }
    
    
    @JsonIgnore
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
    
    public boolean isZoneDead() {
        return false;
    }
    
    @JsonIgnore
    public double getDisplayCurrentTemp()
    {
        return 0;
    }
    
    /*@JsonIgnore
    public void updateZoneControls(double desiredVal) {
    }*/
    
    @JsonIgnore
    public HashMap<String, Double> getTSData() {
        return null;
    }
    
    @JsonIgnore
    public ZonePriority getPriority() {
        return ZonePriority.LOW;
    }
    
    @JsonIgnore
    public double getAverageZoneTemp() {
        return 72; //TODO- TEMP
    }
    
    @JsonIgnore
    public double getCurrentTemp() {
        return 0;
    }
    
    @JsonIgnore
    public Equip getEquip() {
        return null;
    }
    
    @JsonIgnore
    public ZoneState getState() {
        return state;
    }
    
    public boolean buildingLimitMinBreached() {
        double buildingLimitMin =  TunerUtil.readTunerValByQuery("building and limit and min", L.ccu().systemProfile.getSystemEquipRef());
        double tempDeadLeeway = TunerUtil.readTunerValByQuery("temp and dead and leeway",L.ccu().systemProfile.getSystemEquipRef());
    
        double currentTemp = getCurrentTemp();
        if (currentTemp < buildingLimitMin && currentTemp > (buildingLimitMin-tempDeadLeeway)) {
            return true;
        }
        return false;
    }
    
    public boolean buildingLimitMaxBreached() {
        double buildingLimitMax =  TunerUtil.readTunerValByQuery("building and limit and max", L.ccu().systemProfile.getSystemEquipRef());
        double tempDeadLeeway = TunerUtil.readTunerValByQuery("temp and dead and leeway",L.ccu().systemProfile.getSystemEquipRef());
    
        double currentTemp = getCurrentTemp();
        if (currentTemp > buildingLimitMax && currentTemp < (buildingLimitMax+tempDeadLeeway)) {
            return true;
        }
        return false;
    }
}

