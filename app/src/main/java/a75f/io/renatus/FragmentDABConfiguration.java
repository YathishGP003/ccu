package a75f.io.renatus;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.util.ArrayList;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.dab.DabProfile;
import a75f.io.logic.bo.building.dab.DabProfileConfiguration;
import a75f.io.logic.bo.building.definitions.DamperShape;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.ButterKnife;

import static a75f.io.logic.bo.building.definitions.DamperType.ZeroToTenV;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class FragmentDABConfiguration extends BaseDialogFragment
{
    public static final String ID = FragmentDABConfiguration.class.getSimpleName();
    
    static final int TEMP_OFFSET_LIMIT = 100;
    
    private short    mSmartNodeAddress;
    private NodeType mNodeType;
    private Zone     mZone;
    
    LinearLayout damper1layout;
    LinearLayout damper2layout;
    Spinner      damper1Type;
    Spinner      damper1Size;
    Spinner      damper1Shape;
    Spinner      damper2Type;
    Spinner      damper2Size;
    Spinner      damper2Shape;
    
    LinearLayout reheatOptionLayout;
    Button       setButton;
    Spinner      zonePriority;
    NumberPicker temperatureOffset;
    NumberPicker maxCoolingDamperPos;
    NumberPicker minCoolingDamperPos;
    NumberPicker maxHeatingDamperPos;
    NumberPicker minHeatingDamperPos;
    ToggleButton enableOccupancyControl;
    ToggleButton enableCO2Control;
    ToggleButton enableIAQControl;
    
    private ProfileType             mProfileType;
    private DabProfile              mDabProfile;
    private DabProfileConfiguration mProfileConfig;
    
    private ArrayList<Damper.Parameters> mDampers = new ArrayList<Damper.Parameters>();
    ArrayAdapter<String> damper1TypesAdapter;
    ArrayAdapter<String> damper2TypesAdapter;
    
    
    DamperType damper1TypeSelected = ZeroToTenV;
    DamperType damper2TypeSelected = ZeroToTenV;
    
    String floorRef;
    String zoneRef;
    
    public FragmentDABConfiguration()
    {
    }
    
    public static FragmentDABConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName, ProfileType profileType)
    {
        FragmentDABConfiguration f = new FragmentDABConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString());
        bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal());
        f.setArguments(bundle);
        return f;
    }
    
    @Override
    public String getIdString()
    {
        return ID;
    }
    
    
    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = 1165;//ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = 672;//ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
        //setTitle();
    }
    
    private void setTitle() {
        Dialog dialog = getDialog();
        
        if (dialog == null) {
            return;
        }
        
        
        dialog.setTitle("DAB");
        TextView titleView = this.getDialog().findViewById(android.R.id.title);
        if(titleView != null)
        {
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(getResources().getColor(R.color.orange_75f));
            titleView.setTextSize(28);
            titleView.setTypeface(null, Typeface.BOLD);
            
            ViewGroup viewGroup = (ViewGroup) titleView.getRootView();
            Button button = new Button(getActivity());
            button.setText(" < ");
            button.setTextSize(30);
            button.setTypeface(null, Typeface.BOLD);
            button.setPadding(10,10,10,10);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            button.setLayoutParams(layoutParams);
            button.setGravity(Gravity.LEFT);
            button.setTextColor(getResources().getColor(R.color.orange_75f));
            button.setOnClickListener(v->removeDialogFragment(ID));
            button.setBackgroundColor(Color.TRANSPARENT);
            viewGroup.addView(button);
            
        }
        int titleDividerId = getContext().getResources()
                                         .getIdentifier("titleDivider", "id", "android");
        
        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider != null) {
            titleDivider.setBackgroundColor(getContext().getResources()
                                                        .getColor(R.color.transparent));
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_dab_config, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        //mZone = L.findZoneByName(mFloorName, mRoomName);
        mProfileType = ProfileType.values()[getArguments().getInt(FragmentCommonBundleArgs.PROFILE_TYPE)];
        ButterKnife.bind(this, view);
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        reheatOptionLayout = (LinearLayout)view.findViewById(R.id.dabReheatOptions);
        
        setButton = (Button) view.findViewById(R.id.setBtn);
        
        mDabProfile = (DabProfile) L.getProfile(mSmartNodeAddress);
        
        if (mDabProfile != null) {
            CcuLog.d(L.TAG_CCU_UI,  "Get DABConfig: ");
            mProfileConfig = (DabProfileConfiguration) mDabProfile.getProfileConfiguration(mSmartNodeAddress);
        } else {
            CcuLog.d(L.TAG_CCU_UI, "Create DABProfile: ");
            mDabProfile = new DabProfile();
        }
        
        
      /*  damper1layout  = (LinearLayout)view.findViewById(R.id.damper1layout);
        damper1layout.setVisibility(View.VISIBLE);
    
        damper2layout  = (LinearLayout)view.findViewById(R.id.damper2layout);
        damper2layout.setVisibility(View.VISIBLE);*/
        
        damper1Size = view.findViewById(R.id.damper1Size);
        ArrayAdapter<CharSequence> damperSizeAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.damper_size, R.layout.spinner_dropdown_item);
        damperSizeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        damper1Size.setAdapter(damperSizeAdapter);
        
        ArrayList<String> damperShapes = new ArrayList<>();
        for (DamperShape shape : DamperShape.values()) {
            damperShapes.add(shape.displayName);
        }
        ArrayAdapter<String> damperShapeAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_dropdown_item, damperShapes);
        damperShapeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        damper1Shape = view.findViewById(R.id.damper1Shape);
        damper1Shape.setAdapter(damperShapeAdapter);
    
    
        damper2Size = view.findViewById(R.id.damper2Size);
        damper2Size.setAdapter(damperSizeAdapter);
        damper2Shape = view.findViewById(R.id.damper2Shape);
        damper2Shape.setAdapter(damperShapeAdapter);
        
        
        temperatureOffset = (NumberPicker) view.findViewById(R.id.temperatureOffset);
        setNumberPickerDividerColor(temperatureOffset);
        temperatureOffset.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        String[] nums = new String[TEMP_OFFSET_LIMIT * 2 + 1];//{"-4","-3","-2","-1","0","1","2","3","4"};
        for (int nNum = 0; nNum < TEMP_OFFSET_LIMIT * 2 + 1; nNum++)
            nums[nNum] = String.valueOf((float) (nNum - TEMP_OFFSET_LIMIT) / 10);
        temperatureOffset.setDisplayedValues(nums);
        temperatureOffset.setMinValue(0);
        temperatureOffset.setMaxValue(TEMP_OFFSET_LIMIT * 2);
        temperatureOffset.setValue(TEMP_OFFSET_LIMIT);
        temperatureOffset.setWrapSelectorWheel(false);
        
        maxCoolingDamperPos = view.findViewById(R.id.maxDamperPos);
        setNumberPickerDividerColor(maxCoolingDamperPos);
        maxCoolingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        maxCoolingDamperPos.setMinValue(0);
        maxCoolingDamperPos.setMaxValue(100);
        maxCoolingDamperPos.setValue(100);
        maxCoolingDamperPos.setWrapSelectorWheel(false);
        
        minCoolingDamperPos = view.findViewById(R.id.minDamperPos);
        setNumberPickerDividerColor(minCoolingDamperPos);
        minCoolingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minCoolingDamperPos.setMinValue(0);
        minCoolingDamperPos.setMaxValue(100);
        minCoolingDamperPos.setValue(40);
        minCoolingDamperPos.setWrapSelectorWheel(false);
        
        maxHeatingDamperPos = view.findViewById(R.id.maxHeatingDamperPos);
        setNumberPickerDividerColor(maxHeatingDamperPos);
        maxHeatingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        maxHeatingDamperPos.setMinValue(0);
        maxHeatingDamperPos.setMaxValue(100);
        maxHeatingDamperPos.setValue(100);
        maxHeatingDamperPos.setWrapSelectorWheel(false);
        
        minHeatingDamperPos = view.findViewById(R.id.minHeatingDamperPos);
        setNumberPickerDividerColor(minHeatingDamperPos);
        minHeatingDamperPos.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minHeatingDamperPos.setMinValue(0);
        minHeatingDamperPos.setMaxValue(100);
        minHeatingDamperPos.setValue(40);
        minHeatingDamperPos.setWrapSelectorWheel(false);
        
        setDividerColor(temperatureOffset);
        setDividerColor(maxCoolingDamperPos);
        setDividerColor(minCoolingDamperPos);
        setDividerColor(maxHeatingDamperPos);
        setDividerColor(minHeatingDamperPos);


        enableOccupancyControl = view.findViewById(R.id.enableOccupancyControl);
        enableCO2Control = view.findViewById(R.id.enableCO2Control);
        enableIAQControl = view.findViewById(R.id.enableIAQControl);
        zonePriority = view.findViewById(R.id.zonePriority);
        ArrayAdapter<CharSequence> zonePriorityAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.zone_priority, R.layout.spinner_dropdown_item);
        zonePriorityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        zonePriority.setAdapter(zonePriorityAdapter);
        
        damper1Type = view.findViewById(R.id.damper1Type);
        ArrayList<String> damper1Types = new ArrayList<>();
        for (DamperType damper : DamperType.values()) {
            damper1Types.add(damper.displayName+ (damper.name().equals("MAT") ? "": " (Analog-out1)"));
        }
        damper1TypesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, damper1Types);
        damper1TypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        damper1Type.setAdapter(damper1TypesAdapter);
        
        damper1Type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                damper1TypeSelected = DamperType.values()[position];
    
              /*  if(position == 4)
                damper1layout.setVisibility(View.INVISIBLE);
                else
                damper1layout.setVisibility(View.VISIBLE);
                damper1layout.invalidate();*/
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        damper2Type = view.findViewById(R.id.damper2Type);
    
        ArrayList<String> damper2Types = new ArrayList<>();
        for (DamperType damper : DamperType.values()) {
            damper2Types.add(damper.displayName+ (damper.name().equals("MAT") ? "": " (Analog-out2)"));
        }
        damper2TypesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, damper2Types);
        damper2TypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        damper2Type.setAdapter(damper2TypesAdapter);
    
        damper2Type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                damper2TypeSelected = DamperType.values()[position];
                damper2Type.setSelection(position);
               /* if(position == 4)
                    damper2layout.setVisibility(View.INVISIBLE);
                else
                    damper2layout.setVisibility(View.VISIBLE);
                damper2layout.invalidate();*/
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        
        if (mProfileConfig != null) {
            damper1Type.setSelection(mProfileConfig.damper1Type, false);
            damper1Size.setSelection(damperSizeAdapter.getPosition(String.valueOf(mProfileConfig.damper1Size)), false);
            damper1Shape.setSelection(damperShapeAdapter.getPosition(DamperShape.values()[mProfileConfig.damper1Shape].displayName), false);
    
            damper2Type.setSelection(mProfileConfig.damper2Type, false);
            damper2Size.setSelection(damperSizeAdapter.getPosition(String.valueOf(mProfileConfig.damper2Size)), false);
            damper2Shape.setSelection(damperShapeAdapter.getPosition(DamperShape.values()[mProfileConfig.damper2Shape].displayName), false);
            
            enableOccupancyControl.setChecked(mProfileConfig.enableOccupancyControl);
            enableCO2Control.setChecked(mProfileConfig.enableCO2Control);
            enableIAQControl.setChecked(mProfileConfig.enableIAQControl);
            zonePriority.setSelection(mProfileConfig.getPriority().ordinal());
            int offsetIndex = (int)mProfileConfig.temperaturOffset+TEMP_OFFSET_LIMIT;
            temperatureOffset.setValue(offsetIndex);
            minCoolingDamperPos.setValue(mProfileConfig.minDamperCooling);
            maxCoolingDamperPos.setValue(mProfileConfig.maxDamperCooling);
            minHeatingDamperPos.setValue(mProfileConfig.minDamperHeating);
            maxHeatingDamperPos.setValue(mProfileConfig.maxDamperHeating);
            
            
        } else {
            zonePriority.setSelection(2);//NORMAL
        }
        
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                
                new AsyncTask<Void, Void, Void>() {
                    
                    @Override
                    protected void onPreExecute() {
                        setButton.setEnabled(false);
                        ProgressDialogUtils.showProgressDialog(getActivity(),"Saving DAB Configuration");
                        super.onPreExecute();
                    }
                    
                    @Override
                    protected Void doInBackground( final Void ... params ) {
                        setupDabZoneProfile();
                        L.saveCCUState();
                        
                        return null;
                    }
                    
                    @Override
                    protected void onPostExecute( final Void result ) {
                        ProgressDialogUtils.hideProgressDialog();
                        FragmentDABConfiguration.this.closeAllBaseDialogFragments();
                        getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                
            }
        });
        
    }
    
    private void setupDabZoneProfile() {
        
        DabProfileConfiguration dabConfig = new DabProfileConfiguration();
        dabConfig.damper1Type = damper1TypeSelected.ordinal();
        dabConfig.damper1Size = Integer.parseInt(damper1Size.getSelectedItem().toString());
        dabConfig.damper1Shape = DamperType.values()[damper1Shape.getSelectedItemPosition()].ordinal();
        dabConfig.damper2Type = damper2TypeSelected.ordinal();
        dabConfig.damper2Size = Integer.parseInt(damper2Size.getSelectedItem().toString());
        dabConfig.damper2Shape = DamperType.values()[damper2Shape.getSelectedItemPosition()].ordinal();
        
        dabConfig.setNodeType(mNodeType);
        dabConfig.setNodeAddress(mSmartNodeAddress);
        dabConfig.enableOccupancyControl = enableOccupancyControl.isChecked();
        dabConfig.enableCO2Control = enableCO2Control.isChecked();
        dabConfig.enableIAQControl = enableIAQControl.isChecked();
        dabConfig.setPriority(ZonePriority.values()[zonePriority.getSelectedItemPosition()]);
        dabConfig.minDamperCooling = (minCoolingDamperPos.getValue());
        dabConfig.maxDamperCooling = (maxCoolingDamperPos.getValue());
        dabConfig.minDamperHeating = (minHeatingDamperPos.getValue());
        dabConfig.maxDamperHeating = (maxHeatingDamperPos.getValue());
        dabConfig.temperaturOffset = temperatureOffset.getValue() - TEMP_OFFSET_LIMIT;
        
        Output analog1Op = new Output();
        analog1Op.setAddress(mSmartNodeAddress);
        analog1Op.setPort(Port.ANALOG_OUT_ONE);
        analog1Op.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(damper1TypeSelected.displayName);
        dabConfig.getOutputs().add(analog1Op);
    
        Output analog2Op = new Output();
        analog2Op.setAddress(mSmartNodeAddress);
        analog2Op.setPort(Port.ANALOG_OUT_TWO);
        analog2Op.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(damper2TypeSelected.displayName);
        dabConfig.getOutputs().add(analog2Op);
    
        mDabProfile.getProfileConfiguration().put(mSmartNodeAddress, dabConfig);
        if (mProfileConfig == null) {
            mDabProfile.addDabEquip(mSmartNodeAddress, dabConfig, floorRef, zoneRef );
        } else {
            mDabProfile.updateDabEquip(dabConfig);
        }
        L.ccu().zoneProfiles.add(mDabProfile);
        CcuLog.d(L.TAG_CCU_UI, "Set DAB Config: Profiles - "+L.ccu().zoneProfiles.size());
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
    private void setNumberPickerDividerColor(NumberPicker pk) {
        Class<?> numberPickerClass = null;
        try {
            numberPickerClass = Class.forName("android.widget.NumberPicker");
            Field selectionDivider = numberPickerClass.getDeclaredField("mSelectionDivider");
            selectionDivider.setAccessible(true);
            //if(!CCUUtils.isxlargedevice(getActivity())) {
            selectionDivider.set(pk, getResources().getDrawable(R.drawable.line_959595));
            //}else{
            //   selectionDivider.set(pk, getResources().getDrawable(R.drawable.connect_192x48_orange));
            //}
            
        } catch (ClassNotFoundException e) {
            Log.e("class not found",e.toString());
        } catch (NoSuchFieldException e) {
            Log.e("NoSuchFieldException",e.toString());
        } catch (IllegalAccessException e) {
            Log.e("IllegalAccessException",e.toString());
        }catch (Exception e){
            Log.e("dividerexception",e.getMessage().toString());
        }
    }
}