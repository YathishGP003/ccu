package a75f.io.renatus.modbus;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.ModbusEquipsInfo;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.modbus.ModbusEquipTypes;
import a75f.io.logic.bo.building.modbus.ModbusProfile;
import a75f.io.modbusbox.EquipsManager;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.FloorPlanFragment;
import a75f.io.renatus.R;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FragmentModbusConfiguration extends BaseDialogFragment {

    public static final String ID = FragmentModbusConfiguration.class.getSimpleName();

    private short curSelectedSlaveId;
    String floorRef;
    String zoneRef;

    ModbusProfile modbusProfile;
    EquipmentDevice equipmentDevice;
    @BindView(R.id.spEquipmentType)
    AppCompatSpinner spEquipmentType;

    @BindView(R.id.spAddress)
    AppCompatSpinner spAddress;

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

    List<EquipmentDevice> equipmentDeviceCollection;
    RecyclerModbusParamAdapter recyclerModbusParamAdapter;
    boolean isEditConfig = false;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        curSelectedSlaveId = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        zoneRef = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        floorRef = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);

        View view = inflater.inflate(R.layout.fragment_modbus_config, container, false);
        ButterKnife.bind(this, view);

        setBtn.setOnClickListener(view1 -> {
            if(!isEditConfig && L.isModbusSlaveIdExists((short)(spAddress.getSelectedItemPosition() +1))){
                Toast.makeText(getActivity(),"Slave Id already exists, choose another slave id to proceed", Toast.LENGTH_LONG).show();
            } else
                saveConfig();
        });

        ivEditAddress.setOnClickListener(view1 -> {
            if(ivEditAddress.getDrawable().equals(getContext().getDrawable(R.drawable.ic_edit_accent))) {
                spAddress.setVisibility(View.GONE);
                editSlaveId.setVisibility(View.VISIBLE);
                //Replace icon with cancel.
                ivEditAddress.setImageDrawable(getContext().getDrawable(R.drawable.ic_refresh));
            }else{
                spAddress.setVisibility(View.VISIBLE);
                editSlaveId.setVisibility(View.GONE);
                ivEditAddress.setImageDrawable(getContext().getDrawable(R.drawable.ic_edit_accent));
            }
        });
        equipmentDeviceCollection  = EquipsManager.getInstance().getAllEquipments();
        return view;
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if(L.getProfile(curSelectedSlaveId) != null) {
            ProfileType profileType = L.getProfile(curSelectedSlaveId).getProfileType();
            switch (profileType) {
                case MODBUS_PAC:
                case MODBUS_RRS:
                case MODBUS_UPS:
                case MODBUS_WLD:
                case MODBUS_EM:
                    modbusProfile = (ModbusProfile) L.getProfile(curSelectedSlaveId);
                    if(modbusProfile != null){
                        curSelectedSlaveId = modbusProfile.getSlaveId();
                        equipmentDevice = getFromBox(curSelectedSlaveId);
                        if(Objects.nonNull(equipmentDevice)) {
                            int index = 0;
                            ArrayAdapter equipmentAdapter = new ArrayAdapter(getActivity(), R.layout.spinner_item_custom, equipmentDeviceCollection);
                            spEquipmentType.setAdapter(equipmentAdapter);
                            Log.d("Modbus","Edit config mb="+curSelectedSlaveId+","+equipmentDevice.toString()+","+equipmentDevice.getName());
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
        }else {
            modbusProfile = new ModbusProfile();
            ArrayAdapter equipmentAdapter = new ArrayAdapter(getActivity(), R.layout.spinner_item_custom, equipmentDeviceCollection);
            spEquipmentType.setAdapter(equipmentAdapter);
            spEquipmentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    equipmentDevice = (EquipmentDevice) parent.getSelectedItem();
                    Log.i("MODBUS_UI", "equipmentDevice:" + equipmentDevice);
                    updateUi(true);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

    }
    private void updateUi(boolean isNewConfig){

        //If multiple slave address occurs
        ArrayList<Integer> slaveAddress = new ArrayList();
        for(int i =1; i <= 247; i++)
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
        }else
            spAddress.setEnabled(true);
        spAddress.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                curSelectedSlaveId = (short) (position +1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Log.i("MODBUS_UI", "Registers:" + equipmentDevice.getRegisters());
        GridLayoutManager gridLayoutManager = null;
        LinearLayout.LayoutParams header1LayoutParams = (LinearLayout.LayoutParams) paramHeader1.getLayoutParams();
        LinearLayout.LayoutParams header2LayoutParams = (LinearLayout.LayoutParams) paramHeader2.getLayoutParams();
        List<Parameter> parameterList = new ArrayList<>();
        if(Objects.nonNull(equipmentDevice.getRegisters())) {
            for (Register registerTemp : equipmentDevice.getRegisters()) {
                Log.i("MODBUS_UI", "Registers:" + registerTemp.getRegisterAddress());
                Log.i("MODBUS_UI", "Parameters:" + registerTemp.getParameters().get(0).getName());
                Log.i("MODBUS_UI", "size:" + registerTemp.getParameters().size());
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
        }else {
            gridLayoutManager = new GridLayoutManager(getActivity(),1);
            header1LayoutParams.weight = 2;
            paramHeader1.setLayoutParams(header1LayoutParams);
            paramHeader1.setVisibility(View.VISIBLE);
            paramHeader2.setVisibility(View.GONE);
        }
        recyclerModbusParamAdapter = new RecyclerModbusParamAdapter(getActivity(), parameterList, isNewConfig);
        recyclerParams.setLayoutManager(gridLayoutManager);
        recyclerParams.setAdapter(recyclerModbusParamAdapter);
        recyclerParams.invalidate();
    }
    private void saveConfig(){

        setBtn.setEnabled(false);
        if(ProgressDialogUtils.isDialogShowing())
            ProgressDialogUtils.hideProgressDialog();
        ProgressDialogUtils.showProgressDialog(getActivity(),"Saving Modbus Configuration");
        new Thread(() -> {
            setUpsModbusProfile();
            L.saveCCUState();
        }).start();

        new Handler().postDelayed(() -> {
            ProgressDialogUtils.hideProgressDialog();
            FragmentModbusConfiguration.this.closeAllBaseDialogFragments();
            getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
        },12000);
    }
    private void setUpsModbusProfile() {

        Log.i("ModbusUI", "Data:" + recyclerModbusParamAdapter.modbusParam);
        String equipType = equipmentDevice.getEquipType();
        ModbusEquipTypes curEquipTypeSelected = ModbusEquipTypes.valueOf(equipType);
        String equipRef = null;
        curSelectedSlaveId = (short)(spAddress.getSelectedItemPosition() + 1) ;
        if(spAddress.getVisibility() == View.GONE){
            curSelectedSlaveId = (short) Integer.parseInt(editSlaveId.getText().toString());
        }
     //   equipmentDevice.getRegisters().get(0).setParameters(recyclerModbusParamAdapter.modbusParam);
        equipmentDevice.setSlaveId(curSelectedSlaveId);
        boolean isNewDevice = L.getProfile((short) curSelectedSlaveId) == null;

        switch (curEquipTypeSelected) {
            case UPS:
                if(L.getProfile((short)curSelectedSlaveId) == null) {
                    modbusProfile.addMbEquip((short) curSelectedSlaveId, floorRef, zoneRef, equipmentDevice, recyclerModbusParamAdapter.modbusParam,ProfileType.MODBUS_UPS);
                    L.ccu().zoneProfiles.add(modbusProfile);
                    equipRef = modbusProfile.getEquip().getId();
                }else
                    equipRef = updateModbusProfile(curSelectedSlaveId);
                CcuLog.d(L.TAG_CCU_UI, "Set modbus Config: MB Profiles - " + L.ccu().zoneProfiles.size()+","+equipRef+","+curSelectedSlaveId);
                break;
            case PAC:
                if(L.getProfile((short)curSelectedSlaveId) == null) {
                    modbusProfile.addMbEquip((short) curSelectedSlaveId, floorRef, zoneRef, equipmentDevice, recyclerModbusParamAdapter.modbusParam,ProfileType.MODBUS_PAC);
                    L.ccu().zoneProfiles.add(modbusProfile);
                    equipRef = modbusProfile.getEquip().getId();
                }else
                    equipRef = updateModbusProfile(curSelectedSlaveId);
                CcuLog.d(L.TAG_CCU_UI, "Set modbus Config: MB Profiles - " + L.ccu().zoneProfiles.size()+","+equipRef+","+curSelectedSlaveId);
                break;
            case RRS:
                if(L.getProfile((short)curSelectedSlaveId) == null) {
                    modbusProfile.addMbEquip((short) curSelectedSlaveId, floorRef, zoneRef, equipmentDevice, recyclerModbusParamAdapter.modbusParam,ProfileType.MODBUS_RRS);
                    L.ccu().zoneProfiles.add(modbusProfile);
                    equipRef = modbusProfile.getEquip().getId();
                }else
                    equipRef = updateModbusProfile(curSelectedSlaveId);
                CcuLog.d(L.TAG_CCU_UI, "Set modbus Config: MB Profiles - " + L.ccu().zoneProfiles.size()+","+equipRef+","+curSelectedSlaveId);
                break;
            case WLD:
                CcuLog.d(L.TAG_CCU_UI, "Set modbus Config: MB Profiles - " + L.ccu().zoneProfiles.size()+","+L.getProfile((short)curSelectedSlaveId)+","+curSelectedSlaveId);
                if(L.getProfile((short)curSelectedSlaveId) == null) {
                    modbusProfile.addMbEquip((short) curSelectedSlaveId, floorRef, zoneRef, equipmentDevice, recyclerModbusParamAdapter.modbusParam,ProfileType.MODBUS_WLD);
                    L.ccu().zoneProfiles.add(modbusProfile);
                    equipRef = modbusProfile.getEquip().getId();
                }else
                    equipRef = updateModbusProfile(curSelectedSlaveId);
                break;
            case EM:
                CcuLog.d(L.TAG_CCU_UI, "Set modbus Config: MB Profiles - " + L.ccu().zoneProfiles.size() + "," + L.getProfile((short) curSelectedSlaveId) + "," + curSelectedSlaveId);
                if (L.getProfile((short) curSelectedSlaveId) == null) {
                    modbusProfile.addMbEquip((short) curSelectedSlaveId, floorRef, zoneRef, equipmentDevice, recyclerModbusParamAdapter.modbusParam, ProfileType.MODBUS_EM);
                    L.ccu().zoneProfiles.add(modbusProfile);
                    equipRef = modbusProfile.getEquip().getId();
                } else
                    equipRef = updateModbusProfile(curSelectedSlaveId);
                break;
        }
        saveToBox(zoneRef,equipRef,equipmentDevice,curSelectedSlaveId, isNewDevice,floorRef);
    }
    public String updateModbusProfile(int slave_id){
        modbusProfile.updateMbEquip((short)slave_id,floorRef,zoneRef, equipmentDevice,recyclerModbusParamAdapter.modbusParam);
        L.ccu().zoneProfiles.add(modbusProfile);
        return modbusProfile.getEquip().getId();
    }

    public void saveToBox(String zoneRef, String equipRef, EquipmentDevice modbusDevice, int slaveId, boolean isNewDevice, String floorRef) {
        if (isNewDevice){
            modbusDevice.setId(0);
            modbusDevice.setPaired(true);
        }
        modbusDevice.setEquipRef(equipRef);
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

    /*public List<EquipmentDevice> loadJSONFromAsset() {
        List<EquipmentDevice> equipmentDevicesList = new ArrayList<EquipmentDevice>();
        try {
            String[] fileList;
            fileList = getActivity().getAssets().list("modbus");
            for(String filename: fileList) {
                InputStream is = getActivity().getAssets().open("modbus/"+filename);
                int size = is.available();
                if(size > 0) {
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    EquipmentDevice equipmentDevice = gson.fromJson(new String(buffer, "UTF-8"), EquipmentDevice.class);
                    List<ModbusEquipsInfo> equipsQueryBuilder = modbusEquipsBox.query()
                            .equal(ModbusEquipsInfo_.modbusEquipId, equipmentDevice.getModbusEquipIdId()).build().find();
                    if(equipsQueryBuilder.size() > 0) {
                        for(ModbusEquipsInfo info : equipsQueryBuilder) {
                            Log.d("Modbus", "loadjson=" + equipmentDevice.getModbusEquipIdId()+","+info.getModbusEquipId());
                            if ((info.getModbusEquipId() == null) || !info.getModbusEquipId().equals(equipmentDevice.getModbusEquipIdId())) {
                                modbusEquipsBox.put(new ModbusEquipsInfo(equipmentDevice));
                            }
                        }
                    }else {
                        Log.d("Modbus", "loadjson22 new="+equipmentDevice.getName());
                        modbusEquipsBox.put(new ModbusEquipsInfo(equipmentDevice.getModbusEquipIdId(), equipmentDevice));
                    }
                    equipmentDevicesList.add(equipmentDevice);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return equipmentDevicesList;
        }
        return equipmentDevicesList;
    }*/
}
