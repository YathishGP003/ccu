package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

/**
 * Created by Yinten on 10/1/2017.
 */

public class TestProfile extends ZoneProfile
{
    public TestProfile()
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
        return ProfileType.TEST;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return mProfileConfiguration.get(address);
    }

    
}
