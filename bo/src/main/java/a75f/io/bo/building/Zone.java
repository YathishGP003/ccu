package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.MessageType;

/**
 * Created by Yinten isOn 8/15/2017.
 */
//Also known as room.
public class Zone
{
    public String roomName = "Default Zone";
    public LightProfile mLightProfile;
    
    private HashMap<Short, Node>  mNodes   = new HashMap<>();
    private HashMap<UUID, Input>  mInputs  = new HashMap<>();
    private HashMap<UUID, Output> mOutputs = new HashMap<>();
    private CcuToCmOverUsbSnControlsMessage_t[] controlsMessage;
    private CcuToCmOverUsbSnControlsMessage_t[] seedMessages;
    private CcuToCmOverUsbSnControlsMessage_t[] controlsMessages;
    
    
    public Zone()
    {
    }
    
    
    //Also known as zone name.
    public Zone(String roomName)
    {
        this.roomName = roomName;
    }
    
    
    public  Output findPort(Port port, short smartNodeAddress)
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
    
    
    public HashMap<UUID, Input> getInputs()
    {
        return mInputs;
    }
    
    
    public void setInputs(HashMap<UUID, Input> inputs)
    {
        mInputs = inputs;
    }
    
    
    @Override
    public String toString()
    {
        return roomName;
    }
    
    
    public ZoneProfile findLightProfile()
    {
        if (mLightProfile == null)
        {
            mLightProfile = new LightProfile();
        }
        return mLightProfile;
    }
    
    
    public ArrayList<Output> getOutputs(short address)
    {
        ArrayList<Output> retVal = new ArrayList<Output>();
        Node node = mNodes.get(address);
        for (UUID outputUUID : node.getOutputs())
        {
            retVal.add(getOutputs().get(outputUUID));
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
    
    

    
    
    public HashMap<Short, Node> getNodes()
    {
        return mNodes;
    }
    
    
    public void setNodes(HashMap<Short, Node> nodes)
    {
        mNodes = nodes;
    }
    
    

    
    
    public CcuToCmOverUsbSnControlsMessage_t[] getControlsMessages()
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
            getPort(controlsMessage_t, output.getPort()).set(mLightProfile.mapCircuit(output));
        }
        return (CcuToCmOverUsbSnControlsMessage_t[]) controlMessagesHash.values().toArray();
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
    
    
    public CcuToCmOverUsbDatabaseSeedSnMessage_t[] getSeedMessages()
    {
        CcuToCmOverUsbDatabaseSeedSnMessage_t[] seedMessages =
                new CcuToCmOverUsbDatabaseSeedSnMessage_t[mNodes.size()];
        int i = 0;
        for (Short address : mNodes.keySet())
        {
            seedMessages[i] = getSeedMessage(address);
            i++;
        }
        return seedMessages;
    }
    
    
    public CcuToCmOverUsbDatabaseSeedSnMessage_t getSeedMessage(short address)
    {
        CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage =
                new CcuToCmOverUsbDatabaseSeedSnMessage_t();
        seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
        seedMessage.settings.roomName.set(roomName);
        seedMessage.smartNodeAddress.set(address);
        if (mLightProfile != null)
        {
            mLightProfile.mapSeed(seedMessage);
        }
        return seedMessage;
    }
    
    
    public void addInputCircuit(Node node, ZoneProfile zoneProfile, Input circuit)
    {
        getInputs().put(circuit.getUuid(), circuit);
        zoneProfile.getOutputs().add(circuit.uuid);
        this.getNodes().put(node.getAddress(), node);
    }
    
    public void removeOutputCircuit(Output output, ZoneProfile zoneProfile)
    {
        getOutputs().remove(output.getUuid());
        zoneProfile.getOutputs().remove(output.uuid);
        getNodes().remove(output.getAddress());
    }
    
    public void addOutputCircuit(Node node, ZoneProfile zoneProfile, Output output)
    {
        getOutputs().put(output.getUuid(), output);
        zoneProfile.getOutputs().add(output.uuid);
        this.getNodes().put(node.getAddress(), node);
    }
    
    public void removeInputCircuit(Input output, ZoneProfile zoneProfile)
    {
        getOutputs().remove(output.getUuid());
        zoneProfile.getOutputs().remove(output.uuid);
        getNodes().remove(output.getAddress());
    }
}