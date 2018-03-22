package a75f.io.renatus.ui.register;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import a75f.io.logic.L;
import a75f.io.renatus.R;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.views.wizard.WizardStep;

public class PickEnvironment extends WizardStep implements View.OnClickListener {

    // You must have an empty constructor for every step
    public PickEnvironment() {
    }

    // Set your layout here
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.step_register_pickenvironment, container, false);
        // Set listener for 'Next' button click
        // Note that we are setting OnClickListener using getActivity() because
        // the 'Next' button is actually part of the hosting activity's layout
        // and
        // not the step's layout
        Button nextButton = getActivity().findViewById(R.id.next_button);
        nextButton.setOnClickListener(this);
        nextButton.setEnabled(false);
        nextButton.setVisibility(Button.GONE);

        // Clear all saved user preferences
        Prefs.clear();

        final String[] environments = getActivity().getResources().getStringArray(R.array.environment_items);

        // Binding resources Array to ListAdapter
        ArrayAdapter<String> envAdapter = new ArrayAdapter<String>(getActivity(), R.layout.step_listitem_enviromenttext, environments);
        ListView lv = v.findViewById(R.id.listView);
        lv.setAdapter(envAdapter);

        // listening to single list item on click
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String[] server_url = getActivity().getResources().getStringArray(R.array.environment_url_items);
                // Set all user preferences needed for the selected environment
                L.setServerEnvironment(environments[position]);


                Button nextButton = parent.getRootView().findViewById(R.id.next_button);
                nextButton.setEnabled(true);
                done();
            }
        });

        return v;
    }

    @Override
    public void onClick(View view) {

        // And call done() to signal that the step is completed successfully
        done();
    }
}
