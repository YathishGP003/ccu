package a75f.io.bo.building;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 6/11/18.
 */

/**
 * Helper class to dump time series data in CSV format to the sdcard.
 */
public class CSVLogger
{
    
    ArrayList<String> csvData;
    String csvName;
    File csvFile;
    FileWriter csvWriter;
    
    public CSVLogger(String name) {
        csvName = name;
        csvData = new ArrayList<>();
        csvFile = new File(Environment.getExternalStorageDirectory(), csvName);
    }
    
    public void writeHeader(String [] headers) {
        for (String s : headers) {
            csvData.add(s);
            csvData.add(",");
        }
        csvData.add("\n");
    }
    
    public void writeRowData(ArrayList<String> row) {
        for (String s : row) {
            csvData.add(s);
            csvData.add(",");
        }
        csvData.add("\n");
    }
    
    public void dump() {
        try
        {
            csvWriter = new FileWriter(csvFile);
            for (String item : csvData) {
                csvWriter.append(item);
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    public void closeLogger() {
        try
        {
            csvWriter.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
