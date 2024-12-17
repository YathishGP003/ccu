package a75f.io.renatus.ENGG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.List;

import a75f.io.device.mesh.RootCommandExecuter;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.UtilityApplication;
import butterknife.BindView;
import butterknife.ButterKnife;

public class NetworkConfigFragment extends Fragment {

    @BindView(R.id.networkType)
    Spinner networkType;

    @BindView(R.id.ipAddress)
    EditText ipAddress;

    @BindView(R.id.subnetMask)
    EditText subnetMask;

    @BindView(R.id.broadcastAddress)
    EditText broadcastAddress;

    @BindView(R.id.dnsAddress)
    EditText dnsAddress;

    @BindView(R.id.saveButton)
    Button saveButton;

    @BindView(R.id.eraseButton)
    Button eraseButton;

    List<String> networkTypes = Arrays.asList("eth0", "wlan0");
    public static NetworkConfigFragment newInstance(){
        return new NetworkConfigFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.network_config_fragment, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayAdapter<String> networkTypeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, networkTypes);
        networkTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        networkType.setAdapter(networkTypeAdapter);
        SharedPreferences sharedPreferences = Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE);
        String networkTypePref = sharedPreferences.getString("networkType", "eth0");
        String ipAddressPref = sharedPreferences.getString("ipAddress", "");
        String subnetMaskPref = sharedPreferences.getString("subnetMask", "");
        String broadcastAddressPref = sharedPreferences.getString("broadcastAddress", "");
        String dnsAddressPref = sharedPreferences.getString("dnsAddress", "");

        CcuLog.d("NetworkConfigFragment", "Current Config Network Type: " + networkTypePref + " IP Address: "
                + ipAddressPref + " Subnet Mask: " + subnetMaskPref + " BroadcastAddress: " + broadcastAddressPref
                + " DNS Address: " + dnsAddressPref);

        networkType.setSelection((networkTypePref.equals("eth0") || networkTypePref.isEmpty())? 0 : 1);
        ipAddress.setText(ipAddressPref);
        subnetMask.setText(subnetMaskPref);
        broadcastAddress.setText(broadcastAddressPref);
        dnsAddress.setText(dnsAddressPref);

        saveButton.setOnClickListener(v -> {
            String networkTypeVal = (String) this.networkType.getSelectedItem();
            String ipAddress = this.ipAddress.getText().toString();
            String subnetMask = this.subnetMask.getText().toString();
            String broadcastAddress = this.broadcastAddress.getText().toString();
            String dnsAddress = this.dnsAddress.getText().toString();

            CcuLog.d("NetworkConfigFragment", "Network Type: " + networkTypeVal + " IP Address: "
                            + ipAddress + " Subnet Mask: " + subnetMask + " BroadcastAddress: " + broadcastAddress
                            + " DNS Address: " + dnsAddress);

            sharedPreferences.edit()
                    .putString("networkType", networkTypeVal)
                    .putString("ipAddress",ipAddress)
                    .putString("subnetMask",subnetMask)
                    .putString("broadcastAddress",broadcastAddress)
                    .putString("dnsAddress",dnsAddress)
                    .apply();

            UtilityApplication.setNetwork(ipAddress, broadcastAddress, subnetMask, networkTypeVal, dnsAddress);
        });

        eraseButton.setOnClickListener(v -> {

            new AlertDialog.Builder(getContext())
                    .setTitle("Erase ")
                    .setMessage("This requires rebooting the tablet , Proceed ?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        sharedPreferences.edit().putString("networkType", "")
                                .putString("ipAddress", "")
                                .putString("subnetMask", "")
                                .putString("broadcastAddress", "")
                                .putString("dnsAddress", "")
                                .apply();

                        RenatusApp.rebootTablet();
                     })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(R.drawable.ic_dialog_alert)
                    .show();

        });

    }
}
