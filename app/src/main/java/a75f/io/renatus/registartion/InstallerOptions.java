package a75f.io.renatus.registartion;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.service.unconfirmed.IAmRequest;
import com.renovo.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Segmentation;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;

import org.projecthaystack.HGrid;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.device.bacnet.BACnetUpdateJob;
import a75f.io.device.bacnet.BACnetUtils;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.UtilityApplication;
import a75f.io.renatus.tuners.ExpandableTunerListAdapter;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.views.MasterControl.MasterControlView;
import a75f.io.renatus.views.TempLimit.TempLimitView;

import static a75f.io.logic.L.ccu;
import static a75f.io.renatus.FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED;
import static a75f.io.renatus.SettingsFragment.ACTION_SETTING_SCREEN;
import static a75f.io.renatus.views.MasterControl.MasterControlView.getTuner;

public class InstallerOptions extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ImageView imageGoback;
    TempLimitView imageTemp;
    Button mNext;
    Context mContext;
    Spinner mAddressBandSpinner;
    ToggleButton mToggleTempAll;
    HGrid mCCUS;
    HGrid mSite;
    String mSiteId;
    String addressBandSelected = "1000";
    Prefs prefs;
    String localSiteID;
    String CCU_ID = "";
    private boolean isFreshRegister;
    //
    float lowerHeatingTemp;
    float upperHeatingTemp;
    float lowerCoolingTemp;
    float upperCoolingTemp;
    float lowerBuildingTemp;
    float upperBuildingTemp;
    float mSetBack;
    float zoneDiff;
    float cdb, hdb;

    //BACnet Setup
    ToggleButton toggleBACnet;
    RelativeLayout rl_BACnet;
    EditText editIPAddr,editSubnet,editGateway;
    Button buttonInitialise;
    String networkConfig = "";
    boolean isEthernet = false;
    UtilityApplication utilityApplication;
    LocalDevice baCnetDevice = null;
    TextInputLayout textInputIP;
    RadioGroup radioGroup_config;
    Button buttonSendIAM;
    TextView textBacnetEnable;
    private static final String TAG = InstallerOptions.class.getSimpleName();

    MasterControlView.OnClickListener onSaveChangeListener = (lowerHeatingTemp, upperHeatingTemp, lowerCoolingTemp, upperCoolingTemp, lowerBuildingTemp, upperBuildingTemp, setBack, zoneDiff, hdb, cdb) -> {
        imageTemp.setTempControl(lowerHeatingTemp, upperHeatingTemp, lowerCoolingTemp, upperCoolingTemp, lowerBuildingTemp, upperBuildingTemp);

        this.lowerHeatingTemp = lowerHeatingTemp;
        this.upperHeatingTemp = upperHeatingTemp;
        this.lowerCoolingTemp = lowerCoolingTemp;
        this.upperCoolingTemp = upperCoolingTemp;
        this.lowerBuildingTemp = lowerBuildingTemp;
        this.upperBuildingTemp = upperBuildingTemp;
        this.mSetBack = setBack;
        this.zoneDiff = zoneDiff;
        this.cdb = cdb;
        this.hdb = hdb;
    };

    public InstallerOptions() {
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
    public static InstallerOptions newInstance(String param1, String param2) {
        InstallerOptions fragment = new InstallerOptions();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //((FreshRegistration)getActivity()).showIcons(false);
        View rootView = inflater.inflate(R.layout.fragment_installeroption, container, false);

        mContext = getContext().getApplicationContext();

        prefs = new Prefs(mContext);
        isFreshRegister = getActivity() instanceof FreshRegistration;
        CCU_ID = prefs.getString("CCU_ID");

        if (!isFreshRegister) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) rootView.getLayoutParams();
            p.setMargins(50, 0, 0, 0);
        }

        imageGoback = rootView.findViewById(R.id.imageGoback);
        mAddressBandSpinner = rootView.findViewById(R.id.spinnerAddress);
        mToggleTempAll = rootView.findViewById(R.id.toggleTempAll);
        mNext = rootView.findViewById(R.id.buttonNext);
        imageTemp = rootView.findViewById(R.id.imageTemp);

        //BACnet Setup UI Components
        toggleBACnet = rootView.findViewById(R.id.toggleBACnet);
        rl_BACnet = rootView.findViewById(R.id.rl_Bacnet);
        editIPAddr = rootView.findViewById(R.id.editIPaddr);
        editSubnet = rootView.findViewById(R.id.editSubnet);
        editGateway = rootView.findViewById(R.id.editGateway);
        buttonInitialise = rootView.findViewById(R.id.buttonInitialise);
        textInputIP = rootView.findViewById(R.id.textInputIP);
        radioGroup_config = rootView.findViewById(R.id.radioGroup_config);
        buttonSendIAM = rootView.findViewById(R.id.buttonSendIAM);
        textBacnetEnable = rootView.findViewById(R.id.textBacnetEnable);
        rl_BACnet.setVisibility(View.GONE);
        String ccuGUID = CCUHsApi.getInstance().getGUID(CCUHsApi.getInstance().getCcuId().toString());
        if(prefs.getBoolean("registered") && ccuGUID!=null){
            textBacnetEnable.setVisibility(View.VISIBLE);
            toggleBACnet.setVisibility(View.VISIBLE);
        }else {
            textBacnetEnable.setVisibility(View.GONE);
            toggleBACnet.setVisibility(View.GONE);
        }
        ArrayList<String> addressBand = new ArrayList<>();
        //addressBand.add("Select SmartNode Address Band");
        for (int addr = 1000; addr <= 9900; addr += 100) {
            addressBand.add(String.valueOf(addr));
        }

        ArrayAdapter<String> analogAdapter = new ArrayAdapter<String>(mContext, R.layout.spinner_item, addressBand);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        mAddressBandSpinner.setAdapter(analogAdapter);


        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        //if ccu exists
        if (ccu.size() > 0) {
            for (String addBand : addressBand) {
                //Short addB = L.ccu().getSmartNodeAddressBand();
                String addB = String.valueOf(L.ccu().getSmartNodeAddressBand());
                if (addBand.equals(addB)) {
                    mAddressBandSpinner.setSelection(analogAdapter.getPosition(addBand));
                    break;
                }
            }
        } else {
            ccu().setSmartNodeAddressBand((short) 1000);
        }

        mAddressBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("CCU", "AddressBandSelected : " + mAddressBandSpinner.getSelectedItem());
                if (i > 0) {
                    addressBandSelected = mAddressBandSpinner.getSelectedItem().toString();
                    L.ccu().setSmartNodeAddressBand(Short.parseShort(addressBandSelected));
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        mToggleTempAll.setChecked(prefs.getBoolean(getString(R.string.USE_SAME_TEMP_ALL_DAYS)));
        mToggleTempAll.setOnCheckedChangeListener((compoundButton, isChecked) -> prefs.setBoolean(getString(R.string.USE_SAME_TEMP_ALL_DAYS), isChecked));

      /*  imageGoback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                ((FreshRegistration)getActivity()).selectItem(3);
            }
        });*/
        if (isFreshRegister) mNext.setVisibility(View.VISIBLE);
        else mNext.setVisibility(View.GONE);
        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ccu.size() == 0) {
                    return;
                }
                String ccuId = ccu.get("id").toString();
                ccuId = ccuId.replace("@", "");
                String ccuName = ccu.get("dis").toString();
                CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(ccuId));
                HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
                SettingPoint snBand = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-smartNodeBand")
                        .addMarker("snband").addMarker("sp").setVal(addressBandSelected).build();
                CCUHsApi.getInstance().addPoint(snBand);



                SettingPoint useBacnet = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-useBacnet")
                        .addMarker("bacnet").addMarker("enabled").addMarker("sp").setVal(toggleBACnet.isChecked() ? "true":"false").build();
                CCUHsApi.getInstance().addPoint(useBacnet);
                SettingPoint bacnetConfig = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-bacnetConfig")
                        .addMarker("bacnet").addMarker("config").addMarker("sp")/*.setVal(radioGroup_config.getCheckedRadioButtonId() == R.id.rbAuto ? "0" :"1")*/.build();
                CCUHsApi.getInstance().addPoint(bacnetConfig);
                SettingPoint bacnetIp = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-bacnetIp")
                        .addMarker("bacnet").addMarker("ipconfig").addMarker("sp")/*.setProtocol(editIPAddr.getText() != null ? editIPAddr.getText().toString() : "")*/.build();
                CCUHsApi.getInstance().addPoint(bacnetIp);
                SettingPoint bacnetSubnet = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-bacnetSubnet")
                        .addMarker("bacnet").addMarker("ipsubnet").addMarker("sp").setVal(editSubnet.getText() != null ? editSubnet.getText().toString() : "").build();
                CCUHsApi.getInstance().addPoint(bacnetSubnet);
                SettingPoint bacnetGateway = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-bacnetGateway")
                        .addMarker("bacnet").addMarker("ipgateway").addMarker("sp").setVal(editGateway.getText() != null ? editGateway.getText().toString() : "").build();
                CCUHsApi.getInstance().addPoint(bacnetGateway);
                // TODO Auto-generated method stub
                goTonext();
            }
        });

        if (!isFreshRegister) {
            ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
            if (equips != null && equips.size() > 0) {
                mAddressBandSpinner.setEnabled(false);
            } else {
                mAddressBandSpinner.setEnabled(true);
            }
        }

        imageTemp.setOnClickListener(view -> {
            openMasterControllerDialog();

        });

        getTempValues();

        toggleBACnet.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LocalDevice localDevice = null;
                if(isChecked){
                    rl_BACnet.setVisibility(View.VISIBLE);
                    buttonInitialise.setEnabled(true);
                    buttonInitialise.setText("Initialise");
                    buttonInitialise.requestFocus();
                    ((RadioButton)radioGroup_config.getChildAt(0)).setChecked(true);
                    if(utilityApplication.CheckEthernet()){
                        isEthernet = true;
                        networkConfig = utilityApplication.getIPConfig();
                        String[] ethConfig = networkConfig.split(":");
                        textInputIP.setHint("Ethernet-IP Address");
                        editIPAddr.setText(ethConfig[1]);
                        editGateway.setText(ethConfig[2]);
                        editSubnet.setText(ethConfig[3]);
                    }else {
                        isEthernet = false;
                        networkConfig = utilityApplication.getWiFiConfig();
                        textInputIP.setHint("Wifi-IP Address");
                        String[] ethConfig = networkConfig.split(":");
                        editIPAddr.setText(ethConfig[1]);
                        editGateway.setText(ethConfig[2]);
                        editSubnet.setText(ethConfig[3]);
                    }
                }
                else{
                    try {
                        rl_BACnet.setVisibility(View.GONE);
                        //utilityApplication = new RenatusApp();
                        //BACnetUpdateJob baCnetUpdateJob = new BACnetUpdateJob();
                        baCnetDevice = utilityApplication.getLocalDevice();
                        utilityApplication.terminateBACnet();
                        L.ccu().setUseBACnet(false);
                        if(baCnetDevice != null){
                            baCnetDevice.terminate();
                            Log.i("Bacnet","Device Status:"+baCnetDevice.isInitialized());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        radioGroup_config.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if(checkedId == R.id.rbAuto){
                    editIPAddr.setEnabled(false);
                    editGateway.setEnabled(false);
                    editSubnet.setEnabled(false);
                }else if(checkedId == R.id.rbManual){
                    editIPAddr.setEnabled(true);
                    editGateway.setEnabled(false);
                    editSubnet.setEnabled(false);
                }
            }
        });


        buttonInitialise.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LocalDevice localDevice = null;
                int checkedId = radioGroup_config.getCheckedRadioButtonId();
                if(checkedId == R.id.rbAuto) {
                    if (isEthernet) {
                        localDevice = utilityApplication.enableBACnet(networkConfig);
                    } else {
                        localDevice = utilityApplication.enableBACnetWifi();
                    }
                    if (localDevice != null) {
                        utilityApplication = new RenatusApp();
                        utilityApplication.setLocalDevice(localDevice);
                        L.ccu().setUseBACnet(true);
                        lockConfig();
                        Log.i("Bacnet", "Auto Setup localDevice:" + localDevice.getId() + " object:" + localDevice.getDeviceObject().toString());
                    }
                }else if(checkedId == R.id.rbManual){
                    //localDevice = utilityApplication.enableBACnetManual(editIPAddr.getText().toString(),editGateway.getText().toString(), editSubnet.getText().toString(),isEthernet);
                    utilityApplication.setNetwork(editIPAddr.getText().toString(),editGateway.getText().toString(), editSubnet.getText().toString(),isEthernet);
                    localDevice = utilityApplication.enableBACnet(networkConfig);
                    if (localDevice != null) {
                        utilityApplication = new RenatusApp();
                        utilityApplication.setLocalDevice(localDevice);
                        L.ccu().setUseBACnet(true);
                        lockConfig();
                        Log.i("Bacnet", "Manual Setup localDevice:" + localDevice.getId() + " object:" + localDevice.getDeviceObject().toString());
                    }
                }
                //Todo Write to Haystak
                /*String[] netConfig = networkConfig.split(":");
                String ccuId = ccu.get("id").toString();
                ccuId = ccuId.replace("@", "");
                writeBACnetConfig(netConfig[1],netConfig[2],netConfig[3],ccuId);*/
                Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber()+" isInitialized:"+localDevice.isInitialized());
                utilityApplication.sendWhoIs(localDevice);

            }
        });

        buttonSendIAM.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                baCnetDevice = utilityApplication.getLocalDevice();
                baCnetDevice.send(baCnetDevice.getLocalBroadcastAddress(),new WhoIsRequest(null, null));
                //baCnetDevice.send(baCnetDevice.getLocalBroadcastAddress(),new IAmRequest(new ObjectIdentifier(ObjectType.device,baCnetDevice.getInstanceNumber()), baCnetDevice.get(PropertyIdentifier.maxApduLengthAccepted), baCnetDevice.get(PropertyIdentifier.segmentationSupported),baCnetDevice.get(PropertyIdentifier.vendorIdentifier)));
                baCnetDevice.sendLocalBroadcast(new IAmRequest(new ObjectIdentifier(ObjectType.device,baCnetDevice.getInstanceNumber()), baCnetDevice.get(PropertyIdentifier.maxApduLengthAccepted), Segmentation.segmentedBoth,baCnetDevice.get(PropertyIdentifier.vendorIdentifier)));
                Log.i("Bacnet", "Device:" + baCnetDevice.getId() + " Name:" + baCnetDevice.getDeviceObject().getObjectName()+" Sent IAM");
            }
        });

        getActivity().registerReceiver(mPairingReceiver, new IntentFilter(ACTION_SETTING_SCREEN));

        return rootView;
    }

    private void lockConfig(){
        Log.i("Bacnet", "Initialize Button Pressed");
        buttonInitialise.setText("BACnet Initialised");
        buttonInitialise.setEnabled(false);
        editIPAddr.setEnabled(false);
        editGateway.setEnabled(false);
        editSubnet.setEnabled(false);
        buttonSendIAM.setEnabled(true);
        buttonSendIAM.setVisibility(View.GONE);//Testing Purpose make it visible to send I AM
    }
    // initial master control values
    private void getTempValues() {

        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip p = new Equip.Builder().setHashMap(tuner).build();

        hdb = (float) TunerUtil.getHeatingDeadband(p.getId());
        cdb = (float) TunerUtil.getCoolingDeadband(p.getId());
        HashMap coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        HashMap heatUL = CCUHsApi.getInstance().read("point and limit and max and heating and user");
        HashMap coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        HashMap heatLL = CCUHsApi.getInstance().read("point and limit and min and heating and user");
        HashMap buildingMin = CCUHsApi.getInstance().read("building and limit and min");
        HashMap buildingMax = CCUHsApi.getInstance().read("building and limit and max");
        HashMap setbackMap = CCUHsApi.getInstance().read("unoccupied and setback and equipRef == \"" + p.getId() + "\"");
        HashMap zoneDiffMap = CCUHsApi.getInstance().read("building and zone and differential");

        upperCoolingTemp = (float) getTuner(coolUL.get("id").toString());
        lowerCoolingTemp = (float) getTuner(coolLL.get("id").toString());
        upperHeatingTemp = (float) getTuner(heatUL.get("id").toString());
        lowerHeatingTemp = (float) getTuner(heatLL.get("id").toString());
        lowerBuildingTemp = (float) getTuner(buildingMin.get("id").toString());
        upperBuildingTemp = (float) getTuner(buildingMax.get("id").toString());
        mSetBack = (float) getTuner(setbackMap.get("id").toString());
        zoneDiff = (float) getTuner(zoneDiffMap.get("id").toString());
    }

    //custom dialog to control building temperature
    private void openMasterControllerDialog() {
        if (isAdded()) {

            final Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dialog_master_control);
            MasterControlView masterControlView = dialog.findViewById(R.id.masterControlView);

            dialog.findViewById(R.id.btnCancel).setOnClickListener(view -> dialog.dismiss());
            dialog.findViewById(R.id.btnClose).setOnClickListener(view -> dialog.dismiss());

            dialog.findViewById(R.id.btnSet).setOnClickListener(view -> masterControlView.setTuner(dialog));

            masterControlView.setOnClickChangeListener(onSaveChangeListener);

            new CountDownTimer(100, 100) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    masterControlView.setMasterControl(lowerHeatingTemp, upperHeatingTemp, lowerCoolingTemp,
                            upperCoolingTemp, lowerBuildingTemp, upperBuildingTemp,
                            mSetBack, zoneDiff, hdb, cdb);
                }
            }.start();

            dialog.show();
        }
    }


   /* @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            prefs = new Prefs(getContext().getApplicationContext());
            localSiteID = prefs.getString("SITE_ID");
            loadSiteDetails();
        }
    }*/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    /* This site never existed we are creating a new orphaned site. */

    /*  private void goTonext() {
          //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
          //startActivity(i);
          ((FreshRegistration)getActivity()).selectItem(5);
      }
  */
    private void goTonext() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //showProgressDialog();
            }

            @Override
            protected Void doInBackground(Void... voids) {

                if (!Globals.getInstance().siteAlreadyCreated()) {
                    BuildingTuners.getInstance();
                    DefaultSchedules.setDefaultCoolingHeatingTemp();
                    DefaultSchedules.generateDefaultSchedule(false, null);
                }

                L.saveCCUState();
                CCUHsApi.getInstance().log();
                CCUHsApi.getInstance().syncEntityTree();


                return null;
            }

            @Override
            protected void onPostExecute(Void nul) {
                super.onPostExecute(nul);
                //hideProgressDialog();
                prefs.setBoolean("PROFILE_SETUP", false);
                prefs.setBoolean("CCU_SETUP", true);
                ((FreshRegistration) getActivity()).selectItem(5);

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }


    public void writeBACnetConfig(String ipAddress,  String broadCast, String subNet, String ccuID) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                HashMap ipaddHS = CCUHsApi.getInstance().read("bacnet and ipconfig and sp and equipRef == \"" + ccuID + "\"");
                String ipddID = ipaddHS.get("id").toString();
                CCUHsApi hayStack = CCUHsApi.getInstance();
                CcuLog.d(L.TAG_CCU_UI, "Set His Val "+ipddID+": " +ipAddress);
                hayStack.writeDefaultValById(ipddID, ipAddress);

            }
        });
        thread.start();
    }

    @Override
    public void onDestroyView() {
        if (getActivity() != null){
            getActivity().unregisterReceiver(mPairingReceiver);
        }
        super.onDestroyView();
    }

    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null){
                return;
            }

            if (intent.getAction().equals(ACTION_SETTING_SCREEN)) {
                if (mAddressBandSpinner != null) {
                    ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
                    if (equips != null && equips.size() > 0) {
                        mAddressBandSpinner.setEnabled(false);
                    } else {
                        mAddressBandSpinner.setEnabled(true);
                    }
                }
            }
        }
    };
}
