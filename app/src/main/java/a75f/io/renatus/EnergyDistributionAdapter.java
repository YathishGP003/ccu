package a75f.io.renatus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import a75f.io.api.haystack.Floor;
import a75f.io.renatus.databinding.EnergyProDisItemBinding;

public class EnergyDistributionAdapter extends RecyclerView.Adapter<EnergyDistributionAdapter.EnergyDistributorView> {

    ArrayList<Floor> floorList;
    Context mContext;

    public EnergyDistributionAdapter(ArrayList<Floor> floorList, Context mContext) {
        this.floorList = floorList;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public EnergyDistributorView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        EnergyProDisItemBinding item = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.energy_pro_dis_item, parent, false);
        return new EnergyDistributorView(item);
    }

    @Override
    public void onBindViewHolder(@NonNull EnergyDistributorView holder, int position) {
        holder.bind(this.floorList.get(position));
    }

    @Override
    public int getItemCount() {
        return floorList.size();
    }

    class EnergyDistributorView extends RecyclerView.ViewHolder {

        EnergyProDisItemBinding energyProDisItemBinding;

        public EnergyDistributorView(EnergyProDisItemBinding energyProDisItemBinding) {
            super(energyProDisItemBinding.getRoot());
            this.energyProDisItemBinding = energyProDisItemBinding;
        }

        public void bind(Object obj) {
            this.energyProDisItemBinding.setVariable(BR.floor, obj);
            this.energyProDisItemBinding.executePendingBindings();
        }
    }
}
