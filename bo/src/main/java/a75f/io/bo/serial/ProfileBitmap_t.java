package a75f.io.bo.serial;

import org.javolution.io.Struct;
import org.javolution.io.Union;

/**
 * Created by samjithsadasivan on 8/9/17.
 */

public class ProfileBitmap_t extends Union
{
	public final class ProfileBitmap_tInner extends Struct
	{
	
	public final Unsigned8 dynamicAirflowBalancing = new Unsigned8(1);
	
	public final Unsigned8 lightingControl = new Unsigned8(1);
	
	public final Unsigned8 outsideAirOptimization = new Unsigned8(1);
	
	public final Unsigned8 singleStageEquipment = new Unsigned8(1);
	
	public final Unsigned8 customControl = new Unsigned8(1);
	
	public final Unsigned8 reserved = new Unsigned8(3);
	
   }
	
	public final Unsigned8 bitmap = new Unsigned8();
}
