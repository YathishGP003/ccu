package a75f.io.renatus.safemode;


import static a75f.io.alerts.model.AlertCauses.CCU_EXIT_SAFE_MODE;

import static a75f.io.renatus.UtilityApplication.context;
import static a75f.io.renatus.util.CCUUiUtil.UpdateAppRestartCause;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;

import a75f.io.alerts.AlertManager;
import a75f.io.domain.api.Domain;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.interfaces.SafeModeInterface;
import a75f.io.messaging.handler.RemoteCommandUpdateHandler;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.UtilityApplication;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.CloudConnetionStatusThread;
import a75f.io.renatus.util.remotecommand.RemoteCommandHandlerUtil;

public class SafeModeActivity extends AppCompatActivity implements SafeModeInterface {

    Button exitSafeMode;
    static CloudConnetionStatusThread mCloudConnectionStatus = null;

    public static SafeModeActivity getInstance(){
        return new SafeModeActivity();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        UtilityApplication utilityApplication = (UtilityApplication) getApplicationContext();
        utilityApplication.initMessaging();
        RemoteCommandUpdateHandler.setSafeInterface(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.safe_mode);
        exitSafeMode = findViewById(R.id.exit_safe_mode);
        setExitSafeModeTheme(exitSafeMode);

        exitSafeMode.setOnClickListener(view -> exitSafeMode());

    }

    @Override
    protected void onPause() {
        super.onPause();
        RemoteCommandUpdateHandler.setSafeInterface(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CloudConnectionAlive();
        RemoteCommandUpdateHandler.setSafeInterface(this);
    }

    public void exitSafeMode(){
        CcuLog.d(L.TAG_CCU_SAFE_MODE, "exitSafeMode Complete");
        Domain.diagEquip.getSafeModeStatus().writeHisVal(0.0);
        SharedPreferences crashPreference = this.getSharedPreferences("crash_preference", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = crashPreference.edit();
        editor.clear();
        editor.commit();
        editor.putBoolean("SafeModeExit", true).commit();
        AlertManager.getInstance().fixSafeMode();
        AlertManager.getInstance().fixPreviousCrashAlert();
        UpdateAppRestartCause(CCU_EXIT_SAFE_MODE);
        RenatusApp.closeApp();
    }

    @Override
    public void handleExitSafeMode() {
        CcuLog.d("RemoteCommandHandle", "exitSafeMode handle");
        if(Globals.getInstance().isSafeMode())
            exitSafeMode();
    }

    @Override
    public void updateRemoteCommands(String commands,String cmdLevel,String id) {
        RemoteCommandHandlerUtil.handleRemoteCommand(commands,cmdLevel,id);
    }

    @Override
    public void updateRemoteCommands(JsonObject msgObject) {
        CcuLog.d("RemoteCommand","SafeModeActivity.UpdateRemoteCommands="+msgObject.toString());
        RemoteCommandHandlerUtil.handleRemoteCommand(msgObject);
    }

    public static synchronized void CloudConnectionAlive() {
        if (mCloudConnectionStatus == null) {
            mCloudConnectionStatus = new CloudConnetionStatusThread();
            mCloudConnectionStatus.start();
        }
    }

    private void setExitSafeModeTheme(Button exitSafeMode) {
        if (CCUUiUtil.isCarrierThemeEnabled(getApplicationContext())) {
            exitSafeMode.setTextColor(ContextCompat.getColor(context, R.color.carrier_75f));
        } else if (CCUUiUtil.isDaikinEnvironment(getApplicationContext())) {
            exitSafeMode.setTextColor(ContextCompat.getColor(context, R.color.daikin_75f));
        } else if (CCUUiUtil.isAiroverseThemeEnabled(getApplicationContext())) {
            exitSafeMode.setTextColor(ContextCompat.getColor(context, R.color.airoverse_primary));
        } else {
            exitSafeMode.setTextColor(ContextCompat.getColor(context, R.color.renatus_75f_primary));
        }
    }
}
