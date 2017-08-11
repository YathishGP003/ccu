package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan on 8/9/17.
 */

public class SmartStatControls_t extends Struct
{
	public final SystemTime_t time = inner (new SystemTime_t());
	
	public final Unsigned8 setTemperature = new Unsigned8(); /* default 150 - temp in 2x F */
	
	public final Enum4<SmartStatFanSpeed_t> fanSpeed = new Enum4<>(SmartStatFanSpeed_t.values());
	
	public final Enum4<SmartStatConditioningMode_t> conditioningMode = new Enum4<>(SmartStatConditioningMode_t.values());
	
	public final Unsigned8 relay1 = new Unsigned8(1); /* digital out for activation */
	
	public final Unsigned8 relay2 = new Unsigned8(1); /* digital out for activation */
	
	public final Unsigned8 relay3 = new Unsigned8(1); /* digital out for activation */
	
	public final Unsigned8 relay4 = new Unsigned8(1); /* digital out for activation */
	
	public final Unsigned8 relay5 = new Unsigned8(1); /* digital out for activation */
	
	public final Unsigned8 relay6 = new Unsigned8(1); /* digital out for activation */
	
	public final Unsigned8 reset = new Unsigned8(1); /* force a reset of the device remotely when set to 1 */
	
	public final Unsigned8 reserved = new Unsigned8(1);
}
