package a75f.io.logic.pubnub;

public interface ZoneDataInterface {
    void refreshScreen(String id);
    void updateTemperature(double currentTemp, short nodeAddress);
    void refreshScreenbySchedule(String nodeAddress, String EquipId, String zoneId);
    void refreshTemp(String nodeAddress,String equipId);
    void refreshScreenbyVAV(String nodeAddress, String equipId);
}

