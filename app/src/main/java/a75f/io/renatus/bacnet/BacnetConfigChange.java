package a75f.io.renatus.bacnet;

public interface BacnetConfigChange {
    void submitConfiguration(String configurationType, boolean isAutoEnabled, int timeInSeconds);
}
