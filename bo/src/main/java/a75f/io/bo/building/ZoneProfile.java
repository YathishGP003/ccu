package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Set;
import java.util.UUID;

import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "clazz")
public abstract class ZoneProfile extends Schedulable {

    public static final String TAG = ZoneProfile.class.getSimpleName();
    @JsonIgnore
    protected UUID uuid = UUID.randomUUID();
    

    public ZoneProfile() {
    }

    protected short resolveLogicalValue(Output output) {
        //If an output is overiden, but doesn't have schedules, it should fall back onto the zone profile schedules to see if it can release it's override.
        //  or if it has schedules, it should use the circuit's logical values.  This behavior can be overrode at a specific profile level.
        if ((output.isOverride() && !output.hasSchedules() && !output.checkOverrides(mSchedules))
                || output.hasSchedules()) {
            return output.resolveLogicalValue();
        }
        else
        {
            return this.resolveLogicalValue();
        }
    }

    @JsonIgnore
    public abstract short mapCircuit(Output output);

    protected short scaleDimmablePercent(short localDimmablePercent, int scale) {
        return (short) ((float) scale * ((float) localDimmablePercent / 100.0f));
    }

    public abstract void mapControls(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t);

    public abstract void mapSeed(CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage);

    public abstract void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t regularUpdateMessage);

    public abstract ProfileType getProfileType();
    
    
    public abstract BaseProfileConfiguration getProfileConfiguration(short address);
    
    public abstract Set<Short> getNodeAddresses();
    
    public abstract void removeProfileConfiguration(Short selectedModule);
    
}
