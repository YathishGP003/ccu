package a75f.io.renatus;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;

import java.lang.reflect.Field;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ccu.CazProfile;
import a75f.io.logic.bo.building.ccu.CazProfileConfig;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 2/22/19.
 */

public class FragmentTempInfConfiguration extends BaseDialogFragment
{
    public static final String TAG = "TempInfluence";
    public static final String ID = FragmentTempInfConfiguration.class.getSimpleName();

    static final int TEMP_OFFSET_LIMIT = 100;

    @BindView(R.id.zonePriority)
    Spinner      zonePriority;

    @BindView(R.id.temperatureOffset)
    NumberPicker temperatureOffset;

    @BindView(R.id.setBtn)
    Button setButton;

    private ProfileType             mProfileType;
    private CazProfile              mCcuAsZoneProfile;
    private CazProfileConfig mProfileConfig;

    private short    mSmartNodeAddress;
    private NodeType mNodeType;

    String floorRef;
    String zoneRef;

    @Override
    public String getIdString()
    {
        return ID;
    }

    public FragmentTempInfConfiguration()
    {
    }
    
    public static FragmentTempInfConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName)
    {
        FragmentTempInfConfiguration f = new FragmentTempInfConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString());
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
        View view = inflater.inflate(R.layout.fragment_tempinf_config, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));

        ButterKnife.bind(this, view);
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mCcuAsZoneProfile = (CazProfile) L.getProfile(mSmartNodeAddress);

        if (mCcuAsZoneProfile != null) {
            Log.d("CPUConfig", "Get Config: "+mCcuAsZoneProfile.getProfileType()+","+mCcuAsZoneProfile.getProfileConfiguration(mSmartNodeAddress)+","+mSmartNodeAddress);
            mProfileConfig = (CazProfileConfig) mCcuAsZoneProfile
                    .getProfileConfiguration(mSmartNodeAddress);
        } else {
            Log.d("CPUConfig", "Create Profile: ");
            mCcuAsZoneProfile = new CazProfile();

        }
        setDividerColor(temperatureOffset);
        temperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];//{"-4","-3","-2","-1","0","1","2","3","4"};
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        temperatureOffset.setDisplayedValues(nums);
        temperatureOffset.setMinValue(0);
        temperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        temperatureOffset.setValue(TEMP_OFFSET_LIMIT);
        temperatureOffset.setWrapSelectorWheel(false);
        setButton = (Button) view.findViewById(R.id.setBtn);
        zonePriority = view.findViewById(R.id.zonePriority);
        ArrayAdapter<CharSequence> zonePriorityAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.zone_priority, R.layout.spinner_dropdown_item);
        zonePriorityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        zonePriority.setAdapter(zonePriorityAdapter);


        if(mProfileConfig != null) {
            zonePriority.setSelection(mProfileConfig.getPriority().ordinal());
            int offsetIndex = (int) mProfileConfig.temperaturOffset + TEMP_OFFSET_LIMIT;
            temperatureOffset.setValue(offsetIndex);
        }else {
            zonePriority.setSelection(2);
        }
        
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
            
                new AsyncTask<String, Void, Void>() {
                
                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        ProgressDialogUtils.showProgressDialog(getActivity(),"Saving CCU As a Zone Configuration");
                        super.onPreExecute();
                    }
                
                    @Override
                    protected Void doInBackground(final String... params) {
                        setupCcuAsZoneProfile();
                        L.saveCCUState();
                        return null;
                    }
                
                    @Override
                    protected void onPostExecute( final Void result ) {
                        ProgressDialogUtils.hideProgressDialog();
                        FragmentTempInfConfiguration.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            
            }
        });
    }

    private void setupCcuAsZoneProfile() {

        CazProfileConfig cazConfig = new CazProfileConfig();

        cazConfig.setNodeType(mNodeType);
        cazConfig.setNodeAddress(mSmartNodeAddress);
        cazConfig.setPriority(ZonePriority.values()[zonePriority.getSelectedItemPosition()]);
        cazConfig.temperaturOffset = temperatureOffset.getValue() - TEMP_OFFSET_LIMIT;


        mCcuAsZoneProfile.getProfileConfiguration().put(mSmartNodeAddress, cazConfig);
        if (mProfileConfig == null) {
            mCcuAsZoneProfile.addCcuAsZoneEquip(mSmartNodeAddress, cazConfig, floorRef, zoneRef );
        } else {
            mCcuAsZoneProfile.updateCcuAsZone(cazConfig);
        }
        L.ccu().zoneProfiles.add(mCcuAsZoneProfile);
        CcuLog.d(L.TAG_CCU_UI, "Set CCU As Zone Config: Profiles - "+L.ccu().zoneProfiles.size());
    }
    private void setDividerColor(NumberPicker picker) {
        Field[] numberPickerFields = NumberPicker.class.getDeclaredFields();
        for (Field field : numberPickerFields) {
            if (field.getName().equals("mSelectionDivider")) {
                field.setAccessible(true);
                try {
                    field.set(picker, getResources().getDrawable(R.drawable.divider_np));
                } catch (IllegalArgumentException e) {
                    Log.v("NP", "Illegal Argument Exception");
                    e.printStackTrace();
                } catch (Resources.NotFoundException e) {
                    Log.v("NP", "Resources NotFound");
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    Log.v("NP", "Illegal Access Exception");
                    e.printStackTrace();
                }
                break;
            }
        }
    }

}
