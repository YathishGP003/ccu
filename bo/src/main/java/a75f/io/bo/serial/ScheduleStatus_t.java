package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan on 8/1/17.
 */

public class ScheduleStatus_t extends Struct {

    public final BitField mode = new BitField(2); // 00 - zone schedule, 01 - System Schedule, 10 - Hold

    public final BitField protectSchedule = new BitField(1);// 1 - do not modify schedule even if set temp changed during occupancy. 0 -false

    public final BitField vacation = new BitField(1); // 1 - true, 0 - false

    public final BitField reserved = new BitField(4); //
}
