package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.daikin.IEDeviceHandler;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.util.SystemProfileUtil;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by samjithsadasivan on 11/6/18.
 */

public class VavIERtuProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{
    
    @BindView(R.id.analog1RTU)CheckBox analog1Cb;
    @BindView(R.id.analog2RTU)CheckBox analog2Cb;
    @BindView(R.id.analog3RTU)CheckBox analog3Cb;
    @BindView(R.id.humidificationCb)CheckBox humidificationCb;
    
    @BindView(R.id.coolingDatMin)Spinner coolingDatMin;
    @BindView(R.id.coolingDatMax)Spinner coolingDatMax;
    @BindView(R.id.heatingDatMin)Spinner heatingDatMin;
    @BindView(R.id.heatingDatMax)Spinner heatingDatMax;
    @BindView(R.id.spMin)Spinner spMin;
    @BindView(R.id.spMax)Spinner spMax;
    @BindView(R.id.spMinLabel)TextView spMinLabel;
    @BindView(R.id.spMaxLabel)TextView spMaxLabel;
    @BindView(R.id.fanSpeedMin)Spinner fanSpeedMin;
    @BindView(R.id.fanSpeedMax)Spinner fanSpeedMax;
    @BindView(R.id.fanSpeedMinLabel)TextView fanSpeedMinLabel;
    @BindView(R.id.fanSpeedMaxLabel)TextView fanSpeedMaxLabel;
    
    
    @BindView(R.id.equipmentIp) EditText equipAddr;
    
    @BindView(R.id.analog1RTUTest) Spinner coolingTest;
    @BindView(R.id.analog2RTUTest) Spinner spTest;
    @BindView(R.id.analog3RTUTest) Spinner heatingTest;
    @BindView(R.id.humidificationTest) Spinner humidificationTest;
    @BindView(R.id.oaMinTest) Spinner oaMinTest;
    @BindView(R.id.buttonNext)
    Button mNext;

    @BindView(R.id.btnEditIp)
    ImageView btnEditIp;

    @BindView(R.id.zone_type) TextView zoneType;
    @BindView(R.id.sp_zone_type) ToggleButton zoneTypeSelection;
    VavIERtu systemProfile = null;
    String PROFILE = "VAV_IE_RTU";
    Prefs prefs;
    boolean isFromReg = false;
    
    private final CompositeDisposable disposable = new CompositeDisposable();
    
    public static VavAnalogRtuProfile newInstance()
    {
        return new VavAnalogRtuProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_rtuie, container, false);
        ButterKnife.bind(this, rootView);
        if(getArguments() != null) {
            isFromReg = getArguments().getBoolean("REGISTRATION_WIZARD");
        }
        btnEditIp.setOnClickListener(view1 -> equipAddr.setEnabled(true));
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        prefs = new Prefs(getContext().getApplicationContext());
        if ((L.ccu().systemProfile instanceof VavIERtu))
        {
            systemProfile = (VavIERtu) L.ccu().systemProfile;
            analog1Cb.setChecked(systemProfile.getConfigEnabled("cooling") > 0);
            analog2Cb.setChecked(systemProfile.getConfigEnabled("fan") > 0);
            analog3Cb.setChecked(systemProfile.getConfigEnabled("heating") > 0);
            humidificationCb.setChecked(systemProfile.getConfigEnabled("humidification") > 0);
            refreshUI();
        } else {
    
            disposable.add(RxjavaUtil.executeBackgroundTaskWithDisposable(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Loading System Profile"),
                () -> {
                    if (systemProfile != null) {
                        systemProfile.deleteSystemEquip();
                        L.ccu().systemProfile = null; //Makes sure that System Algos dont run until new profile is ready.
                    }
                    systemProfile = new VavIERtu();
                    systemProfile.addSystemEquip();
                    L.ccu().systemProfile = systemProfile;
                },
                () -> {
                    refreshUI();
                    ProgressDialogUtils.hideProgressDialog();
                }
            ));
        }

        if(isFromReg){
            mNext.setVisibility(View.VISIBLE);
        }
        else {
            mNext.setVisibility(View.GONE);
        }

        mNext.setOnClickListener(v -> {
            goTonext();
            mNext.setEnabled(false);
        });

        analog1Cb.setOnCheckedChangeListener(this);
        analog2Cb.setOnCheckedChangeListener(this);
        analog3Cb.setOnCheckedChangeListener(this);
        humidificationCb.setOnCheckedChangeListener(this);
        
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

