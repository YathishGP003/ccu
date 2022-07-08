package a75f.io.renatus;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.apache.commons.lang3.StringUtils;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsiusTwoDecimal;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a75f.io.alerts.AlertManager;
import a75f.io.alerts.AlertSyncHandler;
import a75f.io.api.haystack.Alert;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.renatus.util.CCUUtils;
import a75f.io.renatus.views.MasterControl.MasterControlView;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class AlertsFragment extends Fragment
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
		View rootView = inflater.inflate(R.layout.alert_fragment, container, false);
		return rootView;
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

				useCelsius = CCUHsApi.getInstance().readEntity("displayUnit");

				if (message.contains("\u00B0")) {
					if(MasterControlView.getTuner(useCelsius.get("id").toString())== TunerConstants.USE_CELSIUS_FLAG_ENABLED) {
						message = formatMessageToCelsius(message);
					}
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(a.getmTitle() + "\n\n" + message + "\n"
				                   + "\n Alert Generated at " + getFormattedDate(a.getStartTime())
				                   + "\n Alert Fixed at "+getFormattedDate(a.getEndTime()))
				       .setCancelable(false)
				       .setIcon(android.R.drawable.ic_dialog_alert)
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
				   .setIcon(android.R.drawable.ic_dialog_alert)
				   .setPositiveButton("OK", (dialog, id) -> {
						Alert alert = alertList.get(position);
						// Remove item from local list regardless of server response.  Just log failure.
						alertList.remove(position);
						adapter.resetList(alertList);
						alertDeleteDisposable =
							AlertManager.getInstance().deleteAlert(alert)
									   .observeOn(AndroidSchedulers.mainThread())
									   .subscribe( () -> CcuLog.i("CCU_ALERT", "delete success"),
											throwable -> CcuLog.w("CCU_ALERT", "delete failure", throwable));
					   })
					.setNegativeButton("Cancel", (dialog, id) -> {
						//do things
					});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		});
		CcuLog.i("UI_PROFILING","AlertsFragment.onViewCreated Done");
		
	}

	String formatMessageToCelsius(String alertMessage){
		String reverse = StringUtils.reverse(alertMessage);
		String[] temp = StringUtils.substringsBetween(reverse,"\u00B0 "," ");
		for (String s : temp) {
			try {
				DecimalFormat f = new DecimalFormat("##.00");
				String celsiusVal = String.valueOf(f.format(fahrenheitToCelsiusTwoDecimal(Float.valueOf(StringUtils.reverse(s)))));
				Pattern fahrenheitVal = Pattern.compile(StringUtils.reverse(s));
				Matcher tempVal = fahrenheitVal.matcher(alertMessage);
				alertMessage = tempVal.replaceAll(celsiusVal);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Pattern fahrenheitUnit = Pattern.compile("\u00B0F");
		Matcher tempUnit = fahrenheitUnit.matcher(alertMessage);
		alertMessage = tempUnit.replaceAll("\u00B0C");
		return alertMessage;
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
			// Common rxjava pattern to not execute call response if fragment is dead or shuting down.
			alertDeleteDisposable.dispose();
		}
		super.onStop();
	}
	private void setAlertList() {
		alertList.clear();
		AlertManager alertManager = AlertManager.getInstance();
		if (!alertManager.hasService()) {
			alertManager.rebuildServiceNewToken(CCUHsApi.getInstance().getJwt());
		}
		AlertManager.getInstance().getAllAlertsNotInternal().forEach(alert -> {
			if(alert.mAlertType.equalsIgnoreCase("CUSTOMER VISIBLE")) alertList.add(alert);
		});
		adapter = new AlertAdapter(alertList,getActivity());
		listView.setAdapter(adapter);
	}
}
