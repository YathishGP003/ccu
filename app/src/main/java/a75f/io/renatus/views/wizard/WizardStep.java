package a75f.io.renatus.views.wizard;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Base class for a wizard's step.
 * As with regular {@link Fragment} each inherited class must have an empty constructor.
 */
public abstract class WizardStep extends Fragment implements TaskCallbacks {
	@SuppressWarnings("unused")
	private static final String TAG = "WizardStep";
	private List<AsyncTask> tasks = new ArrayList<AsyncTask>();
	protected TaskCallbacks mCallbacks;

	static interface OnStepStateChangedListener {
		void onStepStateChanged(WizardStep step);
	}

	static final int STATE_PENDING 	= 0;
	static final int STATE_RUNNING 	= 1;
	static final int STATE_COMPLETED = 2;
	static final int STATE_ABORTED 	= 3;

	private OnStepStateChangedListener onStepStateChangedListener;
	private int state = STATE_PENDING; //Default state for all steps

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Bundle args = getArguments();
		if (args != null) {
			bindFields(args);
		}
		mCallbacks = this;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = null;
	}

	protected void registerTask(AsyncTask task) {
		tasks.add(task);
	}

	protected void unregisterTask(AsyncTask task) {
		if (tasks.contains(task)) {
			tasks.remove(task);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		for (AsyncTask task : tasks) {
			task.cancel(true);
			tasks.remove(task);
		}
	}

	private void bindFields(Bundle args) {
		//Scan the step for fields annotated with @ContextVariable
		//and bind value if found in step's arguments
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.getAnnotation(ContextVariable.class) != null && args.containsKey(field.getName())) {
				field.setAccessible(true);
				try {
					if (field.getType() == Date.class) {
						field.set(this, new Date(args.getLong(field.getName())));
					}
					else {
						field.set(this, args.get(field.getName()));
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Mark the step as 'Done' and proceed to the next step in the flow.
	 */
	public final void done() {
		setState(STATE_COMPLETED);
	}

	/**
	 * Mark the step as 'Aborted' and go back to previous step or activity.
	 */
	public final void abort() {
		setState(STATE_ABORTED);
	}

	void setOnStepChangedListener(OnStepStateChangedListener onStepStateChangedListener) {
		this.onStepStateChangedListener = onStepStateChangedListener;
	}

	int getState() {
		return state;
	}

	void setState(int state) {
		this.state = state;
		onStepStateChangedListener.onStepStateChanged(this);
	}

	@Override
	public void onPreExecute() {}
	@Override
	public void onProgressUpdate(int percent) {}
	@Override
	public void onCancelled() {}
	@Override
	public void onPostExecute() {}
}
