package a75f.io.renatus.ENGG;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import a75f.io.renatus.R;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FieldTestFragment extends Fragment
{
	public static FieldTestFragment newInstance(){
		return new FieldTestFragment();
	}
	
	@BindView(R.id.lightsTestBtn)
	Button lightBtn;
	
	@BindView(R.id.smartStatTestBtn)
	Button smartStatBtn;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_test, container, false);
		ButterKnife.bind(this , rootView);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}
	
	@OnClick(R.id.lightsTestBtn)
	public void handlelightTest() {
		showDialogFragment(LightingTestFragment.newInstance(), "lighttest");
	}
	
	@OnClick(R.id.smartStatTestBtn)
	public void handleSmartSmatTest()
	{
		showDialogFragment(SmartStatTestFragment.newInstance() ,"smartstat");
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
	
}
