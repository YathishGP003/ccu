package a75f.io.renatus.ENGG;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import a75f.io.renatus.BuildConfig;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.LSerial;
import a75f.io.logic.filesystem.FileSystemTools;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.renatus.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
  * Created by samjithsadasivan on 12/18/18.
  */
public class DevSettings extends Fragment implements AdapterView.OnItemSelectedListener
{
    public static DevSettings newInstance(){
        return new DevSettings();
    }
                                    
    @BindView(R.id.biskitModBtn)
    ToggleButton biskitModeBtn;
    
    @BindView(R.id.logCaptureBtn)
    Button logCaptureBtn;
    
    @BindView(R.id.resetAppBtn)
    Button resetAppBtn;
    
    @BindView(R.id.deleteHis)
    Button deleteHis;
    
    @BindView(R.id.forceSyncBtn)
    Button forceSyncBtn;
    
    @BindView(R.id.testModBtn)
    ToggleButton testModBtn;
    
    @BindView(R.id.testModLayout)
    LinearLayout testModLayout;
    
    @BindView(R.id.outsideTemp)
    Spinner outsideTemp;
    
    @BindView(R.id.outsideHumidity)
    Spinner outsideHumidity;
    
    @BindView(R.id.imageCMSerial) ImageView cmSerial;
    @BindView(R.id.imageMBSerial) ImageView mbSerial;
    @BindView(R.id.reconnectSerial) Button reconnectSerial;
    LinearLayout testParams;

    @BindView(R.id.crashButton) Button crashButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                                  Bundle savedInstanceState) {
         View rootView = inflater.inflate(R.layout.fragment_dev_settings, container, false);
         ButterKnife.bind(this , rootView);
         return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
         super.onViewCreated(view, savedInstanceState);
         biskitModeBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
         {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
                 {
                     Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                             .edit().putBoolean("biskit_mode", b).apply();
                 }
         });
        biskitModeBtn.setChecked(Globals.getInstance().isSimulation());
        
        
        logCaptureBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
    
                alert.setTitle("Log File Name ");
                FileSystemTools fileSystemTools = new FileSystemTools(getContext().getApplicationContext());
                String date = fileSystemTools.timeStamp();
                
                //alert.setMessage(date);
    
                // Set an EditText view to get user input
                final EditText input = new EditText(getActivity());
                input.setText("Renatus_Logs_"+date);
                input.setTextSize(20);
                alert.setView(input);
    
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    fileSystemTools.writeLogCat(input.getText().toString() + ".txt");
                                }
                                catch (IOException | SecurityException ex) {
                                    ex.printStackTrace();
                                    getActivity().runOnUiThread(() -> showErrorDialog(
                                            "Unable to save log file: " + ex.getMessage()));
                                }
                            }
                        }.start();
                    }
                });
    
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
    
                alert.show();
            }
        });
    
        resetAppBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d("CCU"," ResetAppState ");
                L.ccu().systemProfile.reset();
                for (ZoneProfile p : L.ccu().zoneProfiles) {
                    p.reset();
                }
                L.ccu().zoneProfiles.clear();
                Globals.getInstance().loadEquipProfiles();
            }
        });
        
        deleteHis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d("CCU"," deleteHis data ");
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        CCUHsApi.getInstance().deleteHistory();
                    }
                }.start();
            }
        });
    
        forceSyncBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d("CCU"," forceSync site data ");
                CCUHsApi.getInstance().forceSync();
            }
        });
        
        testModBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                       .edit().putBoolean("weather_test", b).apply();
                testModLayout.setVisibility(b?View.VISIBLE :View.INVISIBLE);
            }
        });
        testModBtn.setChecked(Globals.getInstance().isWeatherTest());
        testModLayout.setVisibility(Globals.getInstance().isWeatherTest()?View.VISIBLE :View.INVISIBLE);
    
        ArrayList<Integer> zoroToHundred = new ArrayList<>();
        for (int val = 0;  val <= 100; val++)
        {
            zoroToHundred.add(val);
        }
        ArrayAdapter<Integer> zeroToHundredDataAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
        zeroToHundredDataAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        outsideTemp.setAdapter(zeroToHundredDataAdapter);
        outsideTemp.setOnItemSelectedListener(this);
        outsideTemp.setSelection(zeroToHundredDataAdapter.getPosition(Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                                                             .getInt("outside_temp", 0)));
        outsideHumidity.setAdapter(zeroToHundredDataAdapter);
        outsideHumidity.setOnItemSelectedListener(this);
        outsideHumidity.setSelection(zeroToHundredDataAdapter.getPosition(Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                                                                             .getInt("outside_humidity", 0)));
    
    
        cmSerial.setImageResource(LSerial.getInstance().isConnected() ? android.R.drawable.checkbox_on_background
                                                                      : android.R.drawable.checkbox_off_background);
        mbSerial.setImageResource(LSerial.getInstance().isModbusConnected() ? android.R.drawable.checkbox_on_background
                                      : android.R.drawable.checkbox_off_background);
    
        reconnectSerial.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                triggerRebirth(getActivity());
            }
        });

        if (BuildConfig.BUILD_TYPE.equals("local") || BuildConfig.BUILD_TYPE.equals("dev")) {

            crashButton.setVisibility(View.VISIBLE);
            crashButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    throw new RuntimeException("Test Crash"); // Force a crash
                }
            });
        }
    }
    
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3)
    {
        switch (arg0.getId())
        {
            case R.id.outsideTemp:
                Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                       .edit().putInt("outside_temp", Integer.parseInt(outsideTemp.getSelectedItem().toString())).apply();
                break;
    
            case R.id.outsideHumidity:
                Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                       .edit().putInt("outside_humidity", Integer.parseInt(outsideHumidity.getSelectedItem().toString())).apply();
                break;
               
            
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
        
    }

    // Shows a simple error dialog for the given message.  (We should have a general tool for this.)
    private void showErrorDialog(String msg) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Error")
                .setIcon(R.drawable.ic_alert)
                .setMessage(msg)
                .show();
    }
    
    public static void triggerRebirth(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }
                             
}