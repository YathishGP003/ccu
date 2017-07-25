package a75f.io.bo;


import a75f.io.bo.interfaces.ISerial;

public class SmartNode implements ISerial {

    private int[] mEncryptionKey;


    @Override
    public void fromBytes(byte[] bytes) { }

    @Override
    public byte[] toBytes() {
        return new byte[0];
    }
}

