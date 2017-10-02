package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LightProfile extends ZoneProfile
{
    
    
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
    public LightProfileConfiguration getProfileConfiguration(short address)
    {
        return (LightProfileConfiguration) mProfileConfiguration.get(address);
    }
    
    
}
