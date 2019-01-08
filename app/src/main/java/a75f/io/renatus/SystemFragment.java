package a75f.io.renatus;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.logic.bo.building.system.DxCIController;
import a75f.io.logic.tuners.SystemTunerUtil;
import a75f.io.logic.tuners.TunerConstants;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SystemFragment extends Fragment
{
	private static final String TAG = "SystemFragment";
	SeekBar  sbComfortValue;
	EditText stageStatusNow;
	Spinner mSysSpinnerSchedule;


	public SystemFragment()
	{
	}
	
	
	public static SystemFragment newInstance()
	{
		return new SystemFragment();
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_system_setting, container, false);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{

		mSysSpinnerSchedule = view.findViewById(R.id.sysSpinnerSchedule);

		mSysSpinnerSchedule.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Toast.makeText(SystemFragment.this.getActivity(), "Schedule edit popup", Toast.LENGTH_SHORT).show();
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					showScheduleDialog();
				}


				return true;
			}
		});
		sbComfortValue = view.findViewById(R.id.systemComfortValue);
		sbComfortValue.setProgress(5 - (int)SystemTunerUtil.getDesiredCI());
		sbComfortValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Log.d("CCU", "CI Selected Val "+seekBar.getProgress());
				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground( final Void ... params ) {
						SystemTunerUtil.setDesiredCI(TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, 5 - seekBar.getProgress());
						
						return null;
					}
					
					@Override
					protected void onPostExecute( final Void result ) {
						// continue what you are doing...
					}
				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
			}
		});
		
		stageStatusNow = view.findViewById(R.id.stageStatusNow);
		stageStatusNow.setText(DxCIController.getInstance().getDxCIRtuState().name());
	}

	private void showScheduleDialog() {

		AlertDialog.Builder alert = new AlertDialog.Builder(SystemFragment.this.getActivity());

        Schedule siteSchedule = CCUHsApi.getInstance().getSiteSchedule();



        HGrid grid = HGridBuilder.dictToGrid(siteSchedule.getScheduleHDict());
        String systemScheduleGrid = HZincWriter.gridToString(grid);

		final EditText edittext = new EditText(SystemFragment.this.getActivity());
		alert.setMessage("Edit Schedule");
		alert.setTitle("Edit Schedule");
        edittext.setText(systemScheduleGrid);

		alert.setView(edittext);

		alert.setPositiveButton("Yes Option", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

				Editable editTextValue = edittext.getText();
				Log.i(TAG, "Edit Text : " + editTextValue.toString());
				HZincReader reader = new HZincReader(editTextValue.toString());
                Log.i(TAG, "######Reader Dump######");
				HGrid hGrid = reader.readGrid();

				HDict hDict = hGrid.row(0);
				Schedule schedule = new Schedule.Builder().setHDict(hDict).build();

				CCUHsApi.getInstance().updateSchedule(schedule);

				new Thread()
				{
					@Override
					public void run()
					{
						CCUHsApi.getInstance().syncEntityTree();
					}
				}.start();


			}

		});

		alert.setNegativeButton("No Option", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// what ever you want to do with No option.

			}
		});

		alert.show();


	}


}
