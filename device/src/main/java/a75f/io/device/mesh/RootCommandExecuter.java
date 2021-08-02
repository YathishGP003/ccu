package a75f.io.device.mesh;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class RootCommandExecuter {
    
    
    public static String runShellCommand(String command) {
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = output.toString();
        return response;
    }
    
    public static void runRootCommand(String command) {
        Process su = null;
        try {
            su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes(command + "\n");
            CcuLog.i(L.TAG_CCU_DEVICE, command);
            outputStream.flush();
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
        } catch (IOException e) {
            CcuLog.i(L.TAG_CCU_DEVICE, command, e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            CcuLog.i(L.TAG_CCU_DEVICE, command, e);
            e.printStackTrace();
        }
    }
    
}
