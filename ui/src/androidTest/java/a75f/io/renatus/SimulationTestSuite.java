package a75f.io.renatus;

import android.os.Environment;
import android.text.format.DateFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        String testReport = "<br /><br /><h3>Simulation Test Summary</h3>"
                                    .concat("<table width:200; border=1; cellspacing=0; cellpadding=0; table-layout:fixed; word-wrap:break-word;>")
                                    .concat("<tr>")
                                    .concat("<th> Test Case </th>")
                                    .concat("<th> Status </th>")
                                    .concat("<th> Simulation Input </th>")
                                    .concat("<th> Ccu State </th>")
                                    .concat("<th> SmartNode State</th>")
                                    .concat("</tr>");
        
        for (Map.Entry<String, SimulationTestInfo> test : testMap.entrySet()) {
            testReport = testReport.concat(test.getValue().getHtml());
        }
        
        testReport = testReport.concat("</table>");
        
        String fileName = DateFormat.format("dd_MM_yyyy_hh_mm_ss", System.currentTimeMillis()).toString();
        File file = new File(path, fileName + "_Simulation.html");
        String html = "<html><head><title>Simulation Test Report</title></head><body>"+testReport+"</body></html>";
        
        try {
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
}
