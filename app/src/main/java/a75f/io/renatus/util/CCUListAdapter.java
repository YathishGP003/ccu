package a75f.io.renatus.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import a75f.io.logic.ccu.restore.CCU;
import a75f.io.logic.ccu.restore.RestoreCCU;
import a75f.io.renatus.R;
import a75f.io.renatus.registration.CCUSelect;

public class CCUListAdapter extends RecyclerView.Adapter<CCUListAdapter.CCUView> {

    private List<CCU> ccuList;
    private Context context;
    private CCUSelect callBack;
    public CCUListAdapter(List<CCU> ccuList, Context context, CCUSelect callBack){
        this.ccuList = ccuList;
        this.context = context;
        this.callBack = callBack;
    }

    @NonNull
    @Override
    public CCUView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cuu_item, parent, false);
        return new CCUView(itemView);
    }

    private boolean isCCUReplacable(int position){
        return ((CCUUiUtil.getCurrentCCUVersion().contains(ccuList.get(position).getVersion())) &&
                (!ccuList.get(position).isOnline()));
    }
    @Override
    public void onBindViewHolder(@NonNull CCUView holder, int position) {
        holder.name.setText(ccuList.get(position).getName());
        holder.lastUpdated.setText("Last Updated On "+ ccuList.get(position).getLastUpdated());
        holder.version.setText("CCU Version "+ ccuList.get(position).getVersion());
        holder.itemView.setEnabled(false);
        holder.status.setText("ONLINE");
        if(!ccuList.get(position).isOnline()){
            holder.status.setText("OFFLINE");
        }
        if(isCCUReplacable(position)){
           holder.isOffline.setBackgroundColor(CCUUiUtil.getPrimaryThemeColor(context));
           holder.itemView.setEnabled(true);
           holder.name.setTextColor(Color.BLACK);
           holder.lastUpdated.setTextColor(Color.BLACK);
           holder.version.setTextColor(Color.BLACK);
           holder.status.setTextColor(Color.BLACK);
       }

        holder.itemView.setOnClickListener(view -> callBack.onCCUSelect(ccuList.get(position)));
    }

    @Override
    public int getItemCount() {
        return this.ccuList.size();
    }

    class CCUView extends RecyclerView.ViewHolder {
        TextView name;
        TextView lastUpdated;
        TextView version;
        TextView status;
        View isOffline;

        public CCUView(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.ccu_name);
            isOffline = itemView.findViewById(R.id.isOffline);
            lastUpdated = itemView.findViewById(R.id.last_updated);
            status = itemView.findViewById(R.id.ccu_status);
            version = itemView.findViewById(R.id.ccu_version);
        }
    }
}
