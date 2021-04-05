package a75f.io.renatus;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.Bindable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import a75f.io.api.haystack.Floor;
import a75f.io.renatus.databinding.EnergyProDisItemBinding;

public class EnergyDistributionAdapter extends RecyclerView.Adapter<EnergyDistributionAdapter.EnergyDistributorView> {

    ArrayList<Floor> floorList;
    Context mContext;
    List<Integer> energyDistribution;
    public EnergyDistributionAdapter(ArrayList<Floor> floorList, Context mContext) {
        this.floorList = floorList;
        this.mContext = mContext;
        energyDistribution = new ArrayList<>(floorList.size());
    }

    @NonNull
    @Override
    public EnergyDistributorView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        EnergyProDisItemBinding item = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.energy_pro_dis_item, parent, false);
        return new EnergyDistributorView(item);
    }

    @Override
    public void onBindViewHolder(@NonNull EnergyDistributorView holder, int position) {
        MyEventHandler myEventHandler = new MyEventHandler(mContext);
        holder.bind(this.floorList.get(position),myEventHandler);

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

        public void bind(Object obj,MyEventHandler myEventHandler) {
            this.energyProDisItemBinding.setVariable(BR.floor, obj);
            this.energyProDisItemBinding.setVariable(BR.eventListener,myEventHandler);
            this.energyProDisItemBinding.executePendingBindings();

        }

    }

    private void  updateEnergyValue(String selectedValue){
        Toast.makeText(mContext, selectedValue, Toast.LENGTH_SHORT).show();
    }

    public class  MyEventHandler{

        Context context;
        public MyEventHandler(Context context) {
            this.context=context;
        }

        public void customClickHandler(View view){
            Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show();
        }

        public void onEnergyValueChanged(View view){
            updateEnergyValue(((Spinner)view).getSelectedItem().toString());
        }
    }


}
