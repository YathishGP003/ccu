package a75f.io.renatus;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import a75f.io.api.haystack.Floor;

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
        return new EnergyDistributorView(LayoutInflater.from(mContext).
                inflate(R.layout.energy_pro_dis_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EnergyDistributorView holder, int position) {
        holder.floorName.setText(this.floorList.get(position).getDisplayName() + " : ");
    }

    @Override
    public int getItemCount() {
        return floorList.size();
    }

    class EnergyDistributorView extends RecyclerView.ViewHolder {

        TextView floorName;
        Spinner energyDistributeValue;

        public EnergyDistributorView(@NonNull View itemView) {
            super(itemView);
            floorName = itemView.findViewById(R.id.floor_name);
            energyDistributeValue = itemView.findViewById(R.id.distribution_value);

        }
    }
}
