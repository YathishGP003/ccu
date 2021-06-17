package a75f.io.renatus;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.CCUUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ModbusConfigFragment extends Fragment {
    
    private final String PREF_MB_BAUD_RATE = "mb_baudrate";
    private final String PREF_MB_PARITY = "mb_parity";
    private final String PREF_MB_DATA_BITS = "mb_databits";
    private final String PREF_MB_STOP_BITS = "mb_stopbits";
    
    @BindView(R.id.spinnerBaudRate) Spinner spinnerBaudRate;
    
    @BindView(R.id.spinnerParity) Spinner spinnerParity;
    
    @BindView(R.id.spinnerDatabits) Spinner spinnerDatabits;
    
    @BindView(R.id.spinnerStopBits) Spinner spinnerStopbits;
    
    @BindView(R.id.btnRestart) Button btnRestart;
    
    public ModbusConfigFragment() {
    
    }
    
    public static ModbusConfigFragment newInstance() {
        return new ModbusConfigFragment();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
                                                                                                  Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_modbusconfig, container, false);
        ButterKnife.bind(this, rootView);
        
        return rootView;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        spinnerBaudRate.setSelection(((ArrayAdapter<String>)spinnerBaudRate.getAdapter())
                                         .getPosition(String.valueOf(readIntPref(PREF_MB_BAUD_RATE, 9600))));
        spinnerBaudRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MB_BAUD_RATE, Integer.parseInt(spinnerBaudRate.getSelectedItem().toString()));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    
        spinnerParity.setSelection(readIntPref(PREF_MB_PARITY, 0),false);
        spinnerParity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MB_PARITY, position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    
        spinnerDatabits.setSelection(((ArrayAdapter<String>)spinnerDatabits.getAdapter())
                       .getPosition(String.valueOf(readIntPref(PREF_MB_DATA_BITS, 8))),false);
        spinnerDatabits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MB_DATA_BITS, Integer.parseInt(spinnerDatabits.getSelectedItem().toString()));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });

        spinnerStopbits.setSelection(((ArrayAdapter<String>)spinnerStopbits.getAdapter())
                                         .getPosition(String.valueOf(readIntPref(PREF_MB_STOP_BITS, 1))));
        spinnerStopbits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MB_STOP_BITS, Integer.parseInt(spinnerStopbits.getSelectedItem().toString()));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
    
        btnRestart.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                CCUUiUtil.triggerRestart(getActivity());
            }
        });
    }
    
    public int readIntPref(String key, int defaultVal) {
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return spDefaultPrefs.getInt(key, defaultVal);
    }
    
    public void writeIntPref(String key, int val) {
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = spDefaultPrefs.edit();
        editor.putInt(key, val);
        editor.commit();
    }
    

}
