package a75f.io.renatus.modbus;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import a75f.io.api.haystack.CCUHsApi;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.ModbusEquipsInfo;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.modbus.ModbusEquipTypes;
import a75f.io.logic.bo.building.modbus.ModbusProfile;
import a75f.io.logic.bo.util.DesiredTempDisplayMode;
import a75f.io.modbusbox.EquipsManager;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.EnergyDistributionAdapter;
import a75f.io.renatus.FloorPlanFragment;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FragmentModbusConfiguration extends BaseDialogFragment {

    public static final String ID = FragmentModbusConfiguration.class.getSimpleName();
    public ArrayList<Floor> floorList = new ArrayList();
    String floorRef;
    String zoneRef;
    EnergyDistributionAdapter energyDistributionAdapter;
    Comparator<Floor> floorComparator = (a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
    ModbusProfile modbusProfile;
    EquipmentDevice equipmentDevice;
    @BindView(R.id.spEquipmentType)
    Spinner spEquipmentType;
    @BindView(R.id.spAddress)
    Spinner spAddress;
    @BindView(R.id.setBtn)
    Button setBtn;
    @BindView(R.id.paramHeader1)
    LinearLayout paramHeader1;
    @BindView(R.id.paramHeader2)
    LinearLayout paramHeader2;
    @BindView(R.id.recyclerParams)
    RecyclerView recyclerParams;
    @BindView(R.id.editSlaveId)
    EditText editSlaveId;
    @BindView(R.id.ivEditAddress)
    ImageView ivEditAddress;
    @BindView(R.id.header_for_floors)
    TextView floorViewheader;
    @BindView(R.id.floorList)
    RecyclerView floorListView;
    @BindView(R.id.recyclerSubEquips)
    RecyclerView recyclerSubEquips;
    @BindView(R.id.toggleSelectAllParams)
    ToggleButton toggleSelectAllParams;

    @BindView(R.id.textTitleFragment)
    TextView textTitleFragment;
    ProfileType profileType;
    List<EquipmentDevice> equipmentDeviceCollection;
    RecyclerModbusParamAdapter recyclerModbusParamAdapter;
    private ModbusConfigurationAdapter modbusConfigurationAdapter;
    boolean isEditConfig = false;
    private short curSelectedSlaveId;
    String message;
    private CompoundButton.OnCheckedChangeListener toggleButtonChangeListener;

    public static FragmentModbusConfiguration newInstance(short meshAddress, String roomName, String floorName, ProfileType profileType) {
        FragmentModbusConfiguration f = new FragmentModbusConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal());
        f.setArguments(bundle);
        return f;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        curSelectedSlaveId = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);

        int profileOriginalValue = getArguments().getInt(FragmentCommonBundleArgs.PROFILE_TYPE);

        profileType = ProfileType.values()[profileOriginalValue];
        View view = inflater.inflate(R.layout.fragment_modbus_config, container, false);
        ButterKnife.bind(this, view);
        textTitleFragment = view.findViewById(R.id.textTitleFragment);
        message = getString(R.string.save_mb);

        if(profileType == ProfileType.MODBUS_EMR){
            textTitleFragment.setText(R.string.label_modbus_energy_meter);
            message = getString(R.string.save_em_mb);
        }else if(profileType == ProfileType.MODBUS_BTU){
            textTitleFragment.setText(R.string.label_modbus_btu_meter);
            message = getString(R.string.save_btu_mb);
        }
        setBtn.setOnClickListener(view1 -> {
            short selectedEquipSlaveId = (short) (spAddress.getSelectedItemPosition() + 1);
            if(!isEditConfig && null != equipmentDevice.getEquips()) {
                if(!HSUtil.getEquips(zoneRef).isEmpty()){
                    Toast.makeText(getActivity(), "Zone should have no equips to pair modbus with sub equips"
                            , Toast.LENGTH_LONG).show();
                    return;
                }
                if (L.isModbusSlaveIdExists(selectedEquipSlaveId)) {
                    Toast.makeText(getActivity(), "Slave Id " + selectedEquipSlaveId + " already exists, choose " +
                            "another slave id to proceed", Toast.LENGTH_LONG).show();
                    return;
                }
                for (EquipmentDevice equipmentDevice : modbusConfigurationAdapter.getSubEquips()) {
                    short subEquipSlaveId = (short) equipmentDevice.getSlaveId();
                    if (L.isModbusSlaveIdExists(subEquipSlaveId)) {
                        Toast.makeText(getActivity(), "Slave Id " + equipmentDevice.getSlaveId() + " already exists," +
                                " choose another slave id to proceed", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                Set<Short> slaveIds = new HashSet<>();
                for (EquipmentDevice subEquipmentDevice : modbusConfigurationAdapter.getSubEquips()) {
                    short subEquipSlaveId = (short) subEquipmentDevice.getSlaveId();
                    if(subEquipSlaveId == 0){
                        subEquipSlaveId = selectedEquipSlaveId;
                    }
                    if((slaveIds.contains(subEquipSlaveId)) && (selectedEquipSlaveId != subEquipSlaveId)){
                        Toast.makeText(getActivity(), "Make sure all sub equips have unique slave Id, if it is not " +
                                        "same as Parent",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    slaveIds.add(subEquipSlaveId);
                }

                saveConfig(equipmentDevice, selectedEquipSlaveId, recyclerModbusParamAdapter.modbusParam);
            }
            else if (!isEditConfig && L.isModbusSlaveIdExists(selectedEquipSlaveId)) {
                Toast.makeText(getActivity(), "Slave Id already exists, choose another slave id to proceed",
                        Toast.LENGTH_LONG).show();
            } else {
                saveConfig(equipmentDevice, selectedEquipSlaveId, recyclerModbusParamAdapter.modbusParam);
            }
        });
        toggleButtonChangeListener = (compoundButton, isChecked) -> {
            for(Parameter parameter : recyclerModbusParamAdapter.modbusParam) {
                parameter.setDisplayInUI(isChecked);
            }
            if(modbusConfigurationAdapter != null){
                for(EquipmentDevice equipmentDevice :modbusConfigurationAdapter.getSubEquips()){
                    if (Objects.nonNull(equipmentDevice.getRegisters())) {
                        for (Register registerTemp : equipmentDevice.getRegisters()) {
                            if (registerTemp.getParameters() != null) {
                                for (Parameter parameterTemp : registerTemp.getParameters()) {
                                    parameterTemp.setDisplayInUI(isChecked);
                                }
                            }
                        }
                    }
                }
                modbusConfigurationAdapter.updateView();
            }
            recyclerModbusParamAdapter.notifyDataSetChanged();
        };
        toggleSelectAllParams.setOnCheckedChangeListener(toggleButtonChangeListener);

        ivEditAddress.setOnClickListener(view1 -> {
            if (ivEditAddress.getDrawable().equals(getContext().getDrawable(R.drawable.ic_edit_accent))) {
                spAddress.setVisibility(View.GONE);
                editSlaveId.setVisibility(View.VISIBLE);
                //Replace icon with cancel.
                ivEditAddress.setImageDrawable(getContext().getDrawable(R.drawable.ic_refresh));
            } else {
                spAddress.setVisibility(View.VISIBLE);
                editSlaveId.setVisibility(View.GONE);
                ivEditAddress.setImageDrawable(getContext().getDrawable(R.drawable.ic_edit_accent));
            }
        });

        /**
         *Check the profile type selected under system device
         */
        if (profileType == ProfileType.MODBUS_BTU) {
            equipmentDeviceCollection = EquipsManager.getInstance().getAllBtuMeters();
        } else if (profileType == ProfileType.MODBUS_EMR) {
            equipmentDeviceCollection = EquipsManager.getInstance().getEnergyMeterSysEquipments();
        } else {
            equipmentDeviceCollection = EquipsManager.getInstance().getAllEquipments();
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (L.getProfile(curSelectedSlaveId) != null) {
            ProfileType profileType = L.getProfile(curSelectedSlaveId).getProfileType();
            switch (profileType) {
                case MODBUS_PAC:
                case MODBUS_RRS:
                case MODBUS_UPS30:
                case MODBUS_UPS400:
                case MODBUS_UPS80:
                case MODBUS_VRF:
                case MODBUS_WLD:
                case MODBUS_EM:
                case MODBUS_EMS:
                case MODBUS_ATS:
                case MODBUS_UPS150:
                case MODBUS_EMR:
                case MODBUS_BTU:
                case MODBUS_DEFAULT:
                case MODBUS_UPS40K:
                case MODBUS_UPSL:
                case MODBUS_UPSV:
                case MODBUS_UPSVL:
                case MODBUS_VAV_BACnet:
                    modbusProfile = (ModbusProfile) L.getProfile(curSelectedSlaveId);
                    if (modbusProfile != null) {
                        curSelectedSlaveId = modbusProfile.getSlaveId();
                        equipmentDevice = getFromBox(curSelectedSlaveId);
                        if (Objects.nonNull(equipmentDevice)) {
                            int index = 0;
                            ArrayAdapter equipmentAdapter = new ArrayAdapter(getActivity(), R.layout.spinner_item_custom, equipmentDeviceCollection);
                            spEquipmentType.setAdapter(equipmentAdapter);
                            Log.d("Modbus", "Edit config mb=" + curSelectedSlaveId + "," + equipmentDevice.toString() + "," + equipmentDevice.getName());
                            spEquipmentType.setEnabled(false);
                            for (int i = 0; i < equipmentDeviceCollection.size(); i++) {
                                if (equipmentDeviceCollection.get(i).getName().equals(equipmentDevice.getName()))
                                    spEquipmentType.setSelection(i, false);
                            }
                            isEditConfig = true;
                            updateUi(false);
                        }
                    }
                    break;
            }
        } else {
            modbusProfile = new ModbusProfile();
            ArrayAdapter equipmentAdapter = new ArrayAdapter(getActivity(), R.layout.spinner_item_custom, equipmentDeviceCollection);
            spEquipmentType.setAdapter(equipmentAdapter);
            spEquipmentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    equipmentDevice = (EquipmentDevice) parent.getSelectedItem();
                    updateUi(true);
                    toggleSelectAllParams.setChecked(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        CCUUiUtil.setSpinnerDropDownColor(spEquipmentType,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spAddress,getContext());
    }

    private void updateUi(boolean isNewConfig) {
        //If multiple slave address occurs
        ArrayList<Integer> slaveAddress = new ArrayList();
        for (int i = 1; i <= 247; i++)
            slaveAddress.add(i);

        //TODO Slave address can be empty, so we need to make it as editable entry and save it in equipmentDevices?? for edit config
        ArrayAdapter slaveAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, slaveAddress);
        slaveAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAddress.setAdapter(slaveAdapter);
        Log.d("Modbus","updateUi="+equipmentDevice.getName()+","+equipmentDevice.getSlaveId());

        if(Objects.nonNull(equipmentDevice.getSlaveId()) && equipmentDevice.getSlaveId() > 0) {
            curSelectedSlaveId = (short) (equipmentDevice.getSlaveId() -1);
            spAddress.setSelection(curSelectedSlaveId, false);
            spAddress.setEnabled(false);
        } else
            spAddress.setEnabled(true);
        spAddress.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                curSelectedSlaveId = (short) (position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        GridLayoutManager gridLayoutManager = null;
        LinearLayout.LayoutParams header1LayoutParams = (LinearLayout.LayoutParams) paramHeader1.getLayoutParams();
        LinearLayout.LayoutParams header2LayoutParams = (LinearLayout.LayoutParams) paramHeader2.getLayoutParams();
        List<Parameter> parameterList = new ArrayList<>();
        if (Objects.nonNull(equipmentDevice.getRegisters())) {
            for (Register registerTemp : equipmentDevice.getRegisters()) {
                if (registerTemp.getParameters() != null) {
                    for (Parameter parameterTemp : registerTemp.getParameters()) {
                        parameterTemp.setRegisterNumber(registerTemp.getRegisterNumber());
                        parameterTemp.setRegisterAddress(registerTemp.getRegisterAddress());
                        parameterTemp.setRegisterType(registerTemp.getRegisterType());
                        parameterList.add(parameterTemp);
                    }
                }
            }
        }

        if (parameterList != null && parameterList.size() > 3) {
            gridLayoutManager = new GridLayoutManager(getActivity(), 2);
            header1LayoutParams.weight = 1;
            header2LayoutParams.weight = 1;
            paramHeader1.setLayoutParams(header1LayoutParams);
            paramHeader2.setLayoutParams(header2LayoutParams);
            paramHeader1.setVisibility(View.VISIBLE);
            paramHeader2.setVisibility(View.VISIBLE);
        } else {
            gridLayoutManager = new GridLayoutManager(getActivity(), 1);
            header1LayoutParams.weight = 2;
            paramHeader1.setLayoutParams(header1LayoutParams);
            paramHeader1.setVisibility(View.VISIBLE);
            paramHeader2.setVisibility(View.GONE);
        }
        SelectAllParameters selectAllParameters = enable -> {
            if(!enable) {
                toggleSelectAllParams.setOnCheckedChangeListener(null);
                toggleSelectAllParams.setChecked(false);
                toggleSelectAllParams.setOnCheckedChangeListener(toggleButtonChangeListener);
            }
        };
        recyclerModbusParamAdapter = new RecyclerModbusParamAdapter(getActivity(), parameterList, isNewConfig,
                selectAllParameters);
        recyclerParams.setLayoutManager(gridLayoutManager);
        recyclerParams.setAdapter(recyclerModbusParamAdapter);
        recyclerParams.invalidate();

        List<EquipmentDevice> subEquipList = new ArrayList<>();
        if(null != equipmentDevice.getEquips()) {
            for (EquipmentDevice subEquipmentDevice : equipmentDevice.getEquips()) {
                subEquipList.add(subEquipmentDevice);
            }
            modbusConfigurationAdapter = new ModbusConfigurationAdapter(getActivity(), subEquipList, isNewConfig,
                    selectAllParameters);
            recyclerSubEquips.setLayoutManager(new LinearLayoutManager(this.getContext()));
            recyclerSubEquips.setAdapter(modbusConfigurationAdapter);
            recyclerSubEquips.invalidate();
        }
        if (subEquipList.size() > 0) {
            recyclerSubEquips.setVisibility(View.VISIBLE);
        } else {
            recyclerSubEquips.setVisibility(View.GONE);
        }
        if(enableSelectAllParameters(subEquipList)){
            toggleSelectAllParams.setOnCheckedChangeListener(null);
            toggleSelectAllParams.setChecked(true);
            toggleSelectAllParams.setOnCheckedChangeListener(toggleButtonChangeListener);
        }
    }
    private boolean enableSelectAllParameters(List<EquipmentDevice> subEquipList){
        List<EquipmentDevice> equipmentDeviceList = new ArrayList<>();
        equipmentDeviceList.add(equipmentDevice);
        equipmentDeviceList.addAll(subEquipList);
        boolean enable = true;
        for(EquipmentDevice device : equipmentDeviceList){
            if (Objects.nonNull(device.getRegisters())) {
                for (Register registerTemp : device.getRegisters()) {
                    if (registerTemp.getParameters() != null) {
                        for (Parameter parameterTemp : registerTemp.getParameters()) {
                            enable &= parameterTemp.isDisplayInUI();
                        }
                    }
                }
            }
        }
        return enable;
        }

    private void saveConfig(EquipmentDevice equipmentDev, short selectedSlaveId, List<Parameter> modbusParam) {

        new AsyncTask<String, Void, Void>() {

            @Override
            protected void onPreExecute() {
                setBtn.setEnabled(false);
                if (ProgressDialogUtils.isDialogShowing())
                    ProgressDialogUtils.hideProgressDialog();
                ProgressDialogUtils.showProgressDialog(getActivity(), message);
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(final String... params) {
                CCUHsApi.getInstance().resetCcuReady();
                addModbusProfile(equipmentDev, selectedSlaveId, modbusParam);
                L.saveCCUState();
                CCUHsApi.getInstance().setCcuReady();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                ProgressDialogUtils.hideProgressDialog();
                FragmentModbusConfiguration.this.closeAllBaseDialogFragments();
                getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private void addModbusProfile(EquipmentDevice equipmentDev, short selectedSlaveId, List<Parameter> modbusParam){
        boolean isNewDevice = L.getProfile(selectedSlaveId) == null;
        List<EquipmentDevice> subEquipmentDevices = new ArrayList<>();
        if(null != equipmentDevice.getEquips()) {
            for (EquipmentDevice subEquipmentDevice : modbusConfigurationAdapter.getSubEquips()) {
                subEquipmentDevices.add(subEquipmentDevice);
            }
        }
        String equipRef = setUpsModbusProfile(equipmentDev, selectedSlaveId, modbusParam, subEquipmentDevices);
        saveToBox(zoneRef, equipRef, equipmentDev, selectedSlaveId, isNewDevice, floorRef);
    }

    private HashMap<Object, Object> getModbusEquipMap(Short slaveId){
        return CCUHsApi.getInstance().readEntity("equip and modbus and not equipRef and group == \"" + slaveId + "\"");
    }

    private String setUpsModbusProfile(EquipmentDevice equipmentDev, short selectedSlaveId, List<Parameter> modbusParam,
                                       List<EquipmentDevice> subEquipmentDevices) {
        String equipType = equipmentDev.getEquipType();
        Log.i("equipType", "setUpsModbusProfile: "+equipType);
        ModbusEquipTypes curEquipTypeSelected = ModbusEquipTypes.getEnum(equipType);
        String equipRef;
        equipmentDev.setSlaveId(selectedSlaveId);

        if (!getModbusEquipMap(selectedSlaveId).isEmpty()) {
            equipRef = updateModbusProfile(selectedSlaveId, equipmentDev, modbusParam);
        }
        else {
            switch (curEquipTypeSelected) {
                case UPS30K:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_UPS30, subEquipmentDevices);
                    break;
                case UPS80K:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_UPS80, subEquipmentDevices);
                    break;
                case UPS400K:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_UPS400, subEquipmentDevices);
                    break;
                case PAC:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_PAC, subEquipmentDevices);
                    break;
                case RRS:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_RRS, subEquipmentDevices);
                    break;
                case WLD:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_WLD, subEquipmentDevices);
                    break;
                case EM:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_EM, subEquipmentDevices);
                    break;
                case EMS:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_EMS, subEquipmentDevices);
                    break;
                case ATS:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_ATS, subEquipmentDevices);
                    break;
                case VRF:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_VRF, subEquipmentDevices);
                    break;
                case UPS150K:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_UPS150, subEquipmentDevices);
                    break;
                case EMR:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_EMR, subEquipmentDevices);
                    break;
                case BTU:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_BTU, subEquipmentDevices);
                    break;
                case UPS40K:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_UPS40K, subEquipmentDevices);
                    break;
                case UPSL:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_UPSL, subEquipmentDevices);
                    break;
                case UPSV:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_UPSV, subEquipmentDevices);
                    break;
                case UPSVL:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_UPSVL, subEquipmentDevices);
                    break;
                case VAV_BACnet:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_VAV_BACnet, subEquipmentDevices);
                    break;
                case MODBUS_DEFAULT:
                    modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam,
                            ProfileType.MODBUS_DEFAULT, subEquipmentDevices);
                    break;
            }
            L.ccu().zoneProfiles.add(modbusProfile);
            equipRef = modbusProfile.getEquip().getId();
            L.saveCCUState();
        }
        CcuLog.d(L.TAG_CCU_UI, "Set modbus Config: MB Profiles - " + L.ccu().zoneProfiles.size() + "," + equipRef + "," + selectedSlaveId);
        return equipRef;
    }

    public String updateModbusProfile(int slave_id, EquipmentDevice equipmentDev, List<Parameter> modbusParam) {
        modbusProfile.updateMbEquip((short) slave_id, floorRef, zoneRef, equipmentDev, modbusParam);
        L.ccu().zoneProfiles.add(modbusProfile);
        return modbusProfile.getEquip().getId();
    }

    public void saveToBox(String zoneRef, String equipRef, EquipmentDevice modbusDevice, int slaveId, boolean isNewDevice, String floorRef) {
        if (isNewDevice) {
            modbusDevice.setId(0);
            modbusDevice.setPaired(true);
        }
        modbusDevice.setDeviceEquipRef(equipRef);
        modbusDevice.setZoneRef(zoneRef);
        modbusDevice.setFloorRef(floorRef);
        modbusDevice.setSlaveId(slaveId);
        ModbusEquipsInfo modbusEquipsInfo = new ModbusEquipsInfo();
        modbusEquipsInfo.equipmentDevices = modbusDevice;
        modbusEquipsInfo.zoneRef = zoneRef;
        modbusEquipsInfo.equipRef = equipRef;
        EquipsManager.getInstance().saveProfile(modbusDevice);
    }

    public EquipmentDevice getFromBox(int slaveId) {
        //return equipsManager.fetchProfile(equipRef);
        return EquipsManager.getInstance().fetchProfileBySlaveId(slaveId);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = 1200;
            int height = 720;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @Override
    public String getIdString() {
        return ID;
    }

    void initConfiguration(){
        /**
         * If Profile type is ENergy meter then only enable the energy distribution details
         */
        if (profileType == ProfileType.MODBUS_EMR) {
            textTitleFragment.setText(getString(R.string.label_modbus_energy_meter));
            floorList = HSUtil.getFloors();
            if(floorList.size()>0) {
                Collections.sort(floorList, floorComparator);
                floorViewheader.setVisibility(View.VISIBLE);
                floorListView.setVisibility(View.VISIBLE);
                this.energyDistributionAdapter = new EnergyDistributionAdapter(floorList, getContext(), this);
                floorListView.setLayoutManager(new LinearLayoutManager(getContext()));
                floorListView.setAdapter(energyDistributionAdapter);
            }
        }
            /* * If Profile type is BTU meter then only enable the energy distribution details
             */
            if (profileType == ProfileType.MODBUS_BTU) {
                textTitleFragment.setText(getString(R.string.label_modbus_btu_meter));
                floorList = HSUtil.getFloors();
                if(floorList.size()>0) {
                    Collections.sort(floorList, floorComparator);
                    floorViewheader.setVisibility(View.VISIBLE);
                    floorListView.setVisibility(View.VISIBLE);
                    this.energyDistributionAdapter = new EnergyDistributionAdapter(floorList, getContext(), this);
                    floorListView.setLayoutManager(new LinearLayoutManager(getContext()));
                    floorListView.setAdapter(energyDistributionAdapter);
                }
            }
        }
    /**
     * Validate the energy distribution value for each floor
     * @param energyDistribution
     */
    public void validateEnergyDistributionValue(Map<Integer, Integer> energyDistribution) {
        int total = 0;
        for (Integer key : energyDistribution.keySet()) {
            total += energyDistribution.get(key);
        }

        boolean isValue100Percent = (total == 100);
        if(isValue100Percent)
        {
            setBtn.setVisibility(View.VISIBLE);
        }
        else
        {
            Toast.makeText(getContext(), getContext().getString(R.string.energy_distribution_validation_error), Toast.LENGTH_LONG).show();
            setBtn.setVisibility(View.INVISIBLE);
        }
    }

}
