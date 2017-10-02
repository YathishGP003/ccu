package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by Yinten on 9/26/2017.
 */

public class AddressedStruct
{
    private short  mAddress;
    private Struct mStruct;
    
    public AddressedStruct(short address, Struct struct)
    {
        this.mAddress = address;
        this.mStruct = struct;
    }
    
    public short getAddress()
    {
        return mAddress;
    }
    public void setAddress(short address)
    {
        this.mAddress = address;
    }
    public Struct getStruct()
    {
        return mStruct;
    }
    public void setStruct(Struct struct)
    {
        this.mStruct = struct;
    }
}
