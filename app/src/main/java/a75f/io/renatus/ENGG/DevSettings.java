package a75f.io.renatus.ENGG;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import a75f.io.logic.Globals;
import a75f.io.renatus.R;
import butterknife.BindView;
import butterknife.ButterKnife;
        
        /**
  * Created by samjithsadasivan on 12/18/18.
  */
public class DevSettings extends Fragment
{
    public static DevSettings newInstance(){
        return new DevSettings();
    }
                                    
    @BindView(R.id.biskitModBtn)
    ToggleButton biskitModeBtn;
    
    @BindView(R.id.logCaptureBtn)
    Button logCaptureBtn;
    
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
    
       
    }
    
    
    protected void writeLogCat(String name)
    {
        try
        {
            Process process = Runtime.getRuntime().exec("logcat -v threadtime -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder log = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null)
            {
                log.append(line);
                log.append("\n");
            }
            
            //Convert log to string
            final String logString = new String(log.toString());
            
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
            osw.write(logString);
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