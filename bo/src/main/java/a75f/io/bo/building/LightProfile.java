package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Set;

import a75f.io.bo.building.definitions.AlgoTuningParameters;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LightProfile extends ZoneProfile
{
    private HashMap<Short, LightProfileConfiguration> mLightProfileConfiguration =
            new HashMap<Short, LightProfileConfiguration>();
    
    
    public LightProfile()
    {
    }
    
    
    @Override
    public short mapCircuit(Output output)
    {
        short localDimmablePercent = resolveLogicalValue(output);
        //The smartnode circuit is in override mode, check to see if a schedule hasn't crossed a
        // bound.  If a schedule did cross a bound remove the override and continue.
        switch (output.getOutputType())
        {
            case Relay:
                switch (output.mOutputRelayActuatorType)
                {
                    case NormallyClose:
                        return (short) (localDimmablePercent != 0 ? 1 : 0);
                    ///Defaults to normally open
                    case NormallyOpen:
                        return (short) (localDimmablePercent != 0 ? 0 : 1);
                }
                break;
            case Analog:
                switch (output.mOutputAnalogActuatorType)
                {
                    case ZeroToTenV:
                        return localDimmablePercent;
                    case TenToZeroV:
                        return (short) (100 - localDimmablePercent);
                    case TwoToTenV:
                        return (short) (20 + scaleDimmablePercent(localDimmablePercent, 80));
                    case TenToTwov:
                        return (short) (100 - scaleDimmablePercent(localDimmablePercent, 80));
                }
                break;
        }
        return (short) 0;
    }
    
    
    @Override
    public void mapControls(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t)
    {
    }
    
    
    @Override
    public void mapSeed(CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
    {
        //TODO: for now these are public
        seedMessage.settings.lightingIntensityForOccupantDetected
                .set((short) AlgoTuningParameters.LIGHTING_INTENSITY_OCCUPANT_DETECTED);
        seedMessage.settings.minLightingControlOverrideTimeInMinutes
                .set((short) AlgoTuningParameters.MIN_LIGHTING_CONTROL_OVERRIDE_IN_MINUTES);
        seedMessage.settings.profileBitmap.lightingControl.set((short) 1);
    }
    
    
    @Override
    public void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t regularUpdateMessage)
    {
    }
    
    
    @Override
    @JsonIgnore
    public ProfileType getProfileType()
    {
        return ProfileType.LIGHT;
    }
    
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return mLightProfileConfiguration.get(address);
    }
    
    
    @Override
    public Set<Short> getNodeAddresses()
    {
        return mLightProfileConfiguration.keySet();
    }
    
    
    @Override
    public void removeProfileConfiguration(Short selectedModule)
    {
        mLightProfileConfiguration.remove(selectedModule);
    }
    
    
    public HashMap<Short, LightProfileConfiguration> getLightProfileConfiguration()
    {
        return mLightProfileConfiguration;
    }
    
    
    public void setLightProfileConfiguration(HashMap<Short, LightProfileConfiguration> lightProfileConfiguration)
    {
        this.mLightProfileConfiguration = lightProfileConfiguration;
    }
}