            @Override
            public void onViewAttachedToWindow(View view) {
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                if (Globals.getInstance().isTestMode()) {
                    Globals.getInstance().setTestMode(false);
                }
            }
        });
        setSpinnerDropDownIcon();
    }
    
    private void refreshUI() {
        setupAnalogLimitSelectors();
        setupEquipAddrEditor();
        zoneTypeSelection.setChecked(systemProfile.getConfigVal("multiZone") > 0);
        zoneType.setText(systemProfile.getConfigVal("multiZone")> 0?"Multi Zone":"Single Zone");
        handleFanConfigViews(systemProfile.getConfigVal("multiZone") > 0);
        setUpZoneTypeListener();
    }
    
    private void setUpZoneTypeListener() {
        zoneTypeSelection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            zoneTypeSelection.setEnabled(false);
            zoneType.setText(isChecked?"Multi Zone":"Single Zone");
            systemProfile.setConfigVal("multiZone",(isChecked?1.0:0.0));
            handleFanConfigViews(isChecked);
            systemProfile.handleMultiZoneEnable(isChecked?1.0:0.0);
            if (systemProfile.getConfigVal("multiZone") > 0) {
                spTest.setAdapter(getSpAdapter());
            } else {
                spTest.setAdapter(getZeroToHundredArrayAdapter());
            }
            zoneTypeSelection.setEnabled(true);
        });
    }
    
    public void setupEquipAddrEditor() {
        String eqIp = CCUHsApi.getInstance().readDefaultStrVal("point and system and config and ie and ipAddress");


        // As of now for testing we need edit option so commented bellow code
         /*
            String subnetMaskAddress="255.255.255.0";
            final String[] choices = {eqIp,subnetMaskAddress};
            ArrayAdapter<String> addressAdapter =new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_item, choices);
            addressAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            equipAddr.setAdapter(addressAdapter);
        */
        equipAddr.setText(eqIp);
        equipAddr.setOnClickListener(v -> {
            final EditText editText = new EditText(getActivity());
        
            KeyListener keyListener = DigitsKeyListener.getInstance("0123456789.");
            editText.setKeyListener(keyListener);
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                         .setTitle("Internet Equipment IP")
                                         .setMessage(eqIp)
                                         .setView(editText)
                                         .setPositiveButton("Save", (dialog1, which) -> {
                                             if (editText.getText().toString().trim().length() == 0) {
                                                 Toast.makeText(getActivity(), "IP Address Empty", Toast.LENGTH_SHORT).show();
                                                 return;
                                             }
                                             CCUHsApi.getInstance().writeDefaultVal("point and system and config and ie and ipAddress",editText.getText().toString().trim());
                                             equipAddr.setText(editText.getText().toString());
                                         })
                                         .setNegativeButton("Cancel", null)
                                         .create();
            dialog.show();
        });
    }

    private void goTonext() {

        prefs.setBoolean("PROFILE_SETUP",true);
        prefs.setString("PROFILE",PROFILE);
        ((FreshRegistration)getActivity()).selectItem(19);
    }
    
    private void setupAnalogLimitSelectors() {
        ArrayList<Double> coolingDatArr = new ArrayList<>();
        for (double cdat = 55;  cdat <= 70.0; cdat++)
        {
            coolingDatArr.add(cdat);
        }
        ArrayAdapter<Double> coolingDatAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, coolingDatArr);
        coolingDatAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        double val = systemProfile.getConfigVal("cooling and dat and min");
        coolingDatMin.setAdapter(coolingDatAdapter);
        coolingDatMin.setSelection(coolingDatAdapter.getPosition(val), false);
        coolingDatMax.setAdapter(coolingDatAdapter);
        val = systemProfile.getConfigVal("cooling and dat and max");
        coolingDatMax.setSelection(val != 0 ? coolingDatAdapter.getPosition(val) : coolingDatArr.size()-1 , false);
    
        ArrayAdapter<Double> spAdapter = getSpAdapter();
        spMin.setAdapter(spAdapter);
        val = systemProfile.getConfigVal("staticPressure and min");
        spMin.setSelection(spAdapter.getPosition(val) , false);
    
        spMax.setAdapter(spAdapter);
        val = systemProfile.getConfigVal("staticPressure and max");
        spMax.setSelection(val != 0 ? spAdapter.getPosition(val) : spAdapter.getCount()-1, false);
    
        ArrayList<Double> heatingDatArr = new ArrayList<>();
        for (double hdat = 75;  hdat <= 100; hdat++)
        {
            heatingDatArr.add(hdat);
        }
        ArrayAdapter<Double> heatingAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, heatingDatArr);
        heatingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        heatingDatMin.setAdapter(heatingAdapter);
        val = systemProfile.getConfigVal("heating and dat and min");
        heatingDatMin.setSelection(heatingAdapter.getPosition(val), false);
    
        heatingDatMax.setAdapter(heatingAdapter);
        val = systemProfile.getConfigVal("heating and dat and max");
        heatingDatMax.setSelection(val != 0 ? heatingAdapter.getPosition(val) : heatingAdapter.getPosition(100.0), false);
    
        
        ArrayAdapter<Double> coolingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, coolingDatArr);
        coolingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        coolingTest.setAdapter(coolingSatTestAdapter);
        coolingTest.setSelection(0,false);
    
        ArrayAdapter<Double> heatingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, heatingDatArr);
        heatingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        heatingTest.setAdapter(heatingSatTestAdapter);
        heatingTest.setSelection(0,false);
        
        ArrayAdapter<Double> percentSelectorAdapter = getZeroToHundredArrayAdapter();
        humidificationTest.setAdapter(percentSelectorAdapter);
        humidificationTest.setSelection(0,false);
        
        oaMinTest.setAdapter(percentSelectorAdapter);
        oaMinTest.setSelection(0,false);
        
        fanSpeedMin.setAdapter(percentSelectorAdapter);
        val = systemProfile.getConfigVal("fanSpeed and min");
        fanSpeedMin.setSelection(val != 0 ? percentSelectorAdapter.getPosition(val) : 0, false);
        
        fanSpeedMax.setAdapter(percentSelectorAdapter);
        val = systemProfile.getConfigVal("fanSpeed and max");
        fanSpeedMax.setSelection(val != 0 ? percentSelectorAdapter.getPosition(val) : percentSelectorAdapter.getCount()-1,
                                 false);
        
        if (systemProfile.getConfigVal("multiZone") > 0) {
            spTest.setAdapter(spAdapter);
        } else {
            spTest.setAdapter(percentSelectorAdapter);
        }
        spTest.setSelection(0,false);
    
        coolingDatMin.setOnItemSelectedListener(this);
        coolingDatMax.setOnItemSelectedListener(this);
        spMin.setOnItemSelectedListener(this);
        spMax.setOnItemSelectedListener(this);
        heatingDatMin.setOnItemSelectedListener(this);
        heatingDatMax.setOnItemSelectedListener(this);
        coolingTest.setOnItemSelectedListener(this);
        spTest.setOnItemSelectedListener(this);
        heatingTest.setOnItemSelectedListener(this);
        humidificationTest.setOnItemSelectedListener(this);
        oaMinTest.setOnItemSelectedListener(this);
        fanSpeedMin.setOnItemSelectedListener(this);
        fanSpeedMax.setOnItemSelectedListener(this);
        
    }
    
    private ArrayAdapter<Double> getZeroToHundredArrayAdapter() {
        ArrayList<Double> zoroToHundred = new ArrayList<>();
        for (double i = 0;  i <= 100.0; i++) {
            zoroToHundred.add(i);
        }
        ArrayAdapter<Double> adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return adapter;
    }
    
    private ArrayAdapter<Double> getSpAdapter() {
        ArrayList<Double> spArr = new ArrayList<>();
        for (int sp = 2;  sp <= 20; sp++)
        {
            spArr.add((double)sp/10);
        }
        ArrayAdapter<Double> spAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, spArr);
        spAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return spAdapter;
    }
    
    @SuppressLint("NonConstantResourceId") @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch (buttonView.getId())
        {
            case R.id.analog1RTU:
                setSelectionBackground("cooling", isChecked);
                break;
            case R.id.analog2RTU:
                setSelectionBackground("fan", isChecked);
                break;
            case R.id.analog3RTU:
                setSelectionBackground("heating", isChecked);
                break;
            case R.id.humidificationCb:
                setSelectionBackground("humidification", isChecked);
        }
    }
    
    @SuppressLint("NonConstantResourceId") @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3)
    {
        double val = Double.parseDouble(arg0.getSelectedItem().toString());
        switch (arg0.getId())
        {
            
            case R.id.coolingDatMin:
                setConfigBackground("cooling and dat and min", val);
                break;
            case R.id.coolingDatMax:
                setConfigBackground("cooling and dat and max", val);
                break;
            case R.id.spMin:
                setConfigBackground("staticPressure and min", val);
                break;
            case R.id.spMax:
                setConfigBackground("staticPressure and max", val);
                break;
            case R.id.heatingDatMin:
                setConfigBackground("heating and dat and min", val);
                break;
            case R.id.heatingDatMax:
                setConfigBackground("heating and dat and max", val);
                break;
            case R.id.fanSpeedMin:
                setConfigBackground("fanSpeed and min", val);
                break;
            case R.id.fanSpeedMax:
                setConfigBackground("fanSpeed and max", val);
                break;
            case R.id.analog1RTUTest:
            case R.id.analog3RTUTest:
                Globals.getInstance().setTestMode(true);
                IEDeviceHandler.getInstance().sendDatClgSetpoint(val);
                break;
            case R.id.analog2RTUTest:
                Globals.getInstance().setTestMode(true);
                IEDeviceHandler.getInstance().sendFanControl(val, CCUHsApi.getInstance());
                break;
            case R.id.humidificationTest:
                Globals.getInstance().setTestMode(true);
                IEDeviceHandler.getInstance().sendBuildingHumidity(val);
                break;
            case R.id.oaMinTest:
                Globals.getInstance().setTestMode(true);
                break;
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
        
    }
    
    private void setConfigBackground(String tags, double val) {
        disposable.add(RxjavaUtil.executeBackgroundWithDisposable(
            () -> systemProfile.setConfigVal(tags, val)
        ));
    }
    
    private void setSelectionBackground(String analog, boolean selected) {
        disposable.add(RxjavaUtil.executeBackgroundTaskWithDisposable(
            () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Saving VAV System Configuration"),
            () -> systemProfile.setConfigEnabled(analog, selected ? 1: 0),
            () -> {
                if (!selected) {
                    updateSystemMode();
                }
                ProgressDialogUtils.hideProgressDialog();
            }
        ));
    }
    
    private void setUserIntentBackground(String query, double val) {
        disposable.add(RxjavaUtil.executeBackgroundWithDisposable(
            () -> TunerUtil.writeSystemUserIntentVal(query, val)
        ));
    }
    
    public void updateSystemMode() {
        SystemMode systemMode = SystemMode.values()[(int)systemProfile.getUserIntentVal("conditioning and mode")];
        if (systemMode == SystemMode.OFF) {
            return;
        }
        if ((systemMode == SystemMode.AUTO && (!systemProfile.isCoolingAvailable() || !systemProfile.isHeatingAvailable()))
            || (systemMode == SystemMode.COOLONLY && !systemProfile.isCoolingAvailable())
            || (systemMode == SystemMode.HEATONLY && !systemProfile.isHeatingAvailable())) {
            
            SystemProfileUtil.showConditioningDisabledDialog(getActivity(), systemMode);
        }
    }
    
    private void setSpinnerDropDownIcon(){

        CCUUiUtil.setSpinnerDropDownColor(coolingDatMin,getContext());
        CCUUiUtil.setSpinnerDropDownColor(coolingDatMax,getContext());
        CCUUiUtil.setSpinnerDropDownColor(heatingDatMin,getContext());
        CCUUiUtil.setSpinnerDropDownColor(heatingDatMax,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spMin,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spMax,getContext());
        CCUUiUtil.setSpinnerDropDownColor(coolingTest,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spTest,getContext());
        CCUUiUtil.setSpinnerDropDownColor(heatingTest,getContext());
        CCUUiUtil.setSpinnerDropDownColor(humidificationTest,getContext());
        CCUUiUtil.setSpinnerDropDownColor(oaMinTest,getContext());

    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }
    
    private void handleFanConfigViews(boolean multiZone) {
        
        spMin.setVisibility(multiZone ? View.VISIBLE : View.GONE);
        spMinLabel.setVisibility(multiZone ? View.VISIBLE : View.GONE);
        spMax.setVisibility(multiZone ? View.VISIBLE : View.GONE);
        spMaxLabel.setVisibility(multiZone ? View.VISIBLE : View.GONE);
    
        fanSpeedMin.setVisibility(multiZone ? View.GONE : View.VISIBLE);
        fanSpeedMinLabel.setVisibility(multiZone ? View.GONE : View.VISIBLE);
        fanSpeedMax.setVisibility(multiZone ? View.GONE : View.VISIBLE);
        fanSpeedMaxLabel.setVisibility(multiZone ? View.GONE : View.VISIBLE);
        
    }
}
