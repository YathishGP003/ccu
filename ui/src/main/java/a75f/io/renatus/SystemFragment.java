package a75f.io.renatus;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;

import a75f.io.logic.bo.building.system.DxCIController;
import a75f.io.logic.tuners.SystemTunerUtil;
import a75f.io.logic.tuners.TunerConstants;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SystemFragment extends Fragment
{
	
	SeekBar  sbComfortValue;
	EditText stageStatusNow;
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
	
	

}
