package a75f.io.renatus;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by samjithsadasivan on 9/25/17.
 */

public class SimulationParams
{
    public float room_temperature;
    public float airflow_1_temperature;
    public float airflow_2_temperature;
    public int   external_analog_voltage_input_1;
    public int   external_analog_voltage_input_2;
    public int   external_thermistor_input_1;
    public int   external_thermistor_input_2;
    public int   actual_damper_position;
    public float set_temperature;
    public int   hvac_supply_voltage;
    public float measured_motor_1_forward_rpm;
    public float measured_motor_1_reverse_rpm;
    public float measured_motor_2_forward_rpm;
    public float measured_motor_2_reverse_rpm;
    public int   battery_status;
    public int   actual_conditioning_mode;
    public int   external_power;
    public int   node_type;
    public int   damper_1_calibration_error;
    public int   damper_2_calibration_error;
    
    public SimulationParams build(String[] vals)
    {
        SimulationParams params = new SimulationParams();
        params.room_temperature = Float.parseFloat(vals[2]);
        params.airflow_1_temperature = Float.parseFloat(vals[3]);
        params.airflow_2_temperature = Float.parseFloat(vals[4]);
        params.external_analog_voltage_input_1 = Integer.parseInt(vals[5].trim());
        params.external_analog_voltage_input_2 = Integer.parseInt(vals[6].trim());
        params.external_thermistor_input_1 = Integer.parseInt(vals[7].trim());
        params.external_thermistor_input_2 = Integer.parseInt(vals[8].trim());
        params.actual_damper_position = Integer.parseInt(vals[9].trim());
        params.set_temperature = Float.parseFloat(vals[10]);
        params.hvac_supply_voltage = Integer.parseInt(vals[11].trim());
        params.measured_motor_1_forward_rpm = Float.parseFloat(vals[12]);
        params.measured_motor_1_reverse_rpm = Float.parseFloat(vals[13]);
        params.measured_motor_2_forward_rpm = Float.parseFloat(vals[14]);
        params.measured_motor_2_reverse_rpm = Float.parseFloat(vals[15]);
        params.battery_status = Integer.parseInt(vals[16].trim());
        params.actual_conditioning_mode = Integer.parseInt(vals[17].trim());
        params.external_power = Integer.parseInt(vals[18].trim());
        params.node_type = Integer.parseInt(vals[19].trim());
        params.damper_1_calibration_error = Integer.parseInt(vals[20].trim());
        params.damper_2_calibration_error = Integer.parseInt(vals[21].trim());
        return params;
    }
    
    public String convertToJsonString()
    {
        String jsonString = null;
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            jsonString = mapper.writeValueAsString(this);
        }
        catch (JsonGenerationException e)
        {
            e.printStackTrace();
        }
        catch (JsonMappingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return jsonString;
    }
}