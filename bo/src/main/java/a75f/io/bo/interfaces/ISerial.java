package a75f.io.bo.interfaces;

/**
 * Created by ryanmattison on 7/20/17.
 */

public interface ISerial {

    void fromBytes(byte[] bytes);
    byte[] toBytes();

}
