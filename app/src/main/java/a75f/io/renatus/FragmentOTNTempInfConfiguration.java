package a75f.io.renatus;

import static a75f.io.device.bacnet.BacnetUtilKt.addBacnetTags;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.otn.OTNConfiguration;
import a75f.io.logic.bo.building.otn.OTNProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.util.DesiredTempDisplayMode;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.views.CustomCCUSwitch;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.rxjava3.disposables.CompositeDisposable;


/**
 * created by spoorthidev on 21-July-2021
 */
public class FragmentOTNTempInfConfiguration extends BaseDialogFragment {
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    public static String ID = FragmentOTNTempInfConfiguration.class.getSimpleName();
    private static String LOG_TAG = "FragmentOTNTempInfConfiguration";
    static final int TEMP_OFFSET_LIMIT = 100;
    private short    mNodeAddress;
    String floorRef;
    String zoneRef;
    OTNProfile mOTNProfile;
    OTNConfiguration mOTNConfig;


    @BindView(R.id.temperatureOffset)
    NumberPicker mTemperatureOffset;

    @BindView(R.id.setBtn)
    Button mSetbtn;

    @BindView(R.id.toogleautoforceoccupied)
    CustomCCUSwitch mAutoforceoccupied;

    @BindView(R.id.toogleautoaway)
    CustomCCUSwitch mAutoAway;


    @BindView(R.id.zonePriority)
    Spinner mZonePriority;

    @Override
    public  String getIdString() {
        return ID;
    }

    public static FragmentOTNTempInfConfiguration newInstance(short meshAddress, String roomName, String mFloorName, ProfileType profileType)
    {
        FragmentOTNTempInfConfiguration f = new FragmentOTNTempInfConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, mFloorName);
        bundle.putString(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.toString());
        f.setArguments(bundle);
        return f;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = 1165;//ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = 720;//ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_otn_tempinf, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);


        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mOTNProfile =(OTNProfile) L.getProfile(mNodeAddress);

        mTemperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        mTemperatureOffset.setDisplayedValues(nums);
        mTemperatureOffset.setMinValue(0);
        mTemperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        mTemperatureOffset.setValue(TEMP_OFFSET_LIMIT);
        mTemperatureOffset.setWrapSelectorWheel(false);

        if(mOTNProfile != null){
            mOTNConfig = (OTNConfiguration) mOTNProfile.getProfileConfiguration(mNodeAddress);

            mTemperatureOffset.setValue((int) (mOTNConfig.gettempOffset() + TEMP_OFFSET_LIMIT));
            mZonePriority.setSelection(mOTNConfig.getzonePriority());
            mAutoAway.setChecked(mOTNConfig.getautoAway());
            mAutoforceoccupied.setChecked(mOTNConfig.getautoforceOccupied());
        }else {
            mOTNProfile = new OTNProfile();
            mZonePriority.setSelection(2);
        }
        CCUUiUtil.setSpinnerDropDownColor(mZonePriority,this.getContext());
    }

    @OnClick(R.id.setBtn)
    void setOnClick(View v) {
        mSetbtn.setEnabled(false);
        compositeDisposable.add(RxjavaUtil.executeBackgroundTaskWithDisposable(
                ()->{
                    ProgressDialogUtils.showProgressDialog(getActivity(), "Saving OTN Configuration");
                },
                ()->{
                    setupOTNProfile();
                    L.saveCCUState();
                    LSerial.getInstance().sendSeedMessage(false,false, mNodeAddress, zoneRef,floorRef);
                    DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance());
                    },
                ()->{
                    addBacnetTags(requireContext(), floorRef, zoneRef);
                    ProgressDialogUtils.hideProgressDialog();
                    FragmentOTNTempInfConfiguration.this.closeAllBaseDialogFragments();
                    getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                }
        ));
    }

    private void setupOTNProfile() {
        OTNConfiguration otn = new OTNConfiguration();
        otn.settempOffset((double) mTemperatureOffset.getValue() - TEMP_OFFSET_LIMIT);
        otn.setPriority(ZonePriority.values()[mZonePriority.getSelectedItemPosition()]);
        otn.setzonePriority(mZonePriority.getSelectedItemPosition());
        otn.setautoforceOccupied(mAutoforceoccupied.isChecked());
        otn.setautoAway(mAutoAway.isChecked());


        mOTNProfile.getProfileConfiguration().put(mNodeAddress,otn);

        if(mOTNConfig == null ){
            Log.d(LOG_TAG, "Creating new config");
            mOTNProfile.addOTNEquip(ProfileType.OTN,mNodeAddress,otn,floorRef,zoneRef);
        }else{
            Log.d(LOG_TAG, "Updating config");
            mOTNProfile.updateOTN(ProfileType.OTN,mNodeAddress,otn,floorRef,zoneRef);
        }
        L.ccu().zoneProfiles.add(mOTNProfile);

        CcuLog.d(L.TAG_CCU_UI, "Set OTN Config: Profiles - " + L.ccu().zoneProfiles.size());

    }
}
