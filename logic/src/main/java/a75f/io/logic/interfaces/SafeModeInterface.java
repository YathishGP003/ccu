package a75f.io.logic.interfaces;

public interface SafeModeInterface {
    void handleExitSafeMode();
    void updateRemoteCommands(String commands, String cmdLevel, String id);
}
