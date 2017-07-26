package a75f.io.bo.serial.CCUtoCM;

import javolution.io.Struct;
import javolution.io.Union;

/**
 * Created by ryanmattison on 7/25/17.
 */
/*
typedef union {
 struct {
 uint8_t dynamicAirflowBalancing :1;
 uint8_t lightingControl :1;
 uint8_t outsideAirOptimization :1;
 uint8_t singleStageEquipment :1;
 uint8_t customControl :1;
 uint8_t reserved :3;
 };
 uint8_t bitmap;
} SmartNodeProfileBitmap_t;
 */
class SmartNodeProfileBitmap_t extends Union {
    public final Unsigned8 bitmap = new Unsigned8();
    public final SmartNodeProfileBitmap_t_Extras smartNodeProfileBitmap_t_extras = inner(new SmartNodeProfileBitmap_t_Extras());

    public class SmartNodeProfileBitmap_t_Extras extends Struct {
        public final BitField dynamicAirflowBalancing = new BitField(1); /* 1 is heating, 0 is cooling - sent from CCU to Smart Node for display indication */
        public final BitField lightingControl = new BitField(1); /* digital out for activation */
        public final BitField outsideAirOptimization = new BitField(1); /* digital out for activation */
        public final BitField singleStageEquipment = new BitField(1); /* digital out for activation */
        public final BitField customControl = new BitField(1); /* digital out for activation */
        public final BitField reserved = new BitField(3); /* force a reset of the device remotely when set to 1 */
    }
}
