package a75f.io.renatus.ENGG;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import a75f.io.api.haystack.CCUHsApi;
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
    
    LinearLayout testParams;
    
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
    
                String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.getDefault()).format(new Date());
                
                //alert.setMessage(date);
    
                // Set an EditText view to get user input
                final EditText input = new EditText(getActivity());
                input.setText("Renatus_Logs_"+date);
                input.setTextSize(20);
                alert.setView(input);
    
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        new Thread()
                        {
                            @Override
                            public void run()
                            {
                                writeLogCat(input.getText().toString()+".txt");
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
    
    
    protected void writeLogCat(String name)
    {
        try
        {
            Process process = Runtime.getRuntime().exec("logcat -v threadtime -d | grep CCU");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder log = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null)
            {
                log.append(line);
                log.append("\n");
            }
            
            //Create txt file in SD Card
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() +File.separator + "RenatusLogs");
            
            if(!dir.exists())
            {
                dir.mkdirs();
            }
            
            File file = new File(dir, name);
            
            //To write logcat in text file
            FileOutputStream fout = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fout);
            
            //Writing the string to file
            osw.write(log.toString());
            osw.flush();
            osw.close();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
                             
}