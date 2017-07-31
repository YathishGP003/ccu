package a75f.io.serial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;

import a75f.io.util.Globals;

import static a75f.io.serial.SerialCommManager.EOF_BYTE;
import static a75f.io.serial.SerialCommManager.ESC_BYTE;
import static a75f.io.serial.SerialCommManager.SOF_BYTE;

/**
 * Created by samjithsadasivan on 7/31/17.
 */

public class CMSerialDevice {

    public static final String TAG = "CMSerialDevice";
    public static final boolean DEBUG_SERIAL_XFER = true;
                                //Log.isLoggable(TAG, Log.VERBOSE);


    UsbDevice mDevice = null;
    UsbEndpoint mEpIN = null;
    UsbEndpoint mEpOUT = null;
    UsbDeviceConnection mUsbConnection = null;
    UsbManager mUsbManager = null;

    public CMSerialDevice() {

    }

    public boolean open(UsbDevice usbDevice) {

        mUsbConnection = mUsbManager.openDevice(usbDevice);
        if (mUsbConnection == null) {
            Log.e(TAG, "Failed to open device, Shutting down service");
            return false;
        }
        for (int ifIndex = 0; ifIndex < usbDevice.getInterfaceCount(); ifIndex++) {

            if (!mUsbConnection.claimInterface(usbDevice.getInterface(ifIndex), true)) {
                Log.e(TAG, "Claim interface failed for " + ifIndex);
                continue;
            }
            if (String.format("%04X:%04X", usbDevice.getVendorId(),
                    usbDevice.getProductId()).equals(SerialCommManager.FTDI_VID_PID)) {
                if (mUsbConnection.controlTransfer(0x40, 0, 0, 0, null, 0, 0) < 0)//reset
                    Log.d(TAG, "control transfer 1 failed");
                if (mUsbConnection.controlTransfer(0x40, 0, 1, 0, null, 0, 0) < 0)//clear Rx
                    Log.d(TAG, "control transfer 2 failed");
                if (mUsbConnection.controlTransfer(0x40, 0, 2, 0, null, 0, 0) < 0)
                    Log.d(TAG, "control transfer 3 failed");
                if (mUsbConnection.controlTransfer(0x40, 0x03, 0xC04E, 0, null, 0, 0) < 0)//baudrate 38400
                    Log.d(TAG, "control transfer 4 failed");
            }
            UsbInterface usbIf = usbDevice.getInterface(ifIndex);
            for (int epIndex = 0; epIndex < usbIf.getEndpointCount(); epIndex++) {
                switch(usbIf.getEndpoint(epIndex).getType()) {
                    case UsbConstants.USB_ENDPOINT_XFER_BULK:
                        Log.d(TAG, "Bulk Endpoint");
                        if (usbIf.getEndpoint(epIndex).getDirection() == UsbConstants.USB_DIR_IN)
                            mEpIN = usbIf.getEndpoint(epIndex);
                        else
                            mEpOUT = usbIf.getEndpoint(epIndex);
                        break;
                    case UsbConstants.USB_ENDPOINT_XFER_CONTROL:
                        Log.d(TAG, "Control Endpoint");
                        break;
                    case UsbConstants.USB_ENDPOINT_XFER_INT:
                        Log.d(TAG, "Interrupt Endpoint");
                        break;
                    default:
                        Log.d(TAG, "Endpoint invalid");
                }

            }
        }
        if ((mEpIN == null) || (mEpOUT == null)) {
            Toast.makeText(Globals.getInstance().getApplicationContext(),
                                    R.string.no_endpoints_found, Toast.LENGTH_SHORT).show();
            return false;
        } else
            Toast.makeText(Globals.getInstance().getApplicationContext(), String.format("Endpoints"
                    + " found IN: 0x%02X, OUT: 0x%02X", mEpIN.getAddress(), mEpOUT.getAddress()), Toast.LENGTH_SHORT).show();
        return true;

    }

    public void write(byte[] byteArray) {

        byte buffer[] = new byte[1024];
        byte crc = 0;
        byte nOffset = 0;
        int len = byteArray.length;
        buffer[nOffset++] = (byte) (ESC_BYTE & 0xff);
        buffer[nOffset++] = (byte) (SOF_BYTE & 0xff);
        buffer[nOffset++] = (byte) (len & 0xff);

        for (int i = 0; i < len; i++) {
            buffer[i + nOffset] = byteArray[i]; // add payload to the tx buffer
            crc ^= byteArray[i];             // calculate the new crc
            if (byteArray[i] == (byte) (ESC_BYTE & 0xff)) // if the data is equal to ESC byte then add another instance of that
            {
                nOffset++;
                buffer[i + nOffset] = byteArray[i];
            }
        }
        buffer[nOffset + len] = (byte) (crc & 0xff);
        nOffset++;
        buffer[nOffset + len] = (byte) (ESC_BYTE & 0xff);
        nOffset++;
        buffer[nOffset + len] = (byte) (EOF_BYTE & 0xff);
        nOffset++;

        if (DEBUG_SERIAL_XFER) {
            String dp = "";
            for (int n = 0; n < nOffset + len; n++)
                dp = dp + " " + String.valueOf((int) (buffer[n] & 0xff));
            Calendar curDate = GregorianCalendar.getInstance();
            Log.v(TAG, "[" + (nOffset + len) + "]-[" + curDate.get(Calendar.HOUR_OF_DAY) + ":" + curDate.get(Calendar.MINUTE) + "] :" + dp);
        }

        mUsbConnection.bulkTransfer(mEpOUT, buffer, nOffset + len, 0);

    }

    public int read(byte[] rcvArray) {
        return mUsbConnection.bulkTransfer(mEpIN, rcvArray, rcvArray.length, 0);
    }
}
