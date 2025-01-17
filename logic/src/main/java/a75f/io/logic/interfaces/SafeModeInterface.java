package a75f.io.logic.interfaces;

import com.google.gson.JsonObject;

import io.reactivex.rxjava3.annotations.NonNull;

public interface SafeModeInterface {
    void handleExitSafeMode();
    void updateRemoteCommands(String commands, String cmdLevel, String id);
    void updateRemoteCommands(@NonNull JsonObject msgObject);
}
