package a75f.io.device.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class LightingScheduleDays_t extends Struct
{
	public final class LightingScheduleDays_tInner extends Struct
	{
		public final Unsigned8 monday = new Unsigned8(1);
		
		public final Unsigned8 tuesday = new Unsigned8(1);
		
		public final Unsigned8 wednesday = new Unsigned8(1);
		
		public final Unsigned8 thursday = new Unsigned8(1);
		
		public final Unsigned8 friday = new Unsigned8(1);
		
		public final Unsigned8 saturday = new Unsigned8(1);
		
		public final Unsigned8 sunday = new Unsigned8(1);

		public final Unsigned8 reserved = new Unsigned8(1);
	}//n;rjnjgf

	
	public final Unsigned8 bitmap = new Unsigned8();
}
