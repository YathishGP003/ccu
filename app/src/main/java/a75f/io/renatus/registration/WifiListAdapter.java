package a75f.io.renatus.registration;

import android.content.Context;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import a75f.io.renatus.R;

import static a75f.io.renatus.util.CCUUtils.getPrimaryThemeColor;

public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.WifiViewHolder> {

    //private List<ScanResult> wifiList;
    //HashMap<String,ScanResult> wifiList;
    //private ArrayList<HashMap<String, ScanResult>> wifiList;
    //private HashMap<String, ScanResult> wifiList;
    private List<String> wifiList;
    private Context mContext;
    private View.OnClickListener mOnClickListener;
    public String networkSSID;
    public String networkPass;
    private ItemClickListener onItemClickListener;

    public class WifiViewHolder extends RecyclerView.ViewHolder {
        public TextView textWifiNw;
        public TextView textisConnected;
        public ImageView imageWifi;
        public ImageView imageSecurity;

        public WifiViewHolder(View itemView) {
            super(itemView);
            textWifiNw = (TextView) itemView.findViewById(R.id.textWifiNw);
            textisConnected = (TextView) itemView.findViewById(R.id.textisConnected);
            imageWifi = (ImageView) itemView.findViewById(R.id.imageWifi);
            imageSecurity = (ImageView) itemView.findViewById(R.id.imageSecurity);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClickListener.onItemClicked(itemView, getAdapterPosition());
                }
            });
           /* itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    View alertview = LayoutInflater.from(mContext).inflate(R.layout.alert_wifipassword, null);
                    builder.setView(alertview);

                    EditText editText_Password = alertview.findViewById(R.id.editPassword);
                    builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            networkSSID = wifiList.get(getAdapterPosition());
                            networkPass = editText_Password.getText().toString().trim();
                            connectWifi(networkSSID, networkPass);

                            dialog.dismiss();
                        }
                    });


                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });


                    AlertDialog alertDialog = builder.create();
                    TextView tvisConnected = v.findViewById(R.id.textisConnected);
                    if (!tvisConnected.getText().toString().equals("CONNECTED")) {
                        alertDialog.show();
                    }
                }
            });*/
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
        //String ssid_scanned = wifiItem.get(WifiFragment.ITEM_KEY);
        //ScanResult scanResult = wifiList;
        String ssid_scanned = wifiList.get(position);

        holder.textWifiNw.setText(ssid_scanned);
        holder.textisConnected.setVisibility(View.INVISIBLE);

        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            Log.i("Wifi", "Current SSID:" + connectionInfo.getSSID() + " Wifi Available:" + ssid_scanned);
            ssid_scanned = String.format("\"%s\"", ssid_scanned);
            if (!TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid_connected = connectionInfo.getSSID();
                if (ssid_connected.equalsIgnoreCase(ssid_scanned)) {
                    holder.textisConnected.setVisibility(View.VISIBLE);
                    holder.textisConnected.setText("CONNECTED");
                    holder.textWifiNw.setTextColor(getPrimaryThemeColor(mContext));
                    holder.imageSecurity.setVisibility(View.INVISIBLE);
                    holder.imageWifi.setColorFilter(getPrimaryThemeColor(mContext), PorterDuff.Mode.SRC_IN);
                }
            }
        }
    }


    @Override
    public int getItemCount() {
        return wifiList.size();
    }
}