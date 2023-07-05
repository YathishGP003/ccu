package a75f.io.renatus.modbus;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.renatus.R;


public class ModbusConfigurationAdapter extends RecyclerView.Adapter<ModbusConfigurationAdapter.ViewHolder>{
    private Context context;
    private List<EquipmentDevice> subEquips;
    private boolean isConfigured;
    private short curSelectedSlaveId;
    private RecyclerModbusParamAdapter recyclerModbusParamAdapter;
    private SelectAllParameters selectAllParameters;
    private List<RecyclerModbusParamAdapter> recyclerModbusParamAdapterList;

    public List<EquipmentDevice> getSubEquips() {
        return subEquips;
    }

    public void updateView(){
        for(RecyclerModbusParamAdapter recyclerModbusParamAdapter : recyclerModbusParamAdapterList){
            recyclerModbusParamAdapter.notifyDataSetChanged();
        }
    }

    public ModbusConfigurationAdapter(Context context, List<EquipmentDevice> subEquips, boolean isConfigured,
                                      SelectAllParameters selectAllParameters) {
        this.context = context;
        this.subEquips =  subEquips;
        this.isConfigured = isConfigured;
        this.selectAllParameters = selectAllParameters;
        recyclerModbusParamAdapterList = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_modbus_configuration, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.equipmentTypeVal.setText(subEquips.get(position).getName());
        populateSlaveIdForSubEquips(viewHolder, position);
        populateSubEquipParameters(viewHolder, position);
    }

    private void populateSubEquipParameters(@NonNull ViewHolder viewHolder, int position) {
        GridLayoutManager gridLayoutManager = null;
        LinearLayout.LayoutParams header1LayoutParams =
                (LinearLayout.LayoutParams) viewHolder.paramHeader1.getLayoutParams();
        LinearLayout.LayoutParams header2LayoutParams =
                (LinearLayout.LayoutParams) viewHolder.paramHeader2.getLayoutParams();
        List<Parameter> parameterList = new ArrayList<>();
        if (Objects.nonNull(subEquips.get(position).getRegisters())) {
            for (Register registerTemp : subEquips.get(position).getRegisters()) {
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
            gridLayoutManager = new GridLayoutManager(context, 2);
            header1LayoutParams.weight = 1;
            header2LayoutParams.weight = 1;
            viewHolder.paramHeader1.setLayoutParams(header1LayoutParams);
            viewHolder.paramHeader2.setLayoutParams(header2LayoutParams);
            viewHolder.paramHeader1.setVisibility(View.VISIBLE);
            viewHolder.paramHeader2.setVisibility(View.VISIBLE);
        } else {
            gridLayoutManager = new GridLayoutManager(context, 1);
            header1LayoutParams.weight = 2;
            viewHolder.paramHeader1.setLayoutParams(header1LayoutParams);
            viewHolder.paramHeader1.setVisibility(View.VISIBLE);
            viewHolder.paramHeader2.setVisibility(View.GONE);
        }
        recyclerModbusParamAdapter = new RecyclerModbusParamAdapter(context, parameterList, isConfigured,
                selectAllParameters);
        recyclerModbusParamAdapterList.add(recyclerModbusParamAdapter);
        viewHolder.recyclerSubEquipParams.setLayoutManager(gridLayoutManager);
        viewHolder.recyclerSubEquipParams.setAdapter(recyclerModbusParamAdapter);
        viewHolder.recyclerSubEquipParams.invalidate();
    }

    private void populateSlaveIdForSubEquips(ViewHolder viewHolder, int position){
        ArrayList<String> slaveAddress = new ArrayList();
        slaveAddress.add("Same as Parent");
        for (int i = 1; i <= 247; i++)
            slaveAddress.add(Integer.toString(i));

        //TODO Slave address can be empty, so we need to make it as editable entry and save it in equipmentDevices?? for edit config
        ArrayAdapter slaveAdapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, slaveAddress);
        slaveAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        viewHolder.spAddress.setAdapter(slaveAdapter);
        Log.d("Modbus","updateUi="+subEquips.get(position).getName()+","+subEquips.get(position).getSlaveId());

        if(Objects.nonNull(subEquips.get(position).getSlaveId()) && subEquips.get(position).getSlaveId() > 0) {
            curSelectedSlaveId = (short) (subEquips.get(position).getSlaveId());
            viewHolder.spAddress.setSelection(curSelectedSlaveId, false);
            viewHolder.spAddress.setEnabled(isConfigured);
        }
        viewHolder.spAddress.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                curSelectedSlaveId = (short) (pos);
                subEquips.get(position).setSlaveId(curSelectedSlaveId);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    @Override
    public int getItemCount() {
        return (subEquips != null) ? subEquips.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView equipmentTypeVal;
        private AppCompatSpinner spAddress;
        private EditText editSlaveId;
        private RecyclerView recyclerSubEquipParams;
        private LinearLayout paramHeader1;
        private LinearLayout paramHeader2;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            equipmentTypeVal = itemView.findViewById(R.id.equipmentTypeVal);
            spAddress = itemView.findViewById(R.id.spAddress);
            editSlaveId = itemView.findViewById(R.id.editSlaveId);
            paramHeader1 = itemView.findViewById(R.id.paramHeader1);
            paramHeader2 = itemView.findViewById(R.id.paramHeader2);
            recyclerSubEquipParams = itemView.findViewById(R.id.recyclerSubEquipParams);
        }
    }
}
