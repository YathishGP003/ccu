package a75f.io.renatus;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.ENGG.AppInstaller;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Mahesh on 17-07-2019.
 */
public class AboutFragment extends Fragment {


    private boolean mCCUAppDownloaded = false;
    private boolean mHomeAppDownloaded = false;
    @BindView(R.id.tvSerialNumber)
    TextView tvSerialNumber;
    @BindView(R.id.tvCcuVersion)
    TextView tvCcuVersion;

    ProgressBar loading;
    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;
    TextView tvmessage;
    public AboutFragment() {

    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                Log.d("CCU_DOWNLOAD", String.format("Received download complete for %d from %d and %d", downloadId, AppInstaller.getHandle().getCCUAppDownloadId(), AppInstaller.getHandle().getHomeAppDownloadId()));
                if (downloadId == AppInstaller.getHandle().getCCUAppDownloadId())
                    mCCUAppDownloaded = true;

                if (mCCUAppDownloaded) {
                    mCCUAppDownloaded = false;
                    mHomeAppDownloaded = false;
                    final boolean bNewHomeAppAvailable =false;
                    final boolean bNewCCUAppAvailable = AppInstaller.getHandle().isNewCCUAppAvailable();

                    if (bNewCCUAppAvailable || bNewHomeAppAvailable) {
                        if (alertDialog != null && alertDialog.isShowing()) {
                            loading.setVisibility(View.GONE);
							if (bNewCCUAppAvailable)
                                tvmessage.setText("New updates are available for Central Control Unit. Close to proceed.");
                            else if (bNewHomeAppAvailable)
                                tvmessage.setText("New updates are available for 75F Core Services Platform. Close to proceed.");
                            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    AppInstaller.getHandle().install(getActivity(), bNewHomeAppAvailable, bNewCCUAppAvailable, true);
                                }
                            });
                        }
                    } else {
                        if (alertDialog != null && alertDialog.isShowing()) {
                            loading.setVisibility(View.GONE);
                            tvmessage.setText("No new updates available to install at this time.");
                        }
                    }
                }
            }
        }
    };
    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        getActivity().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        ButterKnife.bind(this, rootView);

        HashMap site = CCUHsApi.getInstance().read("site");

        PackageManager pm = getActivity().getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0);
            String str = pi.versionName + "." + pi.versionCode;
            tvCcuVersion.setText(str);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
        tvSerialNumber.setText(siteGUID == null? site.get("id").toString() :siteGUID);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
    @OnClick(R.id.checkUpdatesInstll)
    public void checkAndUpdate(){
        builder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.updates_custom_dialog, null);
        getActivity().findViewById(R.id.layout_root);
        TextView tv = (TextView) layout.findViewById(R.id.title);
        tv.setText("Download Updates");
        tvmessage = (TextView) layout.findViewById(R.id.text);
        loading = (ProgressBar) layout.findViewById(R.id.loading);
        tvmessage.setText("Checking for updates. Please wait...");
        Button close = (Button) layout.findViewById(R.id.close_button);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        builder.setView(layout);
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();

        mCCUAppDownloaded = false;
        mHomeAppDownloaded = false;
        AppInstaller.getHandle().downloadInstalls();
    }
}
