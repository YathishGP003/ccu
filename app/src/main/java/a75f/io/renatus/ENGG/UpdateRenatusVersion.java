package a75f.io.renatus.ENGG;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.javolution.annotations.Nullable;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class UpdateRenatusVersion extends Fragment {

    public static UpdateRenatusVersion newInstance() { return new UpdateRenatusVersion(); }

    @BindView(R.id.renatusVersionText)
    EditText renatusVersionText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_update_renatus, container, false);
        ButterKnife.bind(this, rootView);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @OnClick(R.id.startUpdateVersion)
    public void handleStartInstallUpdate() {
        Activity activity = getActivity();

        String apkName = renatusVersionText.getText().toString();
        Log.d("CCU_DOWNLOAD", "got command to install update--"+DownloadManager.EXTRA_DOWNLOAD_ID +","+apkName);
        RenatusApp.getAppContext().registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    Log.d("CCU_DOWNLOAD", String.format("Received download complete for %d from %d and %d", downloadId, AppInstaller.getHandle().getCCUAppDownloadId(), AppInstaller.getHandle().getDownloadedFileVersion(downloadId)));
                    if (downloadId == AppInstaller.getHandle().getCCUAppDownloadId()) {
                        if (AppInstaller.getHandle().getDownloadedFileVersion(downloadId) > 0)
                            AppInstaller.getHandle().install(null, false, true, true);
                    }/*else if(downloadId == AppInstaller.getHandle().getHomeAppDownloadId()){
                        int homeAppVersion = AppInstaller.getHandle().getDownloadedFileVersion(downloadId);
                        if(homeAppVersion >= 17) {
                            PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit().putInt("home_app_version", homeAppVersion).commit();
                            AppInstaller.getHandle().install(null, true, false, true);
                        }
                    }*/
                }
            }

        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        if(apkName.startsWith("75f") || apkName.startsWith("75F"))
            AppInstaller.getHandle().downloadHomeInstall(apkName);
        else
            AppInstaller.getHandle().downloadCCUInstall(apkName);
    }



    }
