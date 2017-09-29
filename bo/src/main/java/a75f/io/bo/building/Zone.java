package a75f.io.bo.building;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.bo.serial.MessageType;

/**
 * Created by Yinten isOn 8/15/2017.
 */
//Also known as room.
public class Zone
{
    public String                 roomName      = "Default Zone";
    public ArrayList<ZoneProfile> mZoneProfiles = new ArrayList<>();
    
    
    public Zone()
    {
    }
    
    //Also known as zone name.
    public Zone(String roomName)
    {
        this.roomName = roomName;
    }
    
    @JsonIgnore
    public Output findPort(Port port, short smartNodeAddress)
    {
        for (Output output : getOutputs(smartNodeAddress))
        {
            if (output.getPort() == port)
            {
                output.mConfigured = true;
                return output;
            }
        }
        Output output = new Output();
        output.setPort(port);
        output.setAddress(smartNodeAddress);
        output.mConfigured = false;
        return output;
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
            if (zoneProfile.getProfileType() == ProfileType.LIGHT)
            {
                return zoneProfile;
            }
        }
        switch (profileType)
        {
            case LIGHT:
                retVal = new LightProfile();
                break;
            case SSE:
                retVal = new SingleStageProfile();
                break;
        }
        mZoneProfiles.add(retVal);
        return retVal;
    }
    
    
    @JsonIgnore
    public Collection<CcuToCmOverUsbSnControlsMessage_t> getControlsMessages()
    {
        HashMap<Short, CcuToCmOverUsbSnControlsMessage_t> controlMessagesHash = new HashMap<>();
        for (ZoneProfile zp : mZoneProfiles)
        {
            for (short node : zp.getNodeAddresses())
            {
                CcuToCmOverUsbSnControlsMessage_t controlsMessage_t;
                if (controlMessagesHash.containsKey(node))
                {
                    controlsMessage_t = controlMessagesHash.get(node);
                }
                else
                {
                    controlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
                    controlMessagesHash.put(node, controlsMessage_t);
                    controlsMessage_t.smartNodeAddress.set(node);
                    controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
                }
                for (Output output : zp.getProfileConfiguration(node).getOutputs())
                {
                    getPort(controlsMessage_t, output.getPort()).set(zp.mapCircuit(output));
                }
            }
        }
        return controlMessagesHash.values();
    }
    
    
    @JsonIgnore
    private Struct.Unsigned8 getPort(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
                                     Port smartNodePort)
    {
        switch (smartNodePort)
        {
            case ANALOG_OUT_ONE:
                return controlsMessage_t.controls.analogOut1;
            case ANALOG_OUT_TWO:
                return controlsMessage_t.controls.analogOut2;
            case RELAY_ONE:
                return controlsMessage_t.controls.digitalOut1;
            case RELAY_TWO:
                return controlsMessage_t.controls.digitalOut2;
            default:
                return null;
        }
    }
    
    
    @JsonIgnore
    public CcuToCmOverUsbDatabaseSeedSnMessage_t[] getSeedMessages(byte[] encryptionKey)
    {
        //This needs to pair using module tuners as well..
        HashSet<Short> addresses = getNodes(); 
        CcuToCmOverUsbDatabaseSeedSnMessage_t[] seedMessages =
                new CcuToCmOverUsbDatabaseSeedSnMessage_t[addresses.size()];
        int i = 0;
        for (Short address : addresses)
        {
            seedMessages[i] = getSeedMessage(encryptionKey, address);
            i++;
        }
        return seedMessages;
    }
    
    
    @JsonIgnore
    public CcuToCmOverUsbDatabaseSeedSnMessage_t getSeedMessage(byte[] encryptionKey, short address)
    {
        CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage =
                new CcuToCmOverUsbDatabaseSeedSnMessage_t();
        seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
        seedMessage.settings.roomName.set(roomName);
        seedMessage.smartNodeAddress.set(address);
        seedMessage.putEncrptionKey(encryptionKey);
        Log.i("ZONE", "Zone Name: " + roomName);
        for (ZoneProfile zp : mZoneProfiles)
        {
            zp.mapSeed(seedMessage);
        }
        return seedMessage;
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
}