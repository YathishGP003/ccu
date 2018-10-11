package a75f.io.logic.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75f.io.logic.bo.building.definitions.ProfileType;

/**
 * Created by Yinten on 10/1/2017.
 */

public class TestProfile extends ZoneProfile
{
    public TestProfile()
    {
    }
    
   /* @Override
    public void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t regularUpdateMessage)
    {
    }*/
    
    @Override
    public void updateZonePoints()
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
