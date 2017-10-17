package a75f.io.renatus.framework;

import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import a75f.io.renatus.framework.SimulationContext;
import a75f.io.renatus.framework.SimulationTestInfo;

/**
 * Created by samjithsadasivan on 10/4/17.
 */

public class SimulationTestSuite
{
    public HashMap<String,SimulationTestInfo> testMap = new HashMap<>();
    
    public void addSimulationTest(String name, SimulationTestInfo val) {
        testMap.put(name,val);
    }
    
    public SimulationTestInfo getSimulationTest(String name) {
        return testMap.get(name);
    }
    
    public void saveReport() {
        
        String path = Environment.getExternalStorageDirectory().getPath();
        String testReport = "<br /><br /><h3><u>Simulation Test Summary</u></h3>"
                                    .concat("<table width:200; border=1; cellspacing=0; cellpadding=0; table-layout:fixed; word-wrap:break-word;>")
                                    .concat("<tr>")
                                    .concat("<th width=500px> Test Case </th>")
                                    .concat("<th width=500px> Description </th>")
                                    .concat("<th width=500px> Result </th>")
                                    .concat("</tr>");
        try {
            for (Map.Entry<String, SimulationTestInfo> test : testMap.entrySet()) {
                testReport = testReport.concat(test.getValue().getHtml());
                File detailFile = new File(path+"/simulation/", test.getValue().name + ".html");
                FileOutputStream out = new FileOutputStream(detailFile);
                byte[] data = test.getValue().getHtmlDetails().getBytes();
                out.write(data);
                out.close();
            }
            
            testReport = testReport.concat("</table>");
            
            String fileName = DateFormat.format("dd_MM_yyyy_hh_mm_ss", System.currentTimeMillis()).toString();
            File file = new File(path+"/simulation/", fileName + "_summary.html");
            String html = "<html><head><title>Simulation Test Report</title></head><body>"+testReport+"</body></html>";
            
            
            FileOutputStream out = new FileOutputStream(file);
            byte[] data = html.getBytes();
            out.write(data);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void copyAssetsToStorage(String path){
        InputStream in;
        try {
            in = SimulationContext.getInstance().getContext().getAssets().open(path);
            File outFile = new File(Environment.getExternalStorageDirectory().getPath()+"/simulation", "jquery-3.2.1.js");
            OutputStream out = new FileOutputStream(outFile);;
    
            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1){
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            Log.e("Error", e.toString());
        }
    }
}
