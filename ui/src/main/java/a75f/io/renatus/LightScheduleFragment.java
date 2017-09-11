package a75f.io.renatus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;

import org.javolution.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import a75f.io.bo.building.Schedule;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.logic.cache.Globals;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by samjithsadasivan on 9/8/17.
 */

public class LightScheduleFragment extends BaseDialogFragment
{
	
	UUID mCurrentPortId = null;
	SmartNodeOutput mCurrentPort = null;
	
	Schedule mSchedule = null;
	
	@BindView(R.id.timePickerSt)
	TimePicker startTimePicker;
	
	@BindView(R.id.timePickerEt)
	TimePicker endTimePicker;
	
	@BindView(R.id.scheduleCancel)
	Button cancelBtn;
	
	@BindView(R.id.scheduleSave)
	Button saveBtn;
	
	@BindViews({R.id.checkBoxSun, R.id.checkBoxMon, R.id.checkBoxTue, R.id.checkBoxWed, R.id.checkBoxThu, R.id.checkBoxFri, R.id.checkBoxSat})
	List<CheckBox> daysList;
	
	public static LightScheduleFragment newInstance(SmartNodeOutput port){
		LightScheduleFragment fragment = new LightScheduleFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(FragmentCommonBundleArgs.SNOUTPUT_UUID, port.mUniqueID);
		fragment.setArguments(bundle);
		return fragment;
	}
	
	public LightScheduleFragment(){
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_light_schedule, container, false);
		mCurrentPortId = (UUID)getArguments().getSerializable(FragmentCommonBundleArgs.SNOUTPUT_UUID);
		ButterKnife.bind(this, rootView);
		//int titleTextId = getResources().getIdentifier("title", "id", "android");
		
		setTitle("Lighting Schedule");
		
		
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		mCurrentPort = (SmartNodeOutput) Globals.getInstance().getCCUApplication().findSmartNodePortByUUID(mCurrentPortId);
		
		if (mCurrentPort == null) {
			dismiss();
		}
			// else {
//			mSchedule = mCurrentPort.mSchedules.get(0);
//			fillScheduleData();
//		}
		
	}
	
	public void fillScheduleData() {
		if (mSchedule == null) {
			//No schedule yet
			return;
		}
	   
		
		for (int i = 0 ;i < 7; i++) {
			 
			//if ((CheckBox)daysList.get(i).)
		}
    }
    
	@OnClick(R.id.scheduleCancel)
	public void dismissDialog() {
		dismiss();
	}
	
	@OnClick(R.id.scheduleSave)
	public void saveSchedule() {
		if (mSchedule == null) {
			mSchedule = new Schedule();
		}
		
		for (int i = 0 ;i < 7; i++) {
			
			if (((CheckBox)daysList.get(i)).isChecked()) {
				//Add schedules
			}
		}
		
	}
	
	
	
}
