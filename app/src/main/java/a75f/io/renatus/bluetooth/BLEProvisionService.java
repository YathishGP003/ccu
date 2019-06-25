package a75f.io.renatus.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by ryanmattison on 7/25/17.
 */

public class BLEProvisionService extends Service
{
    
    private final static String TAG = "FragmentBLEDevicePin";
    
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String           mBluetoothDeviceAddress;
    private BluetoothGatt    mBluetoothGatt;
    
    
    
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BLEAction intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = BLEAction.ACTION_GATT_CONNECTED;
                //      tSleep();
                broadcastUpdate(intentAction, null);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                
                Log.i(TAG, "Attempting to start service discovery:" +
                           mBluetoothGatt.discoverServices());
                
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = BLEAction.ACTION_GATT_DISCONNECTED;
                
                mBluetoothGatt.close();
                
                mBluetoothGatt = null;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction, null);
            }
        }
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //   tSleep();
                broadcastUpdate(BLEAction.ACTION_GATT_SERVICES_DISCOVERED, null);
            }
            else
            {
                Log.i(TAG, "onServicesDiscovered: Services failed to be discovered");
            }
        }
        
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(BLEAction.ACTION_DATA_AVAILABLE, characteristic);
            else
                Log.w(TAG, "onCharacteristicRead received: " + status);
            
        }
        
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            
            broadcastUpdate(BLEAction.ACTION_DATA_AVAILABLE, characteristic);
        }
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(BLEAction.ACTION_DATA_WROTE, characteristic);
            else
                Log.w(TAG, "onCharacteristicWrite received: " + status);
            
            
        }
    };
    
    
    /*
	Sleep hack when connection has happened and it is still reading the gatt services, but it notifies early that it is finished with this process.
	Wait a very long time, because the amount of services and chars the server has and the distance between this device & peripheral can impact the read time.
	 */
    private void tSleep() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void broadcastUpdate(final BLEAction action,
                                 final BluetoothGattCharacteristic characteristic) {
        BLEEvent event = new BLEEvent(action, characteristic);
        EventBus.getDefault().post(event);
    }
    
    public BluetoothGattCharacteristic getSupportedGattAttribute(String characteristic) {
        List<BluetoothGattService> supportedGattServices = getSupportedGattServices();
        for (BluetoothGattService blueToothGattService : supportedGattServices) {
            Log.i(TAG, "BluetoothGatt Service: " + blueToothGattService.getUuid().toString());
            for (BluetoothGattCharacteristic blueToothGattCharacteristic : blueToothGattService.getCharacteristics()) {
                Log.i(TAG, "BluetoothGatt Chara: " + blueToothGattCharacteristic.getUuid().toString());
                if (blueToothGattCharacteristic.getUuid().toString().equalsIgnoreCase(characteristic)) {
                    return blueToothGattCharacteristic;
                }
                
            }
        }
        return null;
    }
    
    
    public class LocalBinder extends Binder
    {
        public BLEProvisionService getService() {
            return BLEProvisionService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }
    
    
    
    
    private final IBinder mBinder = new LocalBinder();
    
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        
        //		// Previously connected device.  Try to reconnect.
        //		if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
        //		    && mBluetoothGatt != null) {
        //			Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
        //			if (mBluetoothGatt.connect()) {
        //				//mConnectionState = STATE_CONNECTING;
        //				return true;
        //			} else {
        //				return false;
        //			}
        //		}
        
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        //mBluetoothDeviceAddress = address;
        //mConnectionState = STATE_CONNECTING;
        return true;
    }
    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothGatt != null)
        {
            mBluetoothGatt.disconnect();
        }
    }
    
    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        disconnect();
    }
    
    
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        
    }
    
    public void readCharacteristic(String characteristic) {
        
        readCharacteristic(getSupportedGattAttribute(characteristic));
    }
    
    
    public boolean isCharacteristicReady(String characteristic)
    {
        return getSupportedGattAttribute(characteristic) != null;
    }
    
    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}. The write result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to write to from.
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] dataToWrite) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        characteristic.setValue(dataToWrite);
        boolean initiatedSuccessfully = mBluetoothGatt.writeCharacteristic(characteristic);
        Log.i(TAG, "Write to: " + characteristic.getUuid().toString() + " - initially successful? " + initiatedSuccessfully);
        //tSleep();
        return initiatedSuccessfully;
    }
    
    public boolean writeCharacteristic(String characteristic, byte[] dataToWrite) {
        BluetoothGattCharacteristic supportedGattAttribute = getSupportedGattAttribute(characteristic);
        return writeCharacteristic(supportedGattAttribute, dataToWrite);
        
    }
    
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }
    
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        
        return mBluetoothGatt.getServices();
    }
    
    public static class BLEEvent {
        private BLEAction                   mAction;
        private BluetoothGattCharacteristic mBluetoothGattCharacteristic;
        
        public BLEEvent() { /* Default constructor */ }
        
        public BLEEvent(BLEAction mAction) {
            this.mAction = mAction;
            this.mBluetoothGattCharacteristic = null;
        }
        
        public BLEEvent(BLEAction mAction, BluetoothGattCharacteristic mBluetoothGattCharacteristic) {
            this.mAction = mAction;
            this.mBluetoothGattCharacteristic = mBluetoothGattCharacteristic;
        }
        
        public BLEAction getAction() {
            return mAction;
        }
        
        public void setAction(BLEAction action) {
            this.mAction = action;
        }
        
        public BluetoothGattCharacteristic getBluetoothGattCharacteristic() {
            return mBluetoothGattCharacteristic;
        }
        
        public void setBluetoothGattCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
            this.mBluetoothGattCharacteristic = bluetoothGattCharacteristic;
        }
    }
    
}

