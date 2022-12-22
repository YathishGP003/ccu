package a75f.io.logic.bo.building.ccu;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

public class CazProfileConfig extends BaseProfileConfiguration {


    public double temperaturOffset;

    public RoomTempSensor roomTempSensor;
    public SupplyTempSensor supplyTempSensor;

    public SupplyTempSensor getSupplyTempSensor() {
        return supplyTempSensor;
    }

    public void setSupplyTempSensor(SupplyTempSensor supplyTempSensor) {
        this.supplyTempSensor = supplyTempSensor;
    }

    public RoomTempSensor getRoomTempSensor() {
        return roomTempSensor;
    }

    public void setRoomTempSensor(RoomTempSensor roomTempSensor) {
        this.roomTempSensor = roomTempSensor;
    }
}
