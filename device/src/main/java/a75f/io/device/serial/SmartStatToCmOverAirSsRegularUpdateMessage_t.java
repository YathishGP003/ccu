package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;
/**
 * Created by Anilkumar On 02/05/2019.
 */
public class SmartStatToCmOverAirSsRegularUpdateMessage_t extends Struct {

    public final Unsigned16 smartNodeAddress = new Unsigned16(); /* LW Mesh Address of the Smart Node sending the message */

    public final Unsigned16 parentShortAddr = new Unsigned16(); /* only for router we have next hop */

    public final Unsigned8 lqi = new Unsigned8(); /* Link Quality Indicator of last received data packet */

    public final Signed8 rssi = new Signed8(); /* Received Signal Strength Indicator of last received data packet */


    public final Unsigned16 roomTemperature = new Unsigned16(); /* room temp in 1/10 F. This is the adjusted temp and is offset + measured temp */

    public final Unsigned16 humidity = new Unsigned16(); /* humidity  in 1/10 F */

    public final Unsigned16 lightIntensity = new Unsigned16(); /* light intensity */

    public final Unsigned16 externalAnalogVoltageInput1 = new Unsigned16(); /* external voltage sense in millivolts */

    public final Unsigned16 externalAnalogVoltageInput2 = new Unsigned16(); /* external voltage sense in millivolts */

    public final Unsigned16 externalThermistorInput1 = new Unsigned16(); /* external resistance sense in tens of ohms */

    public final Unsigned16 externalThermistorInput2 = new Unsigned16(); /* external resistance sense in tens of ohms */

    public SmartNodeSensorReading_t[] sensorReadings = array(new SmartNodeSensorReading_t[MessageConstants.NUM_SN_TYPE_VALUE_SENSOR_READINGS]);

    public final Unsigned8 occupancyDetected = new Unsigned8(1); /* 1 is occupancy sensed, 0 is no occupancy */

    @Override
    public ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
