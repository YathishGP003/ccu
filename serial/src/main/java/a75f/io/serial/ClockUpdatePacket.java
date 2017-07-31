package a75f.io.serial;

import java.util.Calendar;
import java.util.GregorianCalendar;

import a75f.io.bo.interfaces.ISerial;
import a75f.io.util.GlobalUtils;

import static a75f.io.serial.SerialCommManager.MESSAGETYPE.CCU_HEARTBEAT_UPDATE;

/**
 * Created by samjithsadasivan on 7/27/17.
 */

class ClockUpdatePacket implements ISerial {
    byte[] mByteArray;

    public ClockUpdatePacket build() {
        mByteArray = new byte[4];
        mByteArray[0] = (byte) CCU_HEARTBEAT_UPDATE.ordinal();

        Calendar curDate = GregorianCalendar.getInstance();
        mByteArray[1] = (byte) (GlobalUtils.getCurrentDayOfWeekWithMondayAsStart() & 0xff);
        mByteArray[2] = (byte) (curDate.get(Calendar.HOUR_OF_DAY) & 0xff);
        mByteArray[3] = (byte) (curDate.get(Calendar.MINUTE) & 0xff);
        return this;
    }

    public byte[] toBytes() {
        if (mByteArray == null)
            throw new IllegalStateException("Packet not created");
        return mByteArray;
    }
    public void fromBytes(byte[] ip) {

    }

}
