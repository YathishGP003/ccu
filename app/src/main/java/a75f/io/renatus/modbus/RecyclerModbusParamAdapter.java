package a75f.io.renatus.modbus;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.renatus.R;

class RecyclerModbusParamAdapter extends RecyclerView.Adapter<RecyclerModbusParamAdapter.ViewHolder> {

    Context context;
    List<Parameter> modbusParam;
    List<Register> registerList;
    boolean isNewConfig;
    public RecyclerModbusParamAdapter(Context context, List<Parameter> modbusParam, boolean isNewConfig) {
        this.context = context;
        this.modbusParam = modbusParam;
        this.isNewConfig = isNewConfig;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_modbus_param, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.textParam.setText(modbusParam.get(position).getName());
        viewHolder.toggleButton.setChecked(modbusParam.get(position).isDisplayInUI());
        viewHolder.toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            modbusParam.get(position).setDisplayInUI(isChecked);
        });
    }

    @Override
    public int getItemCount() {
        if (modbusParam != null) {
            return modbusParam.size();
        } else {
            return 0;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textParam;
        public ToggleButton toggleButton;
        public View layout;

        public ViewHolder(View v) {
            super(v);
            layout = v;
            textParam = v.findViewById(R.id.textParam);
            toggleButton = v.findViewById(R.id.toggleButton);
        }
    }
}
