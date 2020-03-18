package a75f.io.renatus.registartion;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import a75f.io.renatus.R;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;

public class WifiFragment extends Fragment /*implements InstallType */  implements WifiListAdapter.ItemClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int WAIT_TIME = 2000;
    private String TAG = "Wifi Fragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    ImageView imageGoback;
    RecyclerView recyclerWifi;
    WifiManager mainWifiObj;
    HashMap<String, ScanResult> distinctNetworks;
    List<ScanResult> results;
    List<String> wifinetworks;
    WifiListAdapter wifiListAdapter;
    ImageView imageRefresh;
    ToggleButton toggleWifi;
    ProgressBar progressbar;
    Context mContext;
    Handler mHandler;
    Runnable mRunable;
    TextView mTurnonwifi;
    RelativeLayout layoutConnectWifi;
    private SwitchFragment switchFragment;
    int goToNext = 0;
    Prefs prefs;
    String INSTALL_TYPE = "";
    private boolean isFreshRegister;
    private BroadcastReceiver mNetworkReceiver;

    public WifiFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StartCCUFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WifiFragment newInstance(String param1, String param2) {
        WifiFragment fragment = new WifiFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mNetworkReceiver = new NetworkChangeReceiver();
        getActivity().registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_wifi, container, false);
        isFreshRegister = getActivity() instanceof FreshRegistration;
        imageGoback = (ImageView) rootView.findViewById(R.id.imageGoback);
        layoutConnectWifi =  rootView.findViewById(R.id.layoutConnectWifi);
        imageRefresh = (ImageView) rootView.findViewById(R.id.imageRefresh);
        toggleWifi = (ToggleButton) rootView.findViewById(R.id.toggleWifi);
        recyclerWifi = (RecyclerView) rootView.findViewById(R.id.recyclerWifi);
        progressbar = (ProgressBar) rootView.findViewById(R.id.progressbar);
        mTurnonwifi = (TextView) rootView.findViewById(R.id.textView_turnon);
        rootView.findViewById(R.id.textConnectWifi).setEnabled(true);
        rootView.findViewById(R.id.textConnectWifi).setClickable(true);

        rootView.findViewById(R.id.textConnectWifi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "click connect wifi");
                switchFragment.Switch(3);
            }
        });
        progressbar.setVisibility(View.GONE);
        mTurnonwifi.setVisibility(View.GONE);

        imageRefresh.setOnLongClickListener(v ->
        {
            System.out.println("Image Refresh 2");
            switchFragment.Switch(3);
            return false;
        });

        mContext = getContext().getApplicationContext();
        wifinetworks = new ArrayList<>();
        if (!isFreshRegister) layoutConnectWifi.setVisibility(View.VISIBLE); else layoutConnectWifi.setVisibility(View.GONE);

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
        //imageRefresh.setAnimation(animation);

        imageGoback.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                mHandler.removeCallbacks(mRunable);
                // TODO Auto-generated method stub
                //((FreshRegistration)getActivity()).selectItem(1);
                switchFragment.Switch(1);
            }
        });

        imageRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mainWifiObj.isWifiEnabled()) {
                    animation.setRepeatCount(0);
                    animation.setDuration(1200);
                    imageRefresh.startAnimation(animation);
                    mainWifiObj.startScan();
                    recyclerWifi.setAdapter(null);
                    showScanResult();
                }
            }
        });

        mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        IntentFilter filterWifi = new IntentFilter();
        filterWifi.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filterWifi.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filterWifi.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filterWifi.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);

        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (getUserVisibleHint()) {
                    final String action = intent.getAction();
                    results = mainWifiObj.getScanResults();
                    Log.i(TAG, "Scan Result Action:" + action + " Result:" + results.toString());
                    if (action.equals("android.net.wifi.STATE_CHANGE") || action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                        //toggleWifi.setChecked(mainWifiObj.isWifiEnabled());
                        if (mainWifiObj.isWifiEnabled()) {
                            mTurnonwifi.setVisibility(View.GONE);
                            toggleWifi.setChecked(true);
                            imageRefresh.setEnabled(true);
                            imageRefresh.setImageResource(R.drawable.ic_refresh);
                            recyclerWifi.setVisibility(View.VISIBLE);
                        } else {
                            toggleWifi.setChecked(false);
                            mTurnonwifi.setVisibility(View.VISIBLE);
                            imageRefresh.setEnabled(false);
                            imageRefresh.setImageResource(R.drawable.ic_refresh_disable);
                            recyclerWifi.setVisibility(View.GONE);
                        }

                        showScanResult();
                    }
                }
            }
        }, filterWifi);

        toggleWifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (getUserVisibleHint()) {

                    mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                    if (isChecked) {
                        progressbar.setVisibility(View.VISIBLE);
                        if (!mainWifiObj.isWifiEnabled()) {
                            imageRefresh.setEnabled(true);
                            imageRefresh.setImageResource(R.drawable.ic_refresh);
                            animation.setDuration(2000);
                            imageRefresh.startAnimation(animation);
                            mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                            mainWifiObj.setWifiEnabled(true);
                        }
                    } else {
                        mHandler.removeCallbacks(mRunable);
                        if (mainWifiObj.isWifiEnabled()) {
                            mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                            mainWifiObj.setWifiEnabled(false);
                        }
                        progressbar.setVisibility(View.GONE);
                        mTurnonwifi.setVisibility(View.GONE);
                        imageRefresh.setEnabled(true);
                        imageRefresh.setImageResource(R.drawable.ic_refresh);
                    }
                    if (!mainWifiObj.isWifiEnabled()) {
                        mTurnonwifi.setVisibility(View.VISIBLE);
                        recyclerWifi.setAdapter(null);
                        imageRefresh.setEnabled(false);
                        imageRefresh.setImageResource(R.drawable.ic_refresh_disable);
                    }
                }
            }
        });


        //toggleWifi.setChecked(mainWifiObj.isWifiEnabled());
        if (isFreshRegister) {
            ((FreshRegistration) getActivity()).setToggleWifi(mainWifiObj.isWifiEnabled());
        }
        if (mainWifiObj.isWifiEnabled()) {
            mainWifiObj.startScan();
        } else {
            mTurnonwifi.setVisibility(View.VISIBLE);
        }
        return rootView;
    }

   /* @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            //toggleWifi.setChecked(mainWifiObj.isWifiEnabled());
            ((FreshRegistration)getActivity()).setToggleWifi(mainWifiObj.isWifiEnabled());
            if(mainWifiObj.isWifiEnabled())
            {
                mainWifiObj.startScan();
            }
        }
    }*/

    public void showScanResult() {

        try {
            if (isFreshRegister) {
                ((FreshRegistration) getActivity()).setToggleWifi(mainWifiObj.isWifiEnabled());
            }
            distinctNetworks = new HashMap<String, ScanResult>();
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
            Log.i(TAG, "NW:" + distinctNetworks.toString() + " keyset:" + wifiset);
            wifinetworks.clear();
            wifinetworks = new ArrayList<>(wifiset);
            ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo.isConnected()) {
                final WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                for (int j = 0; j < wifinetworks.size(); j++) {
                    String ssid_scanned = wifinetworks.get(j);
                    ssid_scanned = String.format("\"%s\"", ssid_scanned);
                    Log.i(TAG, "ssid connected:" + connectionInfo.getSSID() + " scanned:" + ssid_scanned);
                    if (j != 0) {
                        if (connectionInfo.getSSID().equals(ssid_scanned)) {
                            String temp = wifinetworks.get(0);
                            wifinetworks.set(0, wifinetworks.get(j));
                            wifinetworks.set(j, temp);
                        }
                    }
                }

                /*mRunable = new Runnable() {
                    @Override
                    public void run() {
                        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                        if (networkInfo.isConnected()) {
                            //Toast.makeText(getActivity(), "Wifi Connected", Toast.LENGTH_SHORT).show();
                            //((FreshRegistration)getActivity()).selectItem(3);
                            //((FreshRegistration)getActivity().getApplicationContext()).selectItem(goToNext);
                            Log.i(TAG, "goto:" + goToNext);
                            if (INSTALL_TYPE.equals("CREATENEW")) {
                                switchFragment.Switch(3);
                            }
                            if (INSTALL_TYPE.equals("ADDCCU")) {
                                switchFragment.Switch(6);
                            }
                            if (INSTALL_TYPE.equals("PRECONFIGCCU")) {
                                switchFragment.Switch(7);
                            }
                            if (INSTALL_TYPE.equals("REPLACECCU")) {
                                switchFragment.Switch(8);
                            }
                        }
                    }
                };
                mHandler.postDelayed(mRunable, WAIT_TIME);*/
            }

            wifiListAdapter.updateData(wifinetworks);
            recyclerWifi.setAdapter(wifiListAdapter);
            if (progressbar.isShown()) {
                progressbar.setVisibility(View.GONE);
            }
        } catch (Exception e) {

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
        if (mNetworkReceiver != null){
            getActivity().unregisterReceiver(mNetworkReceiver);
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
          ProgressDialogUtils.showProgressDialog(getActivity(),"Connecting...");
          connectWifi(ssid, editText_Password.getText().toString());

          dialog.dismiss();
        });


        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());


        AlertDialog alertDialog = builder.create();

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

        new Handler().postDelayed(() -> {
            if(getActivity()!= null && isAdded() && !isOnline(getActivity())){
                Toast.makeText(getActivity(), "Incorrect password", Toast.LENGTH_SHORT).show();
                ProgressDialogUtils.hideProgressDialog();
            }
        }, 30000);
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


    private boolean isOnline(Context context) {

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();

            //should check null because in airplane mode it will be null
            return (netInfo != null && netInfo.isConnected());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }
}

