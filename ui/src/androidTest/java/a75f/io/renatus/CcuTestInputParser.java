package a75f.io.renatus;

import android.content.Context;
import android.util.Log;

import com.opencsv.CSVReader;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.json.serializers.JsonSerializer;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

public class CcuTestInputParser
{
    private static final String ASSETS_STATE_PATH = "ccustates/";
    private static final String ASSETS_TESTS_PATH = "simulationtests/";
    public static CCUApplication parseStateConfig(CcuTestEnv cxt, String fileName){
        
        String ccuConfig = readFileFromAssets(cxt.getContext(), ASSETS_STATE_PATH+fileName);
        CCUApplication appStruct = null;
        try
        {
            appStruct = (CCUApplication) JsonSerializer.fromJson(ccuConfig, CCUApplication.class);
        }
        catch (IOException e)
        {
            
            e.printStackTrace();
            
        }
        return appStruct;
        
    }
    
    public static List<String[]> parseSimulationFile(CcuTestEnv cxt, String fileName) {
        List<String[]> simFile = readSimulationCSV(cxt.getContext(), ASSETS_TESTS_PATH+fileName);
        return simFile;
    }
    
    public static String readFileFromAssets(Context ctx, String pathToJson){
        InputStream rawInput;
        ByteArrayOutputStream rawOutput = null;
        try {
            rawInput = ctx.getAssets().open(pathToJson);
            byte[] buffer = new byte[rawInput.available()];
            rawInput.read(buffer);
            rawOutput = new ByteArrayOutputStream();
            rawOutput.write(buffer);
            rawOutput.close();
            rawInput.close();
            ;
        } catch (IOException e) {
            Log.e("Error", e.toString());
        }
        return rawOutput != null ? rawOutput.toString() : null;
    }
    
    public static List<String[]> readSimulationCSV(Context ctx, String pathToCSV) {
    
        List<String[]> csvLineList = null;
        try
        {
            CSVReader reader = new CSVReader(new InputStreamReader(ctx.getAssets().open(pathToCSV)));
            csvLineList = reader.readAll();
        }
        catch (FileNotFoundException e)
        {
            Log.e("Error",e.getMessage());
        }
        catch (IOException e)
        {
            Log.e("Error",e.getMessage());
        }
    
        return csvLineList;
    }
}
