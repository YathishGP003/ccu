package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Set;

import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LightProfile extends ZoneProfile
{
    private HashMap<Short, LightProfileConfiguration> mLightProfileConfiguration =
            new HashMap<>();
    
    
    public LightProfile()
    {
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
