package a75f.io.logic.bo.building;

public class Node
{
    private short    mAddress;
    private NodeType mNodeType;
    
    
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
