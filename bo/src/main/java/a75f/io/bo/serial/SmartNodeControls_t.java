package a75f.io.bo.serial;

import javolution.io.Struct;
import javolution.io.Union;

/**
 * Created by ryanmattison on 7/25/17.
 */

public class SmartNodeControls_t extends Struct {

        public final SystemTime_t time = inner(new SystemTime_t());
        public final Unsigned8 setTemperature = new Unsigned8();
        public final Unsigned8 damperPosition = new Unsigned8(); /* Percentage to open damper - default 90% open. */
        public final Unsigned8 analogOut1 = new Unsigned8(); /* output for PWM channel 1 */
        public final Unsigned8 analogOut2 = new Unsigned8(); /* output for PWM channel 2 */
        public final Unsigned8 analogOut3 = new Unsigned8();
        public final Unsigned8 analogOut4 = new Unsigned8(); /* output for PWM channel 3 */
        public final Unsigned8 infraredCommand = new Unsigned8(); /* Command for the infrared transmitter */
        public final SmartNodeControls_Extras smartNodeControls_extras = inner(new SmartNodeControls_Extras());

        public class SmartNodeControls_Extras extends Union {

                public final SmartNodeControls_Extras_Struct smartNodeControlsBitExtras = inner(new SmartNodeControls_Extras_Struct());
                public final Unsigned8 smartNodeControlsBitExtrasAsInt = new Unsigned8();

                public class SmartNodeControls_Extras_Struct extends Struct {
                        public final BitField conditioningMode = new BitField(1); /* 1 is heating, 0 is cooling - sent from CCU to Smart Node for display indication */
                        public final BitField digitalOut1 = new BitField(1); /* digital out for activation */
                        public final BitField digitalOut2 = new BitField(1); /* digital out for activation */
                        public final BitField digitalOut3 = new BitField(1); /* digital out for activation */
                        public final BitField digitalOut4 = new BitField(1); /* digital out for activation */
                        public final BitField reset = new BitField(1); /* force a reset of the device remotely when set to 1 */
                }
        }




}
