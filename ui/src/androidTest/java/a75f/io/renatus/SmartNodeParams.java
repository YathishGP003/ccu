package a75f.io.renatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by samjithsadasivan on 9/26/17.
 */

public class SmartNodeParams
{
    public String timestamp;
    public float max_user_temp;
    public float min_user_temp;
    public int max_damper_open;
    public int min_damper_open;
    public int temperature_offset;
    public int forward_motor_backlash;
    public int reverse_motor_backlash;
    public float proportional_constant;
    public float integral_constant;
    public float proportional_temperature_range;
    public int integration_time;
    public float airflow_heating_temperature;
    public float airflow_cooling_temperature;
    public int power_in_led_enabled;
    public int power_out_led_enabled;
    public int analog_in_1_led_enabled;
    public int analog_in_2_led_enabled;
    public int analog_in_3_led_enabled;
    public int analog_in_4_led_enabled;
    public int analog_out_1_led_enabled;
    public int analog_out_2_led_enabled;
    public int digital_out_1_led_enabled;
    public int digital_out_2_led_enabled;
    public int power_24v_1_led_enabled;
    public int power_24v_2_led_enabled;
    public int logo_led_enabled;
    public int dynamic_airflow_balancing_enabled;
    public int lighting_control_enabled;
    public int outside_air_optimization_enabled;
    public int single_stage_equipment_enabled;
    public int lighting_intensity_for_occupant_detected;
    public int min_lighting_control_override_time_in_minutes;
    public int show_centigrade;
    public int display_hold;
    public int military_time;
    public int enable_occupancy_detection;
    public float room_temperature;
    public float airflow_1_temperature;
    public float airflow_2_temperature;
    public int external_analog_voltage_input_1;
    public float external_analog_voltage_input_2;
    public float external_thermistor_input_1;
    public float external_thermistor_input_2;
    public int actual_damper_position;
    public float set_temperature;
    public int hvac_supply_voltage;
    public float measured_motor_1_forward_rpm;
    public float measured_motor_1_reverse_rpm;
    public float measured_motor_2_forward_rpm;
    public float measured_motor_2_reverse_rpm;
    public SensorReading sensor_reading_0;
    public SensorReading sensor_reading_1;
    public SensorReading sensor_reading_2;
    public SensorReading sensor_reading_3;
    public SensorReading sensor_reading_4;
    public SensorReading sensor_reading_5;
    public int battery_status;
    public int actual_conditioning_mode;
    public int external_power;
    public int node_type;
    public int damper_1_calibration_error;
    public int damper_2_calibration_error;
    public int time_day_of_week;
    public int time_hours;
    public int time_minutes;
    public float set_temperature_from_ccu;
    public int desired_damper_position;
    public int analog_out_1;
    public int analog_out_2;
    public int analog_out_3;
    public int analog_out_4;
    public int infrared_command;
    public int conditioning_mode_from_ccu;
    public int digital_out_1;
    public int digital_out_2;
    public int digital_out_3;
    public int digital_out_4;
    public int reset;
    
    public static SmartNodeParams getParamsFromJson(String json) {
        SmartNodeParams params = null;
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            params = mapper.readValue(json, SmartNodeParams.class);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return params;
    }
    
}
