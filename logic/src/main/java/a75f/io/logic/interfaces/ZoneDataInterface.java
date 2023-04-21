package a75f.io.logic.interfaces;


public interface ZoneDataInterface {
    void refreshScreen(String id);
    void updateTemperature(double currentTemp, short nodeAddress);
    void refreshScreenbySchedule(String nodeAddress, String EquipId, String zoneId);
    void refreshDesiredTemp(String nodeAddress, String coolDesiredTemp, String heatDesiredTemp);
    void updateSensorValue(short nodeAddress);
    void refreshHeartBeatStatus(String id);
}

