package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;

/**
 * Created by Yinten on 9/29/2017.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "clazz")
public abstract class BaseProfileConfiguration
{
    
    protected short    mNodeAddress;
    protected NodeType mNodeType;
    protected ArrayList<Input>  mInputs  = new ArrayList<>();
    protected ArrayList<Output> mOutputs = new ArrayList<>();
    
    
    public ArrayList<Input> getInputs()
    {
        return mInputs;
    }
    
    public void setInputs(ArrayList<Input> inputs)
    {
        this.mInputs = inputs;
    }
    
    public ArrayList<Output> getOutputs()
    {
        return mOutputs;
    }
    
    public void setOutputs(ArrayList<Output> outputs)
    {
        this.mOutputs = outputs;
    }
    
    public NodeType getNodeType()
    {
        return mNodeType;
    }
    
    public void setNodeType(NodeType nodeType)
    {
        this.mNodeType = nodeType;
    }
    
    public short getNodeAddress()
    {
        return mNodeAddress;
    }
    
    public void setNodeAddress(short nodeAddress)
    {
        this.mNodeAddress = nodeAddress;
    }
}
