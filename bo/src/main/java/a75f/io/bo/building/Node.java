package a75f.io.bo.building;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Node
{
    private short    mAddress;
    private NodeType mNodeType;
    private List<UUID> mInputs  = new ArrayList<>();
    private List<UUID> mOutputs = new ArrayList<>();
    
    
    public List<UUID> getInputs()
    {
        return mInputs;
    }
    
    
    public void setInputs(List<UUID> inputs)
    {
        this.mInputs = inputs;
    }
    
    
    public List<UUID> getOutputs()
    {
        return mOutputs;
    }
    
    
    public void setOutputs(List<UUID> outputs)
    {
        this.mOutputs = outputs;
    }
    
    
    public short getAddress()
    {
        return mAddress;
    }
    
    
    public void setAddress(short address)
    {
        this.mAddress = address;
    }
    
    
    public NodeType getNodeType()
    {
        return mNodeType;
    }
    
    
    public void setNodeType(NodeType nodeType)
    {
        this.mNodeType = nodeType;
    }
}
