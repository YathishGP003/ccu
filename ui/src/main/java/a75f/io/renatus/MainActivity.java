package a75f.io.renatus;

import android.content.Context;
import android.content.Intent;
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

import a75f.io.renatus.BLE.BLEHomeFragment;
import a75f.io.serial.SerialCommManager;
import a75f.io.serial.SerialCommService;
import a75f.io.util.Globals;

public class MainActivity extends AppCompatActivity {

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
                case R.id.navigation_notifications:

                    return true;


            }
            return false;
        }

    };
    private Toast enumerateUSBDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        changeContent(DefaultFragment.getInstance());
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                enumurateUSBDevices();
            }
        }, 3000);

    }


    private void changeContent(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
    }

    public synchronized void enumurateUSBDevices() {
        //enumerateUSBDevices.show();

        UsbDevice d = null;
        boolean bDeviceFound = false;

        UsbManager usbman = (UsbManager) MainActivity.this.getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> devlist = usbman.getDeviceList();
        Iterator<UsbDevice> deviter = devlist.values().iterator();

        while (deviter.hasNext()) {
            d = deviter.next();


            if (String.format("%04X:%04X", d.getVendorId(), d.getProductId()).equals(SerialCommManager.CM_VID_PID)) {
                // we need to upload the hex file, first request permission
                Log.d("SERIAL_DEBUG", "Device under: " + d.getDeviceName());
                if (usbman.openDevice(d) != null) {
                    bDeviceFound = true;
                    break;
                }
            }
        }

        if (bDeviceFound) {
            Log.i("Serial", "Device Found");
            Intent serialIntent = new Intent(Globals.getInstance().getApplicationContext(), SerialCommService.class);
            serialIntent.putExtra("USB_DEVICE", d);
            Globals.getInstance().getApplicationContext().startService(serialIntent);
        }

    }

}
