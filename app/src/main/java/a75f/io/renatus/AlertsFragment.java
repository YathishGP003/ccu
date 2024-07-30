package a75f.io.renatus;

import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsiusTwoDecimal;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.views.MasterControl.MasterControlView;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class AlertsFragment extends Fragment implements AlertManager.AlertListListener
{
	
	ArrayList<Alert> alertList;
	ListView         listView;
	private static AlertAdapter adapter;
	private Disposable alertDeleteDisposable;
	private HashMap<Object, Object> useCelsius;


	public static AlertsFragment newInstance()
	{

		return new AlertsFragment();
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
        return inflater.inflate(R.layout.alert_fragment, container, false);
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		CcuLog.i("UI_PROFILING","AlertsFragment.onViewCreated");
		
		listView= view.findViewById(R.id.alertList);
		alertList=new ArrayList<>();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				Alert a = alertList.get(position);
				String message = a.getmMessage();

				if (message.contains("75F")) {
					String replacement = "75F";

					if (CCUUiUtil.isDaikinThemeEnabled(getContext())) {
						replacement = "SiteLine™";
					} else if (CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
						replacement = "ClimaVision";
					} else if (CCUUiUtil.isAiroverseThemeEnabled(requireContext())) {
						replacement = "Airoverse for Facilities";
					}

					message = message.replace("75F", replacement);
				}

				useCelsius = CCUHsApi.getInstance().readEntity("displayUnit");

				if (message.contains("\u00B0")) {
					message = formatMessageToCelsius(message);

				}
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(a.getmTitle() + "\n\n" + message + "\n"
				                   + "\n Alert Generated at " + getFormattedDate(a.getStartTime())
				                   + "\n Alert Fixed at "+getFormattedDate(a.getEndTime()))
				       .setCancelable(false)
				       .setIcon(R.drawable.ic_dialog_alert)
				       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					       public void onClick(DialogInterface dialog, int id) {
						       //do things
					       }
				       });
				
				if (!a.isFixed()) {
					builder.setNegativeButton("Mark-Fixed", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							AlertManager.getInstance().fixAlert(a);
							getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
						}
					});
				}
				
				
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
		
		listView.setOnItemLongClickListener((arg0, v, position, arg3) -> {

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Delete ?")
				   .setCancelable(true)
				   .setIcon(R.drawable.ic_dialog_alert)
				   .setPositiveButton("OK", (dialog, id) -> {
						Alert alert = alertList.get(position);
						// Remove item from local list regardless of server response.  Just log failure.
						alertList.remove(position);
						adapter.resetList(alertList);
						alertDeleteDisposable =
							AlertManager.getInstance().deleteAlert(alert)
									   .observeOn(AndroidSchedulers.mainThread())
									   .subscribe( () -> CcuLog.i(TAG_CCU_ALERTS, "delete success"),
											throwable -> CcuLog.w(TAG_CCU_ALERTS, "delete failure", throwable));
					   })
					.setNegativeButton("Cancel", (dialog, id) -> {
						//do things
					});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		});

		adapter = new AlertAdapter(alertList, getActivity());
		listView.setAdapter(adapter);
		AlertManager.getInstance().setAlertListListener(this);
		CcuLog.i("UI_PROFILING","AlertsFragment.onViewCreated Done");
		
	}

	String formatMessageToCelsius(String alertMessage) {

		StringBuilder sb = new StringBuilder();
		String[] strings;
		strings = alertMessage.split(" ");
		try {
			for (int i = 0; i < strings.length; i++) {
				//"\u00B0" is the unicode for °
				if (strings[i].contains("\u00B0")) {
					if(useCelsius.containsKey("id") && MasterControlView.getTuner(useCelsius.get("id").toString()) == TunerConstants.USE_CELSIUS_FLAG_ENABLED) {
						strings[i] = "\u00B0C";
						strings[i - 1] = String.valueOf(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(strings[i - 1])));
					} else {
						DecimalFormat df = new DecimalFormat("#.#");
						strings[i - 1] = String.valueOf(Double.parseDouble(df.format(Double.parseDouble(strings[i - 1]))));
					}
				}
			}
			for (String string : strings) {
				sb.append(string);
				sb.append(" ");
			}
		}catch (NumberFormatException e) {
			e.printStackTrace();
			CcuLog.e(TAG_CCU_ALERTS, "Failed to format units in alert message", e);
			return alertMessage;
		}
		return sb.toString();
	}
	
	String getFormattedDate(long millis) {
		if (millis == 0)
		{
			return "";
		}
		DateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
		Date date = new Date(millis);
		return sdf.format(date);
	}

	@Override
	public void onResume() {
		super.onResume();

		setAlertList();
	}

	@Override
	public void onStop() {
		if (alertDeleteDisposable != null) {
			// Common rxjava pattern to not execute call response if fragment is dead or shutting down.
			alertDeleteDisposable.dispose();
		}
		super.onStop();
	}
	private void setAlertList() {
		if(alertList != null) {
			alertList.clear();
			AlertManager.getInstance().getAllAlertsNotInternal().forEach(alert -> {
				if (alert.mAlertType.equalsIgnoreCase("CUSTOMER VISIBLE")) alertList.add(alert);
			});
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void setMenuVisibility(boolean menuVisible) {
		super.setMenuVisibility(menuVisible);
		if (menuVisible) {
			CcuLog.d(TAG_CCU_ALERTS, "menuVisible is visible");
			setAlertList();
		}
	}
	@Override
	public void onAlertsChanged() {
		int isAlertFragmentCreated = RenatusLandingActivity.mTabLayout.getSelectedTabPosition();
		if (isAlertFragmentCreated == 3 && !PreferenceUtil.getIsCcuLaunched()) {
			getActivity().runOnUiThread(() -> {
				setAlertList();
			});
		}
	}
}
