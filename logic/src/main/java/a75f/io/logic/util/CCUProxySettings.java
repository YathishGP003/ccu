package a75f.io.logic.util;
import static a75f.io.logic.util.backupfiles.FileConstants.ADB_COMMANDS_PATH;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import a75f.io.logger.CcuLog;
public class CCUProxySettings {
    public static void setUpProxySettingsIfExists(){
        File proxySettings = new File(ADB_COMMANDS_PATH+"startUpAdbCommands.txt");
        boolean isProxySettings = proxySettings.exists();
        CcuLog.i("CCU_PROXY","Is proxySettings exists ? "+proxySettings.exists());
        if(isProxySettings) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        setStaticIpIfExists(proxySettings);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    timer.cancel();
                }
            }, 60000);
        } else{
            CcuLog.i("CCU_PROXY","IP settings does not exist ");
        }
    }
    private static void setStaticIpIfExists(File proxySettings) throws FileNotFoundException {
        Scanner reader = new Scanner(proxySettings);
        while (reader.hasNextLine()) {
            String data = reader.nextLine();
            runRootCommand(data);
        }
        reader.close();
    }
    public static void runRootCommand(String command) {
        CcuLog.i("CCU_PROXY","Command Executed: "+command);
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static String ShellExecute(String command) {
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
        return output.toString();
    }

    public static boolean CheckEthernet() {
        CcuLog.i("CCU_PROXY","CheckEthernet : ");
        boolean isEthernetConnected = false;
        String checkEthernetConnected = ShellExecute("cat /sys/class/net/eth0/operstate");
        if (checkEthernetConnected.contains("up")) {
            isEthernetConnected = true;
        } else if (checkEthernetConnected.contains("down")) {
            isEthernetConnected = false;
        }
        return isEthernetConnected;
    }

}