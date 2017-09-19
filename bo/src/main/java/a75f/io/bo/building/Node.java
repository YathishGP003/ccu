package a75f.io.bo.building;

import java.util.HashSet;
import java.util.UUID;

public class Node
{
    private short    mAddress;
    private NodeType mNodeType;
    private HashSet<UUID> mInputs  = new HashSet<>();
    private HashSet<UUID> mOutputs = new HashSet<>();
    
    
    public HashSet<UUID> getInputs()
    {
        return mInputs;
    }
    
    
    public void setInputs(HashSet<UUID> inputs)
    {
        this.mInputs = inputs;
    }
    
    
    public HashSet<UUID> getOutputs()
    {
        return mOutputs;
    }
    
    
    public void setOutputs(HashSet<UUID> outputs)
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
