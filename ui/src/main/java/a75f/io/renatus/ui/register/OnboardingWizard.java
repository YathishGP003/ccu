package a75f.io.renatus.ui.register;

import android.os.Bundle;
import android.widget.Button;

import a75f.io.logic.L;
import a75f.io.renatus.R;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.views.wizard.WizardActivity;
import a75f.io.renatus.views.wizard.WizardFlow;


public class OnboardingWizard extends WizardActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard_onboarding);
        Button nextButton = this.findViewById(R.id.next_button);
        nextButton.setTextColor(0xFFFFFFFF);
    }

	//You must override this method and create a wizard flow by
	//using WizardFlow.Builder as shown in this example
	@Override
	public void onSetup(WizardFlow flow) {
		WizardFlow.Builder builder = new WizardFlow.Builder()
				.setActivity(this)
				.setContainerId(R.id.step_container);


		//TODO: test mode.
		//if (L.isDeveloperTesting()) {
			builder.addStep(PickEnvironment.class);
		//}

		flow = builder.addStep(WelcomeScreen.class).addStep(ExampleSamjith.class).create();                  //to create the wizard flow.

		// Make sure no previous user data is saved.
		//Prefs.clear();

		//Call the super method using the newly created flow
		super.onSetup(flow);
    }

    //Overriding this method is optional
    @Override
    public void onWizardDone() {
        //Do whatever you want to do once the Wizard is complete
        //in this case I just close the activity, which causes Android
        //to go back to the previous activity.
        finish();
    }
}