package a75f.io.bo.ble;

import android.util.Log;

import a75f.io.bo.interfaces.ISerial;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class GattPin implements ISerial {
    private static final String TAG = GattPin.class.getSimpleName();

    private int mPin;

    public GattPin() { /* Default Constructor */ }

    public GattPin(int pin) {
        mPin = pin;
    }

    public int getPin() {
        return mPin;
    }

    public void setPin(int pin) {
        this.mPin = pin;
    }

    @Override
    public void fromBytes(byte[] bytes) {
        mPin = 0;
        for (byte b : bytes) {
            int i = b % 16;
            mPin += i;
            mPin *= 10;
        }
        mPin /= 10;
        Log.d(TAG, "BLE PIN: " + mPin);

    }

    @Override
    public byte[] toBytes() {
        return new byte[0];
    }

    public static GattPin initialize(byte[] bytes)
    {
        GattPin gp = new GattPin();
        gp.fromBytes(bytes);
        return gp;
    }
}
