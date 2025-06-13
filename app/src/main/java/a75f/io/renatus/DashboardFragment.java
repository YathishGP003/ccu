package a75f.io.renatus;


import static a75f.io.util.DashboardUtilKt.DASHBOARD;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.messaging.handler.DashboardHandler;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.util.DashboardRefreshListener;
import a75f.io.util.DashboardUtilKt;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class DashboardFragment extends Fragment implements DashboardRefreshListener {
    public DashboardFragment() {
    }

	private SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.dashboard_webview)
    WebView webView;

	@BindView(R.id.empty_dashboard)
	View emptyDashboard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_renatus_landing, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		DashboardHandler.Companion.setDashboardRefreshListener(this);
		super.onViewCreated(view, savedInstanceState);
        dashboardView();
    }

	private void dashboardView() {
		if(DashboardUtilKt.isDashboardConfig(Globals.getInstance().getApplicationContext())) {
			ProgressDialogUtils.showProgressDialog(getActivity(),"Loading dashboard...!");
			emptyDashboard.setVisibility(View.GONE);
			webView.setVisibility(View.VISIBLE);
			// Configure WebView settings
			WebSettings webSettings = webView.getSettings();
			webSettings.setJavaScriptEnabled(true); // Enable JavaScript
			webSettings.setDomStorageEnabled(true); // Enable DOM storage if needed
			webSettings.setAllowFileAccess(true);
			webSettings.setAllowUniversalAccessFromFileURLs(true); // Allow access to file:// URLs
			webSettings.setAllowFileAccessFromFileURLs(true);
			webSettings.setBuiltInZoomControls(true); // Enable pinch-to-zoom
			// Ensure links and redirects open within the WebView
			webView.setWebViewClient(new WebViewClient(){
				@Override
				public void onPageStarted(WebView view, String url, Bitmap favicon) {
					super.onPageStarted(view, url, favicon);
					Log.i(DASHBOARD, "onPageStarted: ");
				}

				@Override
				public void onPageFinished(WebView view, String url) {
					super.onPageFinished(view, url);
					ProgressDialogUtils.hideProgressDialog();
					Log.i(DASHBOARD, "onPageFinished: ");
				}
			});

			WebView.setWebContentsDebuggingEnabled(true);
			boolean isLocalAccessEnabled = sharedPrefs.getBoolean(this.getContext().getString(R.string.prefs_access_local_assests),false);
			if (isLocalAccessEnabled) {
				webView.loadUrl("file:///sdcard/CCU/www/index.html");    // use to dump it in sdcard for temporary testing
			} else {
				webView.loadUrl("file:///android_asset/www/index.html"); //use to dump it in assets folder
			}
		} else {
			emptyDashboard.setVisibility(View.VISIBLE);
			webView.setVisibility(View.GONE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		DashboardHandler.Companion.setDashboardRefreshListener(this);
		CcuLog.d(DASHBOARD, "onResume: DashboardFragment");
	}

	@Override
	public void onPause() {
		super.onPause();
		DashboardHandler.Companion.setDashboardRefreshListener(null);
		CcuLog.d(DASHBOARD, "onPause: DashboardFragment");
	}

	@Override
	public  void onStop() {
		super.onStop();
		DashboardHandler.Companion.setDashboardRefreshListener(null);
		CcuLog.d(DASHBOARD, "onStop: DashboardFragment");
	}

	@Override
	public void refreshDashboard(boolean isDashboardConfigured) {
		CcuLog.d(DASHBOARD, "onDashboardConfigured: " + isDashboardConfigured);

		if(DashboardFragment.this.getUserVisibleHint() && DashboardFragment.this.isVisible()) {
			CcuLog.d(DASHBOARD, "onDashboardConfigured: DashboardFragment is  visible");
			dashboardView();
		}
		else {
			CcuLog.d(DASHBOARD, "onDashboardConfigured: DashboardFragment is not visible");
		}
	}
}
