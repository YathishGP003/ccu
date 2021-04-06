package a75f.io.renatus;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import a75f.io.api.haystack.Floor;
import a75f.io.renatus.modbus.FragmentModbusConfiguration;

public class EnergyDistributionAdapter extends RecyclerView.Adapter<EnergyDistributionAdapter.EnergyDistributorView> {

    ArrayList<Floor> floorList;
    Context mContext;
    Map<Integer, Integer> energyDistribution;
    Object callBackReff;

    public EnergyDistributionAdapter(ArrayList<Floor> floorList, Context mContext,Object callBackReff) {
        this.floorList = floorList;
        this.mContext = mContext;
        energyDistribution = new HashMap<>(floorList.size());
        this.callBackReff = callBackReff;
    }

    @NonNull
    @Override
    public EnergyDistributorView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View energyProDisItem = inflater.inflate(R.layout.energy_pro_dis_item, parent, false);
        EnergyDistributorView EnergyDistributorView = new EnergyDistributorView(energyProDisItem);
        return EnergyDistributorView;
    }

    @Override
    public void onBindViewHolder(@NonNull EnergyDistributorView holder, int position) {

        holder.floorName.setText(floorList.get(position).getDisplayName());

        if(callBackReff!=null) {
            holder.distributionValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int dropDownPosition, long id) {
                    int value = Integer.parseInt(holder.distributionValue.getSelectedItem().toString().split("%")[0]);
                    energyDistribution.put(position, value);
                    ((FragmentModbusConfiguration) callBackReff).validateEnergyDistributionValue(energyDistribution);

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return floorList.size();
    }

    class EnergyDistributorView extends RecyclerView.ViewHolder {

        Spinner distributionValue;
        TextView floorName;

        public EnergyDistributorView(View view) {
            super(view);
            distributionValue = view.findViewById(R.id.distribution_value);
            floorName = view.findViewById(R.id.floor_name);
        }
    }

}