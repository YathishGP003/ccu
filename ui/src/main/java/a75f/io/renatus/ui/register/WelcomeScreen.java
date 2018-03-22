package a75f.io.renatus.ui.register;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import a75f.io.renatus.R;
import a75f.io.renatus.views.wizard.WizardStep;


public class WelcomeScreen extends WizardStep implements View.OnClickListener {

    //You must have an empty constructor for every step
    public WelcomeScreen() {
    }

    //Set your layout here
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.step_register_welcomescreen, container, false);

        v.setOnClickListener(this);

        //Set listener for 'Next' button click
        //Note that we are setting OnClickListener using getActivity() because
        //the 'Next' button is actually part of the hosting activity's layout and
        //not the step's layout
        Button nextButton = (Button) this.getActivity().findViewById(R.id.next_button);
        nextButton.setOnClickListener(this);
        nextButton.setText("Login to Kinvey");
        nextButton.setEnabled(true);
        nextButton.setVisibility(Button.VISIBLE);

        return v;
    }

    @Override
    public void onClick(View view) {

        //And call done() to signal that the step is completed successfully
        done();
    }
}

