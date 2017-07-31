package a75f.io.bo.ble;

import org.javolution.io.Struct;

import a75f.io.bo.serial.SerialConsts;

/**
 * Created by ryanmattison on 7/31/17.
 */

public class BLERoomName extends Struct {
    public final Struct.UTF8String roomName = new Struct.UTF8String(SerialConsts.ROOM_NAME_MAX_LENGTH);


}
