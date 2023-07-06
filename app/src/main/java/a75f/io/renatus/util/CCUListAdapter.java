package a75f.io.renatus.util;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.ccu.restore.CCU;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.registration.CCUSelect;
import a75f.io.renatus.registration.UpdateCCUFragment;

public class CCUListAdapter extends RecyclerView.Adapter<CCUListAdapter.CCUView> {

    private List<CCU> ccuList;
    private Context context;
    private CCUSelect callBack;
    private androidx.fragment.app.FragmentManager fragmentManager;
    public CCUListAdapter(List<CCU> ccuList, Context context, CCUSelect callBack, androidx.fragment.app.FragmentManager parentFragmentManager){
        this.ccuList = ccuList;
        this.context = context;
        this.callBack = callBack;
        this.fragmentManager = parentFragmentManager;
    }

    @NonNull
    @Override
    public CCUView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cuu_item, parent, false);
        return new CCUView(itemView);
    }

    private boolean isCCUReplaceable(int position){
        String ccuCurrVersion = CCUUiUtil.getCurrentCCUVersion();
        String ccuVersion = ccuList.get(position).getVersion();
        String lastUpdatedDateTime = ccuList.get(position).getLastUpdated();
        if (StringUtils.isEmpty(ccuCurrVersion) || StringUtils.isEmpty(ccuVersion) || lastUpdatedDateTime.equalsIgnoreCase("n/a")) {
            return false;
        }
        return ((ccuCurrVersion.contains(ccuVersion)) && (!ccuList.get(position).isOnline()));
    }
    @Override
    public void onBindViewHolder(@NonNull CCUView holder, int position) {
        holder.name.setText(ccuList.get(position).getName());
        holder.lastUpdated.setText("Last Updated On "+ ccuList.get(position).getLastUpdated());
        holder.version.setText("CCU Version "+ ccuList.get(position).getVersion());
       //holder.itemView.setEnabled(false);

        holder.status.setText("ONLINE");
        if (!ccuList.get(position).isOnline()) {
            holder.status.setText("OFFLINE");
        }

        if (isCCUReplaceable(position)) {
           holder.isOffline.setBackgroundColor(CCUUiUtil.getPrimaryThemeColor(context));
           holder.itemView.setEnabled(true);
           holder.name.setTextColor(Color.BLACK);
           holder.lastUpdated.setTextColor(Color.BLACK);
           holder.version.setTextColor(Color.BLACK);
           holder.status.setTextColor(Color.BLACK);
       }

        holder.itemView.setOnClickListener(view ->{
            if (!isCCUReplaceable(position)) {
                checkIsCCUHasRecommendedVersion(ccuList.get(position));
            } else {
                callBack.onCCUSelect(ccuList.get(position));
            }
        } );
    }

    public void checkIsCCUHasRecommendedVersion(CCU ccu){
        RxjavaUtil.executeBackgroundTaskWithDisposable(
                ()->{ProgressDialogUtils.showProgressDialog(context, "Checking for File size");
                },
                ()-> {
                    try {
                        String fileSize = getFileSize(ccu.getVersion());
                        updateCCUFragment(fragmentManager, ccu, fileSize);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                ProgressDialogUtils::hideProgressDialog);
    }


    private void updateCCUFragment(FragmentManager parentFragmentManager, CCU ccu, String fileSize) {
        String currentAppVersionWithPatch = getCurrentAppVersionWithPatch();
            FragmentTransaction ft = parentFragmentManager.beginTransaction();
            Fragment previousFragment = parentFragmentManager.findFragmentByTag("popup");
            if (previousFragment != null) {
                ft.remove(previousFragment);
            }
            UpdateCCUFragment newFragment = new UpdateCCUFragment(currentAppVersionWithPatch,
                    ccu, fileSize);
            newFragment.show(ft, "popup");
    }

    private static String getFileSize(String currentCCUVersion) throws JSONException {
        JsonObject body = new JsonObject();
        String[] currentVersionComponents = currentCCUVersion.split("\\.");
        body.addProperty("majorVersion", currentVersionComponents[0]);
        body.addProperty("minorVersion", currentVersionComponents[1]);
        body.addProperty("patchVersion", currentVersionComponents[2]);

        String response = HttpUtil.executeJson(
                RenatusServicesEnvironment.getInstance().getUrls().getGetCCUFileSize(),
                body.toString(),
                a75f.io.api.haystack.BuildConfig.HAYSTACK_API_KEY,
                true,
                HttpConstants.HTTP_METHOD_POST
        );
        if (response != null) {
            JSONObject jsonResponse = new JSONObject(response);
            return jsonResponse.get("size").toString();
        } else {
            return "0";
        }
    }

    private String getCurrentAppVersionWithPatch() {
        return BuildConfig.VERSION_NAME.split(BuildConfig.BUILD_TYPE +"_")[1];
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
