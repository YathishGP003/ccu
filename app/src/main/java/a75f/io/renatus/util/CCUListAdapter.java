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
import org.projecthaystack.HGrid;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.constants.HttpConstants;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.ccu.restore.CCU;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.registration.CCUSelect;
import a75f.io.renatus.registration.UpdateCCUFragment;
import a75f.io.renatus.util.remotecommand.bundle.BundleInstallManager;
import a75f.io.renatus.util.remotecommand.bundle.models.ArtifactDTO;
import a75f.io.renatus.util.remotecommand.bundle.models.UpgradeBundle;
import a75f.io.util.ExecutorTask;

public class CCUListAdapter extends RecyclerView.Adapter<CCUListAdapter.CCUView> {

    private List<CCU> ccuList;
    private Context context;
    private CCUSelect callBack;
    private boolean isCCUUpgradeRequired = false;
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
        return (!ccuList.get(position).isOnline());
    }

    private boolean isCCUVersionMatchingWithReplacingCCU(int position) {
        String ccuCurrVersion = CCUUiUtil.getCurrentCCUVersion();
        String ccuVersion = ccuList.get(position).getVersion();
        return  (ccuCurrVersion.contains(ccuVersion));
    }

    @Override
    public void onBindViewHolder(@NonNull CCUView holder, int position) {
        holder.name.setText(ccuList.get(position).getName());
        holder.lastUpdated.setText("Last Updated On "+ ccuList.get(position).getLastUpdated());
        String versionStr = ccuList.get(position).getVersion().replace("RENATUS_CCU","CCU");
        holder.version.setText("CCU Version "+versionStr );
        holder.itemView.setEnabled(false);

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
            checkBundleVersionPointPresent(ccuList, position);
        } );
    }

    private void checkBundleVersionPointPresent(List<CCU> ccuList, int position) {
        ExecutorTask.executeAsync(
                ()-> ProgressDialogUtils.showProgressDialog(context, "Checking for Bundle Version"),
                ()-> {
                    isCCUUpgradeRequired = false;

                    String bundleVersion = getBundleVersionPoint(ccuList, position);
                    UpgradeBundle bundle = new BundleInstallManager().getUpgradeBundleByName(bundleVersion, true);

                    CcuLog.d(L.TAG_CCU_BUNDLE, "Bundle Version Point"+bundle);

                    //Check if replace bundle required
                    if(bundleVersion != null && bundle != null && bundle.getUpgradeOkay() && bundle.getComponentsToUpgrade().size() > 0){
                        isCCUUpgradeRequired = true;
                        double size = 0;
                        for (ArtifactDTO artifactDTO : bundle.getComponentsToUpgrade()) {
                            size += Double.parseDouble(artifactDTO.getFileSize());
                            CcuLog.d(L.TAG_CCU_BUNDLE, "Upgradable ArtifactDTO name "+artifactDTO.getFileName() + " version "+artifactDTO.getVersion() + " size "+artifactDTO.getFileSize());

                        }
                        CcuLog.d(L.TAG_CCU_BUNDLE, "Update UI with Bundle Version Point"+bundleVersion);
                        try {
                            updateCCUFragment(fragmentManager, null, String.valueOf(size),
                                    true, bundle.getBundle().getBundleName(), bundle);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                    // If replace bundle is not allowed due to bundleVersion point not present or point is empty
                    // so replace CCU only
                    else if (!isCCUVersionMatchingWithReplacingCCU(position)) {
                        isCCUUpgradeRequired = true;
                        CCU ccu = ccuList.get(position);
                        try {
                            String fileSize = getFileSize(ccu.getVersion());
                            updateCCUFragment(fragmentManager, ccu, fileSize, false, null, bundle);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                ()->{
                    ProgressDialogUtils.hideProgressDialog();
                    // Everything is fine, proceed with CCU selection
                    if(!isCCUUpgradeRequired) {
                        callBack.onCCUSelect(ccuList.get(position));
                    }
                });
    }

    private String getBundleVersionPoint(List<CCU> ccuList, int position) {

        String ccuId = ccuList.get(position).getCcuId().replace("@", "");
        String query = ("domainName == \"" + DomainName.bundleVersion + "\" and ccuRef == \"" + ccuId + "\"");

        String response = CCUHsApi.getInstance().fetchRemoteEntityByQuery(query);

        if (response == null || response.isEmpty()) {
            CcuLog.d(L.TAG_CCU_BUNDLE, "Bundle Version Point response is empty");
            return null;
        }
        List<HashMap> bundleVersionPointMapList =
                CCUHsApi.getInstance().HGridToList(new HZincReader(response).readGrid());
        if(bundleVersionPointMapList.isEmpty()){
            CcuLog.d(L.TAG_CCU_BUNDLE, "Bundle Version Point list is empty");
            return null;
        }
        HashMap<Object, Object> bundleVersionMap = bundleVersionPointMapList.get(0);
        if(bundleVersionMap.isEmpty()){
            CcuLog.d(L.TAG_CCU_BUNDLE, "Bundle Version Point map not found");
            return null;
        }
        String bundleVersionId = bundleVersionMap.get(Tags.ID).toString();
        HGrid pointGrid = CCUHsApi.getInstance().readPointArrRemote(bundleVersionId);
        if (pointGrid == null) {
            CcuLog.d(L.TAG_CCU_BUNDLE, "Bundle Version Point grid is empty");
            return null;
        }
        Iterator it = pointGrid.iterator();
        while (it.hasNext()) {
            HRow r = (HRow) it.next();
            if (Integer.parseInt(r.get("level").toString()) == 8) {
                CcuLog.d(L.TAG_CCU_BUNDLE, "Bundle Version Point found value is "+r.get("val").toString());
                return r.get("val").toString();
            }
        }
        return null;
    }

    private void updateCCUFragment(FragmentManager parentFragmentManager, CCU ccu, String fileSize,
                                   boolean isBundleUpdate, String bundleName, UpgradeBundle bundle) throws JSONException {
        String currentAppVersionWithPatch = getCurrentAppVersionWithPatch();
        FragmentTransaction ft = parentFragmentManager.beginTransaction();
        Fragment previousFragment = parentFragmentManager.findFragmentByTag("popup");
        UpdateCCUFragment newFragment;
        if (previousFragment != null) {
            ft.remove(previousFragment);
        }
        if (isBundleUpdate) {
            newFragment = new UpdateCCUFragment().showBundleUpdateScreenFromReplaceCCU(true,
                    true, bundleName, fileSize, currentAppVersionWithPatch, bundle);
        } else {
            newFragment = new UpdateCCUFragment().showUiForReplaceCCU(currentAppVersionWithPatch,
                    ccu, fileSize, true);
        }
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
