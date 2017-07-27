package a75f.io.renatus.BLE;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.renatus.MainActivity;
import a75f.io.renatus.R;
import a75f.io.serial.SerialCommManager;
import a75f.io.util.prefs.EncryptionPrefs;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ryanmattison on 7/27/17.
 */

public class USBHomeFragment extends Fragment {


    private static final String TAG = BLEHomeFragment.class.getSimpleName();

    public static USBHomeFragment getInstance() {
        return new USBHomeFragment();
    }

    @BindView(R.id.fragment_usb_button)
    Button mUSBButton;

    @BindView(R.id.fragment_light_button)
    Button mLightButton;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View retVal = inflater.inflate(R.layout.fragment_usb, container, false);
        ButterKnife.bind(this, retVal);
        return retVal;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @OnClick(R.id.fragment_usb_button)
    public void usbSubmit() {
        Log.i(TAG, "Done");
        ((MainActivity)getActivity()).enumurateUSBDevices();

    }


    @OnClick(R.id.fragment_light_button)
    public void usbLight()
    {

        CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage =  new CcuToCmOverUsbDatabaseSeedSnMessage_t();
        seedMessage.encryptionKey.set(0);

        seedMessage.smartNodeAddress.set(8000);
        seedMessage.controls.time.day.set((short) 1);
        seedMessage.controls.time.hours.set((short) 1);
        seedMessage.controls.time.minutes.set((short) 1);

        seedMessage.settings.ledBitmap.smartNodeLedBitmap_t_extras.digitalOut1.set(1);



        SerialCommManager.getInstance().sendData(seedMessage);
    }


}
