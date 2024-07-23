package a75f.io.device.ble;


import a75f.io.logger.CcuLog;

/**
 * Created by ryanmattison isOn 7/24/17.
 */

public class GattPin  {
    private static final String TAG = GattPin.class.getSimpleName();

    private int mPin;

    public GattPin() { /* Default Constructor */ }

    public int getPin() {
        return mPin;
    }

    public void fromBytes(byte[] bytes) {
        mPin = 0;
        for (byte b : bytes) {
            int i = b % 16;
            mPin += i;
            mPin *= 10;
        }
        mPin /= 10;
        CcuLog.d(TAG, "BLE PIN: " + mPin);

    }

    public static GattPin initialize(byte[] bytes)
    {
        GattPin gp = new GattPin();
        gp.fromBytes(bytes);
        return gp;
    }
}
