package a75f.io.renatus.BLE;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import a75f.io.renatus.R;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class BLEHomeFragment extends Fragment {

    private static final String TAG = BLEHomeFragment.class.getSimpleName();

    public static BLEHomeFragment getInstance() {
        return new BLEHomeFragment();
    }

    @BindView(R.id.fragment_ble_button)
    Button mainTextView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View retVal = inflater.inflate(R.layout.fragment_ble, container, false);
        ButterKnife.bind(this, retVal);
        return retVal;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainTextView.setText("BLE");
    }

    @OnClick(R.id.fragment_ble_button)
    void bleSubmit() {
        Log.i(TAG, "Done");
        FragmentDeviceScan.getInstance().show(getChildFragmentManager(), "dialog");
        Toast.makeText(this.getActivity(), "BLE Fragment Done", Toast.LENGTH_LONG).show();
    }


}
