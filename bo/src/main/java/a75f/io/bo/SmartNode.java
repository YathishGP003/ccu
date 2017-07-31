package a75f.io.bo;


import a75f.io.bo.interfaces.ISerial;

public class SmartNode implements ISerial {

    private byte[] mEncryptionKey;
    private String name;
    private Short mMeshAddress;

    public byte[] getEncryptionKey() {
        return mEncryptionKey;
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.mEncryptionKey = encryptionKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SmartNode() {
        /* Default Constructor */
    }

    public SmartNode(String name) {
        this.name = name;
    }


    @Override
    public void fromBytes(byte[] bytes) {
    }

    @Override
    public byte[] toBytes() {
        return new byte[0];
    }

    public short getMeshAddress() {
        return mMeshAddress;
    }

    public void setMeshAddress(short meshAddress)
    {
        mMeshAddress = meshAddress;
    }
}

