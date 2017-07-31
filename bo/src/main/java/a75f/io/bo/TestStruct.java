package a75f.io.bo;

import org.javolution.io.Struct;

import a75f.io.bo.serial.MessageType;

/**
 * Created by ryanmattison on 7/31/17.
 */

public class TestStruct extends Struct {
    public final Unsigned8 messageType = new Unsigned8();
    //public final Unsigned16 smartNodeAddress = new Unsigned16();

}
