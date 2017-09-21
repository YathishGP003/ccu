package a75f.io.bo.building;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

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
    public ArrayList<ZoneProfile> mZoneProfiles = new ArrayList<ZoneProfile>();
    
    private HashMap<Short, Node>  mNodes   = new HashMap<>();
    private HashMap<UUID, Input>  mInputs  = new HashMap<>();
    private HashMap<UUID, Output> mOutputs = new HashMap<>();
    //    private CcuToCmOverUsbSnControlsMessage_t[] controlsMessage;
    //    private CcuToCmOverUsbSnControlsMessage_t[] seedMessages;
    //    private CcuToCmOverUsbSnControlsMessage_t[] controlsMessages;
    //
    
    
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
    
    
    public ArrayList<Output> getOutputs(short address)
    {
        ArrayList<Output> retVal = new ArrayList<>();
        if (mNodes.containsKey(address))
        {
            Node node = mNodes.get(address);
            for (UUID outputUUID : node.getOutputs())
            {
                retVal.add(getOutputs().get(outputUUID));
            }
        }
        return retVal;
    }
    
    
    public HashMap<UUID, Output> getOutputs()
    {
        return mOutputs;
    }
    
    
    public void setOutputs(HashMap<UUID, Output> outputs)
    {
        this.mOutputs = outputs;
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
        for (Output output : this.getOutputs().values())
        {
            CcuToCmOverUsbSnControlsMessage_t controlsMessage_t;
            if (controlMessagesHash.containsKey(output.getAddress()))
            {
                controlsMessage_t = controlMessagesHash.get(output.getAddress());
            }
            else
            {
                controlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
                controlMessagesHash.put(output.getAddress(), controlsMessage_t);
                controlsMessage_t.smartNodeAddress.set(output.getAddress());
                controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
            }
            for (ZoneProfile zp : mZoneProfiles)
            {
                if (zp.getOutputs().contains(output.getUuid()))
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
        CcuToCmOverUsbDatabaseSeedSnMessage_t[] seedMessages =
                new CcuToCmOverUsbDatabaseSeedSnMessage_t[mNodes.size()];
        int i = 0;
        for (Short address : mNodes.keySet())
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
            if (usesSmartNode(address, zp))
            {
                zp.mapSeed(seedMessage);
            }
        }
        return seedMessage;
    }
    
    
    private boolean usesSmartNode(short address, ZoneProfile zp)
    {
        for (UUID uuid : zp.getOutputs())
        {
            if (mOutputs.containsKey(uuid))
            {
                if (address == mOutputs.get(uuid).getAddress())
                {
                    return true;
                }
            }
        }
        for (UUID uuid : zp.getInputs())
        {
            if (mInputs.containsKey(uuid))
            {
                if (address == mInputs.get(uuid).getAddress())
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    public void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdate)
    {
        short address = (short) smartNodeRegularUpdate.update.smartNodeAddress.get();
        for (ZoneProfile zp : mZoneProfiles)
        {
            if (usesSmartNode(address, zp))
            {
                zp.mapRegularUpdate(smartNodeRegularUpdate);
            }
        }
    }
    
    
    @JsonIgnore
    public void addInputCircuit(Node node, ZoneProfile zoneProfile, Input circuit)
    {
        circuit.setAddress(node.getAddress());
        getInputs().put(circuit.getUuid(), circuit);
        zoneProfile.getOutputs().add(circuit.uuid);
        this.getNodes().put(node.getAddress(), node);
    }
    
    
    public HashMap<UUID, Input> getInputs()
    {
        return mInputs;
    }
    
    
    
    public void setInputs(HashMap<UUID, Input> inputs)
    {
        mInputs = inputs;
    }
    
    
    @JsonProperty("nodes")
    public HashMap<Short, Node> getNodes()
    {
        return mNodes;
    }
    
    
    @JsonProperty("nodes")
    public void setNodes(HashMap<Short, Node> nodes)
    {
        Log.i("Nodes", "Set nodes! " + nodes.size());
        this.mNodes = nodes;
    }
    
    
    @JsonIgnore
    public void removeOutputCircuit(Output output, ZoneProfile zoneProfile)
    {
        getOutputs().remove(output.getUuid());
        zoneProfile.getOutputs().remove(output.uuid);
        getNodes().get(output.getAddress()).getOutputs().remove(output.getUuid());
    }
    
    
    @JsonIgnore
    public void addOutputCircuit(Node node, ZoneProfile zoneProfile, Output output)
    {
        output.setAddress(node.getAddress());
        getOutputs().put(output.getUuid(), output);
        zoneProfile.getOutputs().add(output.uuid);
        node.getOutputs().add(output.uuid);
    }
    
    
    @JsonIgnore
    public void removeInputCircuit(Input input, ZoneProfile zoneProfile)
    {
        getOutputs().remove(input.getUuid());
        zoneProfile.getOutputs().remove(input.uuid);
        getNodes().remove(input.getAddress());
        getNodes().get(input.getAddress()).getInputs().remove(input.getUuid());
    }
    
    
    @JsonIgnore
    public Node getSmartNode(Short mSmartNodeAddress)
    {
        if (mNodes.containsKey(mSmartNodeAddress))
        {
            Log.i("NODE", "Contains Key");
            return mNodes.get(mSmartNodeAddress);
        }
        else
        {
            Log.i("NODE", "New node");
            Node node = new Node();
            node.setAddress(mSmartNodeAddress);
            mNodes.put(mSmartNodeAddress, node);
            return node;
        }
    }
    
    
    public void removeNodeAndClearAssociations(Short selectedModule)
    {
        Node nodeToDelete = getNodes().get(selectedModule);
        for (UUID uuid : nodeToDelete.getOutputs())
        {
            removeCircuit(uuid);
        }
        getNodes().remove(selectedModule);
    }
    
    
    @JsonIgnore
    public void removeCircuit(UUID uuid)
    {
        getOutputs().remove(uuid);
        getInputs().remove(uuid);
        for(ZoneProfile zp : mZoneProfiles)
        {
            zp.getOutputs().remove(uuid);
            zp.getInputs().remove(uuid);
        }
    }
}