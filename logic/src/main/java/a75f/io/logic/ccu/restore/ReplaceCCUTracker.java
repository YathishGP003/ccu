package a75f.io.logic.ccu.restore;

import android.content.Context;
import android.content.SharedPreferences.Editor;

import java.util.Map;

import a75f.io.logic.Globals;

public class ReplaceCCUTracker {
    private Editor editor;
    private static final String CCU_REPLACE_STATUS_SHARED_CONFIG = "ccu_replace_status";

    public ReplaceCCUTracker(){
        editor =  Globals.getInstance().getApplicationContext().getSharedPreferences(CCU_REPLACE_STATUS_SHARED_CONFIG,
                Context.MODE_PRIVATE).edit();
    }

    public Editor getEditor() {
        return editor;
    }

    public Map<String, ?> getReplaceCCUStatus(){
        return Globals.getInstance().getApplicationContext().getSharedPreferences(CCU_REPLACE_STATUS_SHARED_CONFIG,
                Context.MODE_PRIVATE).getAll();
    }

    public void updateReplaceStatus(String key, String value){
        editor.putString(key, value);
        editor.commit();
    }

}
