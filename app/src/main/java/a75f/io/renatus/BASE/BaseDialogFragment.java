package a75f.io.renatus.BASE;

import android.app.ProgressDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * Created by Yinten isOn 8/11/2017.
 */

public abstract class BaseDialogFragment extends DialogFragment
{
	protected void setTitle(String title)
	{
		getDialog().setTitle(title);
	}
	
	protected void showDialogFragment(DialogFragment dialogFragment, String id)
	{

		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(id);
		if (prev != null)
		{
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		// Create and show the dialog.
		dialogFragment.show(getFragmentManager(), id);
	}
	
	protected void removeDialogFragment(String id)
	{
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(id);
		if (prev instanceof DialogFragment)
		{
			ft.remove(prev).commitAllowingStateLoss();
		}
	}
	
	
	protected ProgressDialog mProgress;
	
	protected void showProgressDialog()
	{
		if (mProgress == null)
		{
			mProgress = new ProgressDialog(BaseDialogFragment.this.getActivity());
		}
		if (!mProgress.isShowing())
		{
			mProgress.show();
		}
	}
	protected void dismissProgressDialog()
	{
		if (mProgress != null && mProgress.isShowing())
		{
			mProgress.dismiss();
		}
	}
	
	public abstract String getIdString();
	
	@Override
	public void onPause()
	{
		super.onPause();
		dismissProgressDialog();
	}
	
	public void closeAllBaseDialogFragments()
	{
        for(Fragment fragment : getFragmentManager().getFragments())
        {
            if(fragment instanceof BaseDialogFragment)
            {
				    ((BaseDialogFragment) fragment).dismiss();
					removeDialogFragment(((BaseDialogFragment) fragment).getIdString());
            }
        }
		
		
	}
}
