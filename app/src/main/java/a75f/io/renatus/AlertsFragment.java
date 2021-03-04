package a75f.io.renatus;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class AlertsFragment extends Fragment implements AlertSyncHandler.AlertDeleteListener
{
	
	ArrayList<Alert> alertList;
	ListView         listView;
	private static AlertAdapter adapter;
	
	public AlertsFragment()
	{
		new AlertSyncHandler(this);
	}
	     
	
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
							AlertManager.getInstance(getActivity()).fixAlert(a);
							getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
						}
					});
				}
				
				
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
		
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			
			public boolean onItemLongClick(AdapterView<?> arg0, View v,
			                               int position, long arg3) {
				// TODO Auto-generated method stub
				
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage("Delete ?")
				       .setCancelable(true)
				       .setIcon(android.R.drawable.ic_dialog_alert)
				       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					       public void onClick(DialogInterface dialog, int id) {
					       	   new Thread(new Runnable()
					           {
						           @Override
						           public void run()
						           {
							           AlertManager.getInstance(getActivity()).deleteAlert(alertList.get(position));
						           }
					           }).start();
						       
						       adapter.notifyDataSetChanged();
					       }
				        })
						.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							//do things
						}
						});
				AlertDialog alert = builder.create();
				alert.show();
				return true;
			}
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

	private void setAlertList() {
		alertList = new ArrayList<>(AlertManager.getInstance(getActivity()).getAllAlerts());

		adapter = new AlertAdapter(alertList,getActivity());

		listView.setAdapter(adapter);
	}

	@Override
	public void onDeleteSuccess() {
		if (getActivity() != null && isAdded()) {
			getActivity().runOnUiThread(() -> {
				alertList.clear();
				alertList = new ArrayList<>(AlertManager.getInstance(getActivity()).getAllAlerts());
				adapter = new AlertAdapter(alertList, getActivity());
				listView.setAdapter(adapter);
				adapter.notifyDataSetChanged();
			});
		}
	}
}
