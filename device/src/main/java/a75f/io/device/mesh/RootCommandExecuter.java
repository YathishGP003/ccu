package a75f.io.device.mesh;

import java.io.DataOutputStream;
import java.io.IOException;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class RootCommandExecuter {


    public static void runRootCommand(String command) {
        Process su;
        try {
            su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes(command + "\n");
            CcuLog.i(L.TAG_CCU_DEVICE, command);
            outputStream.flush();
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
        } catch (IOException | InterruptedException e) {
            CcuLog.i(L.TAG_CCU_DEVICE, command, e);
            CcuLog.e(L.TAG_CCU_DEVICE, "error ", e);
        }
    }
    
}
