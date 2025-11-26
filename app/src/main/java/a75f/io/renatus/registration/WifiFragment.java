package a75f.io.renatus.registration;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.renatus.R;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.views.CustomCCUSwitch;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class WifiFragment extends Fragment  implements WifiListAdapter.ItemClickListener {
    private String TAG = "Wifi Fragment";

    ImageView imageGoback;
    RecyclerView recyclerWifi;
    WifiManager mainWifiObj;
    HashMap<String, ScanResult> distinctNetworks;
    List<ScanResult> results;
    List<String> wifinetworks;
    WifiListAdapter wifiListAdapter;
    ImageView imageRefresh;
    CustomCCUSwitch toggleWifi;
    ProgressBar progressbar;
    Context mContext;
    Handler mHandler;
    Runnable mRunable;
    TextView mTurnonwifi;
    RelativeLayout layoutConnectWifi;
    private SwitchFragment switchFragment;
    Prefs prefs;
    String INSTALL_TYPE = "";
    private boolean isFreshRegister;
    private BroadcastReceiver mNetworkReceiver;
    private BroadcastReceiver mWifiReceiver;

    private AlertDialog alertDialog;

    private final CompositeDisposable disposable = new CompositeDisposable();
    public WifiFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNetworkReceiver = new NetworkChangeReceiver();
        mWifiReceiver = new WifiStateReceiver();
        getActivity().registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_wifi, container, false);
        isFreshRegister = getActivity() instanceof FreshRegistration;
        imageGoback = rootView.findViewById(R.id.imageGoback);
        layoutConnectWifi =  rootView.findViewById(R.id.layoutConnectWifi);
        imageRefresh = rootView.findViewById(R.id.imageRefresh);
        toggleWifi =  rootView.findViewById(R.id.toggleWifi);
        recyclerWifi = rootView.findViewById(R.id.recyclerWifi);
        progressbar = rootView.findViewById(R.id.progressbar);
        mTurnonwifi = rootView.findViewById(R.id.textView_turnon);
        rootView.findViewById(R.id.textConnectWifi).setEnabled(true);
        rootView.findViewById(R.id.textConnectWifi).setClickable(true);
        if (isFreshRegister) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) progressbar.getLayoutParams();
            params.setMargins(20, 0, 50, 50);
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.startToStart = ConstraintLayout.LayoutParams.END;
            progressbar.setLayoutParams(params);

        }
        rootView.findViewById(R.id.textConnectWifi).setOnClickListener(v -> {
            CcuLog.i(TAG, "click connect wifi");
            switchFragment.Switch(3);
        });
        progressbar.setVisibility(View.VISIBLE);
        imageRefresh.setOnLongClickListener(v -> {
            System.out.println("Image Refresh 2");
            switchFragment.Switch(3);
            return false;
        });

        mContext = getContext().getApplicationContext();
        wifinetworks = new ArrayList<>();
        if (!isFreshRegister) layoutConnectWifi.setVisibility(View.VISIBLE);
        else layoutConnectWifi.setVisibility(View.GONE);

        prefs = new Prefs(mContext);
        INSTALL_TYPE = prefs.getString("INSTALL_TYPE");
        mHandler = new Handler();
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity(),LinearLayoutManager.VERTICAL, false);
        recyclerWifi.setLayoutManager(mLayoutManager);
        recyclerWifi.setHasFixedSize(true);
        recyclerWifi.setItemAnimator(new DefaultItemAnimator());

        wifiListAdapter = new WifiListAdapter(getActivity(),this);
        wifiListAdapter.updateData(wifinetworks);
        recyclerWifi.setAdapter(wifiListAdapter);

        Animation animation = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(1200);

        imageGoback.setOnClickListener(v -> {
            mHandler.removeCallbacks(mRunable);
            switchFragment.Switch(1);
        });

        imageRefresh.setOnClickListener(v -> {
            if (mainWifiObj.isWifiEnabled()) {
                animation.setRepeatCount(0);
                animation.setDuration(1200);
                imageRefresh.startAnimation(animation);
                mainWifiObj.startScan();
                recyclerWifi.setAdapter(null);
                showScanResult();
            }
        });

        mainWifiObj = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mainWifiObj.isWifiEnabled()) {
            progressbar.setVisibility(View.GONE);
        }

        toggleWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (getUserVisibleHint()) {
                mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                if (isChecked) {
                    if (alertDialog != null && alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                    progressbar.setVisibility(View.VISIBLE);
                    if (!mainWifiObj.isWifiEnabled() || isFreshRegister) {
                        mTurnonwifi.setVisibility(View.GONE);
                        imageRefresh.setEnabled(true);
                        imageRefresh.setImageResource(R.drawable.ic_refresh);
                        animation.setDuration(2000);
                        imageRefresh.startAnimation(animation);
                        mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                        mainWifiObj.setWifiEnabled(true);
                    }
                } else {
                    boolean tunerSyncStatus = Globals.getInstance().getApplicationContext().getSharedPreferences(Globals.getInstance().getDefaultSharedPreferencesName(), Context.MODE_PRIVATE).getBoolean("Tuner_Sync_Status", false);

                    if (!tunerSyncStatus) {
                        AlertDialog.Builder AlertBuilder = new AlertDialog.Builder(getActivity());
                        AlertBuilder.setTitle(requireContext().getString(R.string.sync_failed));
                        AlertBuilder.setIcon(R.drawable.ic_alert);
                        AlertBuilder.setPositiveButton(requireContext().getString(R.string.retry), (dialog, which) -> dialog.dismiss());
                        AlertBuilder.setMessage(requireContext().getString(R.string.failed_to_sync_data_to_cloud));
                        alertDialog = AlertBuilder.create();
                        alertDialog.show();
                    }

                    mHandler.removeCallbacks(mRunable);
                    if (mainWifiObj.isWifiEnabled()) {

                        mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                        mainWifiObj.setWifiEnabled(false);
                    }
                    mTurnonwifi.setVisibility(View.GONE);
                    imageRefresh.setEnabled(true);
                    imageRefresh.setImageResource(R.drawable.ic_refresh);
                }
                if (!mainWifiObj.isWifiEnabled()) {
                    mTurnonwifi.setVisibility(View.GONE);
                    recyclerWifi.setAdapter(null);
                    imageRefresh.setEnabled(false);
                    imageRefresh.setImageResource(R.drawable.ic_refresh_disable);
                }
            }
        });
        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfoForEthernet = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

        if (isFreshRegister) {
            ((FreshRegistration) getActivity()).setToggleWifi(mainWifiObj.isWifiEnabled(), networkInfoForEthernet.isConnected());
        }
        if (mainWifiObj.isWifiEnabled()) {
            mainWifiObj.startScan();
            progressbar.setVisibility(View.GONE);
            mTurnonwifi.setVisibility(View.GONE);
        } else {
            progressbar.setVisibility(View.GONE);
            mTurnonwifi.setVisibility(View.VISIBLE);
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filterWifi = new IntentFilter();
        filterWifi.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filterWifi.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filterWifi.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);
        requireActivity().registerReceiver(mWifiReceiver, filterWifi);
    }

    public void showScanResult() {
        try {
            ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfoForEthernet = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
            if (isFreshRegister) {
                ((FreshRegistration) getActivity()).setToggleWifi(mainWifiObj.isWifiEnabled(), networkInfoForEthernet.isConnected());
            }
            distinctNetworks = new HashMap<>();
            for (int i = 0; i < results.size(); i++) {
                if (!distinctNetworks.containsKey(results.get(i))) {
                    distinctNetworks.put(results.get(i).SSID, results.get(i));
                } else {
                    if (WifiManager.compareSignalLevel(results.get(i).level, distinctNetworks.get(results.get(i).SSID).level) > 0) {
                        distinctNetworks.put(results.get(i).SSID, results.get(i));
                    }
                }
            }
            Set<String> wifiset = distinctNetworks.keySet();
            CcuLog.i(TAG, "NW:" + distinctNetworks.toString() + " keyset:" + wifiset);
            wifinetworks.clear();
            wifinetworks = new ArrayList<>(wifiset);
            NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo.isConnected()) {
                final WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                for (int j = 0; j < wifinetworks.size(); j++) {
                    String ssid_scanned = wifinetworks.get(j);
                    ssid_scanned = String.format("\"%s\"", ssid_scanned);
                    CcuLog.i(TAG, "ssid connected:" + connectionInfo.getSSID() + " scanned:" + ssid_scanned);
                    if (j != 0) {
                        if (connectionInfo.getSSID().equals(ssid_scanned)) {
                            String temp = wifinetworks.get(0);
                            wifinetworks.set(0, wifinetworks.get(j));
                            wifinetworks.set(j, temp);
                        }
                    }
                }
                if (progressbar.isShown()) {
                    progressbar.setVisibility(View.GONE);
                }
            } else {
                if (!mainWifiObj.isWifiEnabled()) {
                    progressbar.setVisibility(View.GONE);
                }
            }

            wifiListAdapter.updateData(wifinetworks);
            recyclerWifi.setAdapter(wifiListAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Context context) {
        try {
            switchFragment = (SwitchFragment) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.dispose();
        if (getActivity() != null && mNetworkReceiver != null) {
            getActivity().unregisterReceiver(mNetworkReceiver);
        }
        if (getActivity() != null && mWifiReceiver != null) {
            getActivity().unregisterReceiver(mWifiReceiver);
        }
    }

    @Override
    public void onItemClicked(View view, int position) {
        String ssid = wifinetworks.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View alertview = LayoutInflater.from(getActivity()).inflate(R.layout.alert_wifipassword, null);
        builder.setView(alertview);
        TextView tvisConnected = view.findViewById(R.id.textisConnected);
        final EditText editText_Password = alertview.findViewById(R.id.editPassword);
        builder.setPositiveButton("Connect", (dialog, which) -> {
            if (TextUtils.isEmpty(editText_Password.getText().toString())) {
                Toast.makeText(getActivity(), "Please enter password", Toast.LENGTH_SHORT).show();
                return;
            }
            ProgressDialogUtils.showProgressDialog(getActivity(), "Connecting...");
            disposable.add(RxjavaUtil.executeBackgroundWithDisposable(() -> connectWifi(ssid, editText_Password.getText().toString())));
            new Handler().postDelayed(() -> {
                if (getActivity() != null && isAdded() && !isOnline(getActivity())) {
                    Toast.makeText(getActivity(), "Incorrect password", Toast.LENGTH_SHORT).show();
                    ProgressDialogUtils.hideProgressDialog();
                }
            }, 30000);

            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        alertDialog = builder.create();
        if (!tvisConnected.getText().toString().equals("CONNECTED")) {
            alertDialog.show();
        }
    }

    private void connectWifi(String ssid, String password) {

        WifiManager mainWifiObj = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", password);

        // remember id
        int netId = mainWifiObj.addNetwork(wifiConfig);
        mainWifiObj.disconnect();
        mainWifiObj.enableNetwork(netId, true);
        mainWifiObj.saveConfiguration();
        mainWifiObj.reconnect();

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"\"" + ssid + "\"\"";
        conf.preSharedKey = "\"" + password + "\"";
        conf.status = WifiConfiguration.Status.ENABLED;
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

        mainWifiObj.addNetwork(conf);
        mainWifiObj.startScan();
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isOnline(context)) {
                showScanResult();
                ProgressDialogUtils.hideProgressDialog();
            }
        }
    }

    public class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getUserVisibleHint()) {
                final String action = intent.getAction();
                results = mainWifiObj.getScanResults();
                CcuLog.i(TAG, "Scan Result Action:" + action + " Result:" + results.toString());
                if (action.equals("android.net.wifi.STATE_CHANGE") || action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    if (mainWifiObj.isWifiEnabled()) {
                        mTurnonwifi.setVisibility(View.GONE);
                        toggleWifi.setChecked(true);
                        imageRefresh.setEnabled(true);
                        imageRefresh.setImageResource(R.drawable.ic_refresh);
                        recyclerWifi.setVisibility(View.VISIBLE);
                    } else {
                        toggleWifi.setChecked(false);
                        mTurnonwifi.setVisibility(View.VISIBLE);
                        progressbar.setVisibility(View.GONE);
                        imageRefresh.setEnabled(false);
                        imageRefresh.setImageResource(R.drawable.ic_refresh_disable);
                        recyclerWifi.setVisibility(View.GONE);
                    }
                    showScanResult();
                }
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        CcuLog.d("Security", "onStop: Security Fragment");
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    private boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return (netInfo != null && netInfo.isConnected());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }
}

