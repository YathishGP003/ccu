package a75f.io.bo;

import a75f.io.bo.interfaces.ISerial;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class MessageEvent implements ISerial {
    @Override
    public void fromBytes(byte[] bytes) {

    }

    @Override
    public byte[] toBytes() {
        return new byte[0];
    }
}
