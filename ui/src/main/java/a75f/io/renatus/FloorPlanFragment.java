package a75f.io.renatus;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.Zone;
import a75f.io.logic.SmartNodeBLL;
import a75f.io.renatus.BLE.FragmentDeviceScan;
import a75f.io.util.Globals;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnItemClick;

/**
 * Created by samjithsadasivan on 8/7/17.
 */

public class FloorPlanFragment extends Fragment
{
	public int mCurFloorIndex = -1;
	public int mCurRoomIndex  = -1;
	
	@BindView(R.id.addFloorBtn)
	ImageButton addFloorBtn;
	
	@BindView(R.id.addRoomBtn)
	ImageButton addRoomBtn;
	
	@BindView(R.id.pairModuleBtn)
	ImageButton pairModuleBtn;
	
	@BindView(R.id.addFloorEdit)
	EditText addFloorEdit;
	
	@BindView(R.id.addRoomEdit)
	EditText addRoomEdit;
	
	@BindView(R.id.addModuleEdit)
	EditText addModuleEdit;
	
	@BindView(R.id.floorList)
	ListView floorListView;
	
	@BindView(R.id.roomList)
	ListView roomListView;
	
	@BindView(R.id.moduleList)
	ListView moduleListView;
	
	private DataArrayAdapter<Floor> mModuleListAdapter = null;
	public  DataArrayAdapter<Zone>  mRoomListAdapter   = null;
	
	
	public FloorPlanFragment()
	{
	}
	
	
	public static FloorPlanFragment newInstance()
	{
		return new FloorPlanFragment();
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_floorplan, container, false);
		ButterKnife.bind(this, rootView);
		return rootView;
	}
	
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		enableFloorButton();
		disableRoomModule();
	}
	
	
	@Override
	public void onStart()
	{
		super.onStart();
		floorListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		floorListView.setMultiChoiceModeListener(new FloorListActionMenuListener(this));
		roomListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		roomListView.setMultiChoiceModeListener(new RoomListActionMenuListener(this));
		//EventBus.getDefault().register();
	}
	
	
	@Override
	public void onResume()
	{
		super.onResume();
		mCurFloorIndex = mCurFloorIndex >= 0 ? mCurFloorIndex : 0;
		floorListView.setAdapter(FloorContainer.getInstance().getFloorListAdapter());
		if (FloorContainer.getInstance().getFloorList().size() > 0)
		{
			selectFloor(mCurFloorIndex);
			mRoomListAdapter = new DataArrayAdapter<Zone>(Globals.getInstance().getApplicationContext(), R.layout.listviewitem, FloorContainer.getInstance().getFloorList().get(mCurFloorIndex).getRoomList());
			roomListView.setAdapter(mRoomListAdapter);
			enableRoomBtn();
			if (FloorContainer.getInstance().getFloorList().get(mCurFloorIndex).getRoomList().size() > 0)
			{
				enableModueButton();
			}
		}
	}
	
	
	@Override
	public void onPause()
	{
		super.onPause();
		FloorContainer.getInstance().saveData();
	}
	
	
	private void selectFloor(int position)
	{
		mCurFloorIndex = position;
		FloorContainer.getInstance().getFloorListAdapter().setSelectedItem(position);
		mRoomListAdapter = new DataArrayAdapter<Zone>(Globals.getInstance().getApplicationContext(), R.layout.listviewitem, FloorContainer.getInstance().getFloorList().get(mCurFloorIndex).getRoomList());
		roomListView.setAdapter(mRoomListAdapter);
		selectRoom(0);
	}
	
	
	private void enableRoomBtn()
	{
		addRoomBtn.setVisibility(View.VISIBLE);
		addRoomEdit.setVisibility(View.INVISIBLE);
	}
	
	
	private void enableModueButton()
	{
		pairModuleBtn.setVisibility(View.VISIBLE);
	}
	
	
	private void selectRoom(int position)
	{
		mCurRoomIndex = position;
		mRoomListAdapter.setSelectedItem(position);
	}
	
	
	private void enableFloorButton()
	{
		addFloorBtn.setVisibility(View.VISIBLE);
		addFloorEdit.setVisibility(View.INVISIBLE);
	}
	
	
	private void disableRoomModule()
	{
		addRoomBtn.setVisibility(View.INVISIBLE);
		addRoomEdit.setVisibility(View.INVISIBLE);
		pairModuleBtn.setVisibility(View.INVISIBLE);
		addModuleEdit.setVisibility(View.INVISIBLE);
	}
	
	
	@OnClick(R.id.addFloorBtn)
	public void handleFloorBtn()
	{
		enableFloorEdit();
		addFloorEdit.setText("");
		addFloorEdit.requestFocus();
		InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(addFloorEdit, InputMethodManager.SHOW_IMPLICIT);
	}
	
	
	private void enableFloorEdit()
	{
		addFloorBtn.setVisibility(View.INVISIBLE);
		addFloorEdit.setVisibility(View.VISIBLE);
	}
	
	
	@OnEditorAction(R.id.addFloorEdit)
	public boolean handleFloorChange(TextView v, int actionId, KeyEvent event)
	{
		if (actionId == EditorInfo.IME_ACTION_DONE)
		{
			final int nID = FloorContainer.getInstance().addFloor(addFloorEdit.getText().toString());
			selectFloor(nID);
			mRoomListAdapter = new DataArrayAdapter<Zone>(Globals.getInstance().getApplicationContext(), R.layout.listviewitem, FloorContainer.getInstance().getFloorList().get(mCurFloorIndex).getRoomList());
			roomListView.setAdapter(mRoomListAdapter);
			//CCUKinveyInterface.updateFloor(FloorData.getFloorData().get(nID), false);
			InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);
			Toast.makeText(getActivity().getApplicationContext(), "Floor " + addFloorEdit.getText() + " added", Toast.LENGTH_SHORT).show();
			enableFloorButton();
			enableRoomBtn();
			return true;
		}
		return false;
	}
	
	
	@OnFocusChange(R.id.addFloorEdit)
	public void handleFloorFocus(View v, boolean hasFocus)
	{
		if (!hasFocus)
		{
			enableFloorButton();
		}
	}
	
	
	@OnClick(R.id.addRoomBtn)
	public void handleRoomBtn()
	{
		enableRoomEdit();
		addRoomEdit.setText("");
		addRoomEdit.requestFocus();
		InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(addRoomEdit, InputMethodManager.SHOW_IMPLICIT);
	}
	
	
	private void enableRoomEdit()
	{
		addRoomBtn.setVisibility(View.INVISIBLE);
		addRoomEdit.setVisibility(View.VISIBLE);
	}
	
	
	@OnEditorAction(R.id.addRoomEdit)
	public boolean handleRoomChange(TextView v, int actionId, KeyEvent event)
	{
		if (actionId == EditorInfo.IME_ACTION_DONE)
		{
			Toast.makeText(getActivity().getApplicationContext(), "Room " + addRoomEdit.getText() + " added", Toast.LENGTH_SHORT).show();
			Zone room = FloorContainer.getInstance().getFloorList().get(mCurFloorIndex).addZone(addRoomEdit.getText().toString());
			selectRoom(FloorContainer.getInstance().getFloorList().size() - 1);
			InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(addRoomEdit.getWindowToken(), 0);
			enableRoomBtn();
			enableModueButton();
			return true;
		}
		return false;
	}
	
	
	@OnFocusChange(R.id.addRoomEdit)
	public void handleRoomFocus(View v, boolean hasFocus)
	{
		if (!hasFocus)
		{
			enableRoomBtn();
		}
	}
	
	
	@OnClick(R.id.pairModuleBtn)
	public void startPairing()
	{//TODO - Test code hardcoded to fix crash
		short meshAddress = SmartNodeBLL.nextSmartNodeAddress();
		String roomName = "75F Room";
		FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(meshAddress, roomName);
		showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
	}
	
	
	private void showDialogFragment(DialogFragment dialogFragment, String id)
	{
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(id);
		if (prev != null)
		{
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		// Create and show the dialog.
		dialogFragment.show(ft, id);
	}
	
	
	@OnItemClick(R.id.floorList)
	public void setFloorListView(AdapterView<?> parent, View view, int position, long id)
	{
		selectFloor(position);
	}
	
	
	@OnItemClick(R.id.roomList)
	public void setRoomListView(AdapterView<?> parent, View view, int position, long id)
	{
		selectRoom(position);
	}
	
	
	@OnItemClick(R.id.moduleList)
	public void setModuleListView(AdapterView<?> parent, View view, int position, long id)
	{
		//select module
	}
}
