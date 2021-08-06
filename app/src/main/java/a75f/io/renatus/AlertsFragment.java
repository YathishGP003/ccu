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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import a75f.io.alerts.AlertManager;
import a75f.io.alerts.AlertSyncHandler;
import a75f.io.api.haystack.Alert;
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
		listView= view.findViewById(R.id.alertList);
		alertList=new ArrayList<>();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				Alert a = alertList.get(position);
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(a.getmTitle() + "\n\n" + a.getmMessage() + "\n"
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
