package a75f.io.renatus.safemode;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.interfaces.SafeModeInterface;
import a75f.io.messaging.handler.RemoteCommandUpdateHandler;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.util.remotecommand.RemoteCommandHandlerUtil;

public class SafeModeActivity extends AppCompatActivity implements SafeModeInterface {

    Button exitSafeMode;

    public static SafeModeActivity getInstance(){
        return new SafeModeActivity();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        RemoteCommandUpdateHandler.setSafeInterface(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.safe_mode);
        exitSafeMode = findViewById(R.id.exit_safe_mode);

        exitSafeMode.setOnClickListener(view -> {
            exitSafeMode();
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        RemoteCommandUpdateHandler.setSafeInterface(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteCommandUpdateHandler.setSafeInterface(this);
    }

    public void exitSafeMode(){
        CcuLog.d("SafeMode", "exitSafeMode Complete");
        CCUHsApi.getInstance().writeHisValByQuery("point and safe and mode and diag and his",0.0);
        SharedPreferences crashPreference = this.getSharedPreferences("crash_preference", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = crashPreference.edit();
        editor.clear();
        editor.commit();
        AlertManager.getInstance().fixSafeMode();
        AlertManager.getInstance().fixPreviousCrashAlert();
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
}
