package a75f.io.device.serial;

import org.javolution.io.Struct;
import org.javolution.io.Union;

/**
 * Created by samjithsadasivan isOn 8/9/17.
 */

public class SmartStatEnabledRelaysBitmap_t extends Union
{
	public final class SmartStatEnabledRelaysBitmap_tInner extends Struct
	{
		public final Unsigned8 relay1 = new Unsigned8(1);
		
		public final Unsigned8 relay2 = new Unsigned8(1);
		
		public final Unsigned8 relay3 = new Unsigned8(1);
		
		public final Unsigned8 relay4 = new Unsigned8(1);
		
		public final Unsigned8 relay5 = new Unsigned8(1);
		
		public final Unsigned8 relay6 = new Unsigned8(1);
		
		public final Unsigned8 reserved = new Unsigned8(2);
	}

	
	public final Unsigned8 bitmap = new Unsigned8();
}
