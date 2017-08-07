package a75f.io.renatus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import a75f.io.util.Globals;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class DefaultFragment extends DialogFragment {


    private static final String TAG = DefaultFragment.class.getSimpleName();
    List<String> ports = Arrays.asList("1000", "2000", "3000", "4000", "5000", "6000", "7000", "8000", "9000");

    public static DefaultFragment getInstance() {
        return new DefaultFragment();
    }


    @BindView(R.id.fragment_main_sn_name_edittext)
    EditText mSNNameEditText;

    @BindView(R.id.fragment_main_textview)
    TextView mMainTextView;

    @BindView(R.id.fragment_main_port_spinner)
    Spinner mPortSpinner;
    
    @BindView(R.id.button)
    Button doneBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View retVal = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, retVal);
        return retVal;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupPortSpinner();
        setupSNName();
    }

    private void setupPortSpinner() {

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_item, ports);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPortSpinner.setAdapter(dataAdapter);
        mPortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ports.get(position);
                Globals.getInstance().getSmartNode().setMeshAddress(Short.parseShort(ports.get(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupSNName() {
        mSNNameEditText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String name = mSNNameEditText.getText().toString();
                Toast.makeText(DefaultFragment.this.getContext(), "Saved: " + name, Toast.LENGTH_LONG).show();
                Globals.getInstance().getSmartNode().setName(mSNNameEditText.getText().toString());
                return false;
            }
        });
    }
    
    @OnClick(R.id.button)
    public void done() {
        dismiss();
    }
}


