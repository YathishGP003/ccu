package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.definitions.RoomDataInterface;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

/**
 * Created by Yinten isOn 8/15/2017.
 */
//Also known as room.
public class Zone
{
    
    public  String                  roomName          = "Default Zone";
    public  ArrayList<ZoneProfile>  mZoneProfiles     = new ArrayList<>();
    private HashMap<String, Object> mTuningParameters = new HashMap<>();
    @JsonIgnore
    private RoomDataInterface mRoomDataInterface;
    
    public Zone()
    {
    }
    
    //Also known as zone name.
    public Zone(String roomName)
    {
        this.roomName = roomName;
    }
    
    @JsonIgnore
    public ArrayList<Output> getOutputs(short address)
    {
        ArrayList<Output> retVal = new ArrayList<>();
        for (ZoneProfile zp : mZoneProfiles)
        {
            BaseProfileConfiguration profileConfiguration = zp.getProfileConfiguration(address);
            if (profileConfiguration != null)
            {
                retVal.addAll(profileConfiguration.getOutputs());
            }
        }
        return retVal;
    }
    
    @Override
    @JsonIgnore
    public String toString()
    {
        return roomName;
    }
    
    @JsonIgnore
    public ZoneProfile findProfile(ProfileType profileType)
    {
        ZoneProfile retVal = null;
        for (ZoneProfile zoneProfile : mZoneProfiles)
        {
            if(zoneProfile.getProfileType().equals(profileType))
            {
                return zoneProfile;
            }
        }
        
        

        return retVal;
    }
    
    @JsonIgnore
    public void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdate)
    {
        Short address = Short.valueOf((short) smartNodeRegularUpdate.update.smartNodeAddress.get());
        for (ZoneProfile zp : mZoneProfiles)
        {
            if (zp.getNodeAddresses().contains(address))
            {
                zp.mapRegularUpdate(smartNodeRegularUpdate);
            }
        }
    }
    
    @JsonIgnore
    public void removeNodeAndClearAssociations(Short selectedModule)
    {
        for (ZoneProfile zp : mZoneProfiles)
        {
            if (zp.getProfileConfiguration(selectedModule) != null)
            {
                zp.removeProfileConfiguration(selectedModule);
            }
        }
    }
    
    public HashSet<Short> getNodes()
    {
        HashSet<Short> addresses = new HashSet<Short>();
        for (ZoneProfile zp : mZoneProfiles)
        {
            addresses.addAll(zp.getNodeAddresses());
        }
        return addresses;
    }
    
    public HashMap<String, Object> getTuningParameters()
    {
        return mTuningParameters;
    }
    
    public void setTuningParameters(HashMap<String, Object> tuningParameters)
    {
        this.mTuningParameters = tuningParameters;
    }
    
    @JsonIgnore
    public double getDisplayCurrentTemp()
    {
        return 69;
    }
    
    @JsonIgnore
    public double getActualDesiredTemp()
    {
        return 64;
    }
    
    @JsonIgnore
    public void setRoomDataInterface(RoomDataInterface roomDataInterface)
    {
        this.mRoomDataInterface = roomDataInterface;
    }
    //Scratch
    //    @JsonIgnore
    //    public Output findPort(Port port, short smartNodeAddress)
    //    {
    //        for (Output output : getOutputs(smartNodeAddress))
    //        {
    //            if (output.getPort() == port)
    //            {
    //                output.mConfigured = true;
    //                return output;
    //            }
    //        }
    //        Output output = new Output();
    //        output.setPort(port);
    //        output.setAddress(smartNodeAddress);
    //        output.mConfigured = false;
    //        return output;
    //    }
}