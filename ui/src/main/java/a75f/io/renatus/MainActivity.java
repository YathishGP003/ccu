package a75f.io.renatus;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;

import a75f.io.bo.SmartNode;
import a75f.io.renatus.BLE.BLEHomeFragment;
import a75f.io.renatus.BLE.USBHomeFragment;
import a75f.io.serial.SerialCommManager;
import a75f.io.serial.SerialCommService;
import a75f.io.util.Globals;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_USB_PERMISSION =
            "a75f.io.renatus.action.USB_PERMISSION";

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    //CCUApp.setScreenOn(true, true);
                    Log.e("USB", "Usb Device dettached" + device.getDeviceName() + device.getClass() + device.getVendorId() + device.getProductId());
                    Toast.makeText(getApplicationContext(), a75f.io.serial.R.string.cm_stopped, Toast.LENGTH_SHORT).show();

                }
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                        false);
                Log.d("USB", "ACTION_USB_PERMISSION: " + permission);
                enumurateUSBDevices();
            }

        }
    };


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    changeContent(DefaultFragment.getInstance());
                    return true;
                case R.id.navigation_ble:
                    changeContent(BLEHomeFragment.getInstance());
                    return true;
                case R.id.navigation_usb:
                    changeContent(USBHomeFragment.getInstance());
                    return true;


            }
            return false;
        }

    };

    PendingIntent mPermissionIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);


        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        changeContent(DefaultFragment.getInstance());

        UsbDevice device = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            Log.d("Serial :","USB Attach triggered activity launch");
            Intent serialIntent = new Intent(getApplicationContext(), SerialCommService.class);
            serialIntent.putExtra("USB_DEVICE", device);
            startService(serialIntent);
        }

        //TODO- Temp test code to avoid usb time out, to be moved
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("Serial","kickOffClockUpdate");
                SerialCommManager.getInstance().kickOffClockUpdate();
            }
        }, 30000);

    }


    private void changeContent(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
    }

    public synchronized void enumurateUSBDevices() {


        UsbDevice d = null;


        UsbManager usbman = (UsbManager) MainActivity.this.getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> devlist = usbman.getDeviceList();
        Iterator<UsbDevice> deviter = devlist.values().iterator();

        while (deviter.hasNext()) {
            d = deviter.next();


            if (String.format("%04X:%04X", d.getVendorId(), d.getProductId()).equals(SerialCommManager.CM_VID_PID)) {
                // we need to upload the hex file, first request permission
                Log.d("SERIAL_DEBUG", "Device under: " + d.getDeviceName());
                if (usbman.hasPermission(d)) {

                    Log.i("Serial", "Device Found");
                    Intent serialIntent = new Intent(MainActivity.this, SerialCommService.class);
                    serialIntent.putExtra("USB_DEVICE", d);
                    MainActivity.this.startService(serialIntent);
                }
                else
                {
                    usbman.requestPermission(d, mPermissionIntent);
                }
            }
        }
        

    }

}
