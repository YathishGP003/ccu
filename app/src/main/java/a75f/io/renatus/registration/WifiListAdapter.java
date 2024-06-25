package a75f.io.renatus.registration;

import android.content.Context;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;


public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.WifiViewHolder> {

    private List<String> wifiList;
    private Context mContext;
    private View.OnClickListener mOnClickListener;
    private ItemClickListener onItemClickListener;

    public class WifiViewHolder extends RecyclerView.ViewHolder {
        public TextView textWifiNw;
        public TextView textisConnected;
        public ImageView imageWifi;
        public ImageView imageSecurity;

        public WifiViewHolder(View itemView) {
            super(itemView);
            textWifiNw = itemView.findViewById(R.id.textWifiNw);
            textisConnected = itemView.findViewById(R.id.textisConnected);
            imageWifi = itemView.findViewById(R.id.imageWifi);
            imageSecurity = itemView.findViewById(R.id.imageSecurity);
            itemView.setOnClickListener(view -> onItemClickListener.onItemClicked(itemView, getAdapterPosition()));
        }

    }



    public WifiListAdapter(Context context, ItemClickListener clickListener) {
        this.onItemClickListener = clickListener;
        this.mContext = context;
    }

    public void updateData(List<String> wifiList) {
        this.wifiList = wifiList;
        this.notifyDataSetChanged();
    }

    interface ItemClickListener{
        void onItemClicked(View view, int position);
    }

    @Override
    public WifiViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_wifiitem, parent, false);
        return new WifiViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(WifiViewHolder holder, int position) {
        String ssid_connected;
        String ssid_scanned = wifiList.get(position);

        holder.textWifiNw.setText(ssid_scanned);
        holder.textisConnected.setVisibility(View.INVISIBLE);

        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            CcuLog.i(L.TAG_CCU_WIFI, "Current SSID:" + connectionInfo.getSSID() + " Wifi Available:" + ssid_scanned);
            ssid_scanned = String.format("\"%s\"", ssid_scanned);
            if (!TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid_connected = connectionInfo.getSSID();
                if (ssid_connected.equalsIgnoreCase(ssid_scanned)) {
                    holder.textisConnected.setVisibility(View.VISIBLE);
                    holder.textisConnected.setText("CONNECTED");
                    holder.textWifiNw.setTextColor(CCUUiUtil.getPrimaryThemeColor(mContext));
                    holder.imageSecurity.setVisibility(View.INVISIBLE);
                    holder.imageWifi.setColorFilter(CCUUiUtil.getPrimaryThemeColor(mContext), PorterDuff.Mode.SRC_IN);
                }
            }
        }
    }


    @Override
    public int getItemCount() {
        return wifiList.size();
    }
}