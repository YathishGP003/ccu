package a75f.io.renatus.util;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

import java.util.ArrayList;
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
import a75f.io.renatus.util.remotecommand.bundle.models.BundleDTO;
import a75f.io.renatus.util.remotecommand.bundle.models.UpgradeBundle;
import a75f.io.util.ExecutorTask;

public class CCUListAdapter extends RecyclerView.Adapter<CCUListAdapter.CCUView> {

    private final List<CCU> ccuList;
    private final Context context;
    private final CCUSelect callBack;
    private boolean isCCUUpgradeRequired = false;
    String selectedCCUId = null;
    private final androidx.fragment.app.FragmentManager fragmentManager;
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
                () -> ProgressDialogUtils.showProgressDialog(context, "Checking for Bundle Version"),
                () -> {
                    selectedCCUId = ccuList.get(position).getCcuId().replace("@", "");
                    UpgradeBundle recommendedBundle = new BundleInstallManager().getRecommendedUpgradeBundle(true);

                    String cloudCcuVersion = CCUHsApi.getInstance()
                            .readDefaultStrValueRemote("(diag and app and version) and ccuRef == @" + selectedCCUId);
                    String cloudBacAppVersion = CCUHsApi.getInstance()
                            .readDefaultStrValueRemote("(diag and bacnet and version) and ccuRef == @" + selectedCCUId);
                    String cloudRemoteAppVersion = CCUHsApi.getInstance()
                            .readDefaultStrValueRemote("(diag and remote and version) and ccuRef == @" + selectedCCUId);
                    String cloudHomeAppVersion = CCUHsApi.getInstance()
                            .readDefaultStrValueRemote("(diag and home and version) and  ccuRef == @" + selectedCCUId);


                    String localCcuVersion = CCUUiUtil.getCurrentCCUVersion();
                    String localBacAppVersion = CCUUiUtil.getAppVersion(context, "io.seventyfivef.bacapp");
                    String localRemoteAppVersion = CCUUiUtil.getAppVersion(context, "io.seventyfivef.remoteaccess");
                    String localHomeAppVersion = CCUUiUtil.getAppVersion(context, "com.x75frenatus.home");

                    if (localHomeAppVersion != null) {
                        String[] splitVersion = localHomeAppVersion.split("_");
                        localHomeAppVersion = splitVersion[splitVersion.length - 1];
                    }

                    if (localBacAppVersion != null) {
                        String[] splitVersion = localBacAppVersion.split("_");
                        localBacAppVersion = splitVersion[splitVersion.length - 1];
                    }

                    if (localRemoteAppVersion != null) {
                        String[] splitVersion = localRemoteAppVersion.split("_");
                        localRemoteAppVersion = splitVersion[splitVersion.length - 1];
                    }

                    String ccuApkFileSize = getFileSize(cloudCcuVersion);
                    CcuLog.d(L.TAG_CCU_REPLACE, "Cloud CCU Version: " + cloudCcuVersion + ", Local CCU Version: " + localCcuVersion
                            + ",\n Cloud BACnet App Version: " + cloudBacAppVersion + ", Local BACnet App Version: " + localBacAppVersion
                            + ",\n Cloud Remote Access App Version: " + cloudRemoteAppVersion + ", Local Remote Access App Version: " + localRemoteAppVersion
                            + ",\n Cloud Home App Version: " + cloudHomeAppVersion + ", Local Home App Version: " + localHomeAppVersion);


                    String buildType = BuildConfig.BUILD_TYPE;
                    CCU ccu = ccuList.get(position);
                    HashMap<String, String> recommendedAppNameVersion = new HashMap<>();
                    HashMap<String, String> appNameVersion = new HashMap<>();
                    ArrayList<String> apkNames = new ArrayList<>();
                    assert recommendedBundle != null;
                    BundleDTO bto = recommendedBundle.getBundle();
                    // Check if the cloud version is different from the local version
                    if (cloudCcuVersion != null && !cloudCcuVersion.contains(localCcuVersion)) {
                        isCCUUpgradeRequired = true;
                        apkNames.add("CCU_" + buildType + "_" + cloudCcuVersion + ".apk");
                    }

                    // Check if the cloud BACnet app version is different from the local version
                    if (cloudBacAppVersion == null) {
                        if (getArtifactVersionString(bto, "bacapp") != null) {
                            if(localBacAppVersion == null ||
                                    !localBacAppVersion.equals(bto.getBACArtifact().getVersion())){
                                String name = "NONE";
                                if(localBacAppVersion != null){
                                    name = localBacAppVersion;
                                }
                                CcuLog.d(L.TAG_CCU_REPLACE, "updating recommended Bundle BACnet app");
                                apkNames.add("BACapp_" + buildType + "_" + bto.getBACArtifact().getVersion() + ".apk");
                                appNameVersion.put("bacAppVersion", name);
                                recommendedAppNameVersion.put("recommendedBacAppVersion", bto.getBACArtifact().getVersion());
                            }
                        }
                    } else if (localBacAppVersion == null) {
                        apkNames.add("BACapp_" + buildType + "_" + cloudBacAppVersion + ".apk");
                        appNameVersion.put("bacAppVersion", "NONE");
                        recommendedAppNameVersion.put("recommendedBacAppVersion", cloudBacAppVersion);
                    } else if (!localBacAppVersion.contains(cloudBacAppVersion)) {
                        apkNames.add("BACapp_" + buildType + "_" + cloudBacAppVersion + ".apk");
                        appNameVersion.put("bacAppVersion", localBacAppVersion);
                        recommendedAppNameVersion.put("recommendedBacAppVersion", cloudBacAppVersion);
                    }

                    // Check if the cloud Home app version is different from the local version
                    if (cloudHomeAppVersion == null) {
                        if (getArtifactVersionString(bto, "homeapp") != null) {
                            if(localHomeAppVersion == null ||
                                    !localHomeAppVersion.equals(bto.getHomeArtifact().getVersion())) {
                                String name = "NONE";
                                if(localHomeAppVersion != null){
                                    name = localHomeAppVersion;
                                }
                                CcuLog.d(L.TAG_CCU_REPLACE, "updating recommended Bundle home app");
                                apkNames.add("HomeApp_" + buildType + "_" + bto.getHomeArtifact().getVersion() + ".apk");
                                appNameVersion.put("homeAppVersion", name);
                                recommendedAppNameVersion.put("recommendedHomeAppVersion", bto.getHomeArtifact().getVersion());
                            }
                        }
                    } else if (localHomeAppVersion == null) {
                        apkNames.add("HomeApp_" + buildType + "_" + cloudHomeAppVersion + ".apk");
                        appNameVersion.put("homeAppVersion", "NONE");
                        recommendedAppNameVersion.put("recommendedHomeAppVersion", cloudHomeAppVersion);
                    } else if (!localHomeAppVersion.contains(cloudHomeAppVersion)) {
                        apkNames.add("HomeApp_" + buildType + "_" + cloudHomeAppVersion + ".apk");
                        appNameVersion.put("homeAppVersion", localHomeAppVersion);
                        recommendedAppNameVersion.put("recommendedHomeAppVersion", cloudHomeAppVersion);
                    }

                    if (cloudRemoteAppVersion == null) {
                        if (getArtifactVersionString(bto, "remoteapp") != null) {
                            if(localRemoteAppVersion == null ||
                                    !localRemoteAppVersion.equals(bto.getRemoteArtifact().getVersion())) {
                                String name = "NONE";
                                if(localRemoteAppVersion != null){
                                    name = localRemoteAppVersion;
                                }
                                CcuLog.d(L.TAG_CCU_REPLACE, "updating recommended Bundle remote app");
                                apkNames.add("RemoteApp_" + buildType + "_" + bto.getRemoteArtifact().getVersion() + ".apk");
                                appNameVersion.put("remoteAppVersion", name);
                                recommendedAppNameVersion.put("recommendedRemoteAppVersion", bto.getRemoteArtifact().getVersion());
                            }
                        }
                    } else if (localRemoteAppVersion == null) {
                        apkNames.add("RemoteApp_" + buildType + "_" + cloudRemoteAppVersion + ".apk");
                        appNameVersion.put("remoteAppVersion", "NONE");
                        recommendedAppNameVersion.put("recommendedRemoteAppVersion", cloudRemoteAppVersion);
                    } else if (!localRemoteAppVersion.contains(cloudRemoteAppVersion)) {
                        apkNames.add("RemoteApp_" + buildType + "_" + cloudRemoteAppVersion + ".apk");
                        appNameVersion.put("remoteAppVersion", localRemoteAppVersion);
                        recommendedAppNameVersion.put("recommendedRemoteAppVersion", cloudRemoteAppVersion);
                    }

                    // proceed with replace
                    if (apkNames.isEmpty()) {
                        return;
                    }

                    boolean isCcuAppExistForReplace = false;

                    for (String apkName : apkNames) {
                        if (apkName.contains("CCU")) {
                            isCcuAppExistForReplace = true;
                            break;
                        }
                    }

                    try {
                        CcuLog.d(L.TAG_CCU_REPLACE, "to be downloaded apks: " + apkNames);
                        isCCUUpgradeRequired = true;
                        updateCCUFragment(fragmentManager, ccu, apkNames, appNameVersion, recommendedAppNameVersion, ccuApkFileSize, isCcuAppExistForReplace);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    ProgressDialogUtils.hideProgressDialog();
                    if (!isCCUUpgradeRequired) {
                        callBack.onCCUSelect(ccuList.get(position));
                    }
                });
    }

    private String getBundleVersionPoint(List<CCU> ccuList, int position) {

        String ccuId = ccuList.get(position).getCcuId();
        String query = ("domainName == @" + DomainName.bundleVersion + " and ccuRef == " + ccuId );

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

    private void updateCCUFragment(FragmentManager parentFragmentManager, CCU ccu,
                                   ArrayList<String> apkNames, HashMap<String, String> appNameVersion,
           HashMap<String, String> recommendedAppNameVersion, String ccuAppFileSize, boolean isCcuAppExistForReplace) throws JSONException {
        String currentAppVersionWithPatch = getCurrentAppVersionWithPatch();
        FragmentTransaction ft = parentFragmentManager.beginTransaction();
        UpdateCCUFragment newFragment = new UpdateCCUFragment().showUiForReplaceCCU(
                currentAppVersionWithPatch,
                ccu,
                ccuAppFileSize,
                true,
                apkNames,
                appNameVersion,
                recommendedAppNameVersion,
                isCcuAppExistForReplace
                );
        newFragment.show(ft, "popup");

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

    public String getArtifactVersionString(BundleDTO bto, String artifactName) {
        String isArtifactEmpty = null;
        switch (artifactName) {
            case "homeapp":
                if (bto.getHomeArtifact() != null
                        && bto.getHomeArtifact().getVersion() != null) {
                    if (!bto.getHomeArtifact().getVersion().isEmpty()) {
                        CcuLog.d(L.TAG_CCU_REPLACE, "Recommended Bundle home app is not empty, hence not updating");
                        isArtifactEmpty = bto.getHomeArtifact().getVersion();
                    }
                }
                break;
            case "bacapp":
                if (bto.getBACArtifact() != null
                        && bto.getBACArtifact().getVersion() != null) {
                    if (!bto.getBACArtifact().getVersion().isEmpty()) {
                        CcuLog.d(L.TAG_CCU_REPLACE, "Recommended Bundle bac app is not empty, hence not updating");
                        isArtifactEmpty = bto.getBACArtifact().getVersion();
                    }
                }
                break;
            case "remoteapp":
                if (bto.getRemoteArtifact() != null
                        || bto.getRemoteArtifact().getVersion() != null) {
                    if (!bto.getRemoteArtifact().getVersion().isEmpty()) {
                        CcuLog.d(L.TAG_CCU_REPLACE, "Recommended Bundle Remote app is not empty, hence not updating");
                        isArtifactEmpty = bto.getRemoteArtifact().getVersion();
                    }
                }
                break;
        }
        return isArtifactEmpty;
    }

    private static String getFileSize(String currentCCUVersion){
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
        CcuLog.d(L.TAG_CCU_DOWNLOAD, "Response from getCCUFileSize: " + response);
        if (response != null) {
            JSONObject jsonResponse = null;
            try {
                jsonResponse = new JSONObject(response);
                return jsonResponse.get("size").toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            return "0";
        }
        return "0";
    }
}
