package a75f.io.bo.serial;

import org.javolution.io.Struct;
import org.javolution.io.Union;

/**
 * Created by samjithsadasivan isOn 8/10/17.
 */

public class SmartNodeProfileBitmap_t extends Union
{
	class SmartNodeProfileBitmap_tInner extends Struct
	{
		public final Unsigned8 dynamicAirflowBalancing = new Unsigned8(1); /* 1 is heating, 0 is cooling - sent from CCU to Smart Node for display indication */
		public final Unsigned8 lightingControl         = new Unsigned8(1); /* digital out for activation */
		public final Unsigned8 outsideAirOptimization  = new Unsigned8(1); /* digital out for activation */
		public final Unsigned8 singleStageEquipment    = new Unsigned8(1); /* digital out for activation */
		public final Unsigned8 customControl           = new Unsigned8(1); /* digital out for activation */
		public final Unsigned8 reserved                = new Unsigned8(3);
	}
	public final Unsigned8 bitmap = new Unsigned8();
}
