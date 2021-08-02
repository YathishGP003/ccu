package a75f.io.renatus;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
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

import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.bpos.BPOSConfiguration;
import a75f.io.logic.bo.building.bpos.BPOSProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * created by spoorthidev on 21-July-2021
 */
public class FragmentBPOSTempInfConfiguration extends BaseDialogFragment {

    public static String ID = FragmentBPOSTempInfConfiguration.class.getSimpleName();
    private static String LOG_TAG = "FragmentBPOSTempInfConfiguration";
    static final int TEMP_OFFSET_LIMIT = 100;
    private short    mNodeAddress;
    String floorRef;
    String zoneRef;
    BPOSProfile mBPOSProfile ;
    BPOSConfiguration mBPOSConfig;


    @BindView(R.id.temperatureOffset)
    NumberPicker mTemperatureOffset;

    @BindView(R.id.setBtn)
    Button mSetbtn;

    @BindView(R.id.toogleautoforceoccupied)
    ToggleButton mAutoforceoccupied;

    @BindView(R.id.toogleautoaway)
    ToggleButton mAutoAway;


    @BindView(R.id.zonePriority)
    Spinner mZonePriority;

    @Override
    public  String getIdString() {
        return ID;
    }

    public static FragmentBPOSTempInfConfiguration newInstance(short meshAddress, String roomName, String mFloorName, ProfileType profileType)
    {
        FragmentBPOSTempInfConfiguration f = new FragmentBPOSTempInfConfiguration();
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
        View view = inflater.inflate(R.layout.fragment_bpos_tempinf, container, false);
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

        mBPOSProfile =(BPOSProfile) L.getProfile(mNodeAddress);

        mTemperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        mTemperatureOffset.setDisplayedValues(nums);
        mTemperatureOffset.setMinValue(0);
        mTemperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        mTemperatureOffset.setValue(TEMP_OFFSET_LIMIT);
        mTemperatureOffset.setWrapSelectorWheel(false);

        if(mBPOSProfile != null){
            mBPOSConfig = (BPOSConfiguration) mBPOSProfile.getProfileConfiguration(mNodeAddress);

            mTemperatureOffset.setValue((int) (mBPOSConfig.gettempOffset() + TEMP_OFFSET_LIMIT));
            mZonePriority.setSelection(mBPOSConfig.getzonePriority());
            mAutoAway.setChecked(mBPOSConfig.getautoAway());
            mAutoforceoccupied.setChecked(mBPOSConfig.getautoforceOccupied());
        }else {
            mBPOSProfile = new BPOSProfile();
            mZonePriority.setSelection(2);
        }

    }

    @OnClick(R.id.setBtn)
    void setOnClick(View v) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected void onPreExecute() {
                mSetbtn.setEnabled(false);
                ProgressDialogUtils.showProgressDialog(getActivity(), "Saving BPOS Configuration");
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(final String... params) {
                setupbposProfile();
                L.saveCCUState();
                return null;
            }
            

            @Override
            protected void onPostExecute(final Void result) {
                ProgressDialogUtils.hideProgressDialog();
                FragmentBPOSTempInfConfiguration.this.closeAllBaseDialogFragments();
                getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private void setupbposProfile() {
        BPOSConfiguration bpos = new BPOSConfiguration();
        bpos.settempOffset(mTemperatureOffset.getValue() - TEMP_OFFSET_LIMIT);
        bpos.setPriority(ZonePriority.values()[mZonePriority.getSelectedItemPosition()]);
        bpos.setzonePriority(mZonePriority.getSelectedItemPosition());
        bpos.setautoforceOccupied(mAutoforceoccupied.isChecked());
        bpos.setautoAway(mAutoAway.isChecked());


        mBPOSProfile.getProfileConfiguration().put(mNodeAddress,bpos);

        if(mBPOSConfig == null ){
            Log.d(LOG_TAG, "Creating new config");
            mBPOSProfile.addBPOSEquip(ProfileType.BPOS,mNodeAddress,bpos,floorRef,zoneRef);
        }else{
            Log.d(LOG_TAG, "Updating config");
            mBPOSProfile.updateBPOS(ProfileType.BPOS,mNodeAddress,bpos,floorRef,zoneRef);
        }
        L.ccu().zoneProfiles.add(mBPOSProfile);

        CcuLog.d(L.TAG_CCU_UI, "Set BPOS Config: Profiles - " + L.ccu().zoneProfiles.size());

    }
}
